package com.securevault;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TokenService {
    public record Refresh(String token, Store.User user, long expiresAt) {}
    private record Stored(String owner, String family, long expires, boolean used, boolean revoked) {}
    private final Store store;
    private final long lifetime;
    private final SecureRandom random = new SecureRandom();
    public TokenService(Store store,@Value("${app.refresh-seconds}") long lifetime) { this.store=store; this.lifetime=lifetime; }
    @Transactional public Refresh create(Store.User user) { return insert(user, UUID.randomUUID().toString(), System.currentTimeMillis()+lifetime*1000); }
    private Refresh insert(Store.User user, String family, long expires) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store.jdbc().update("INSERT INTO refresh_tokens(token_hash,owner_id,family_id,created_at,expires_at) VALUES(?,?,?,?,?)", hash(token),user.id(),family,System.currentTimeMillis(),expires);
        return new Refresh(token,user,expires);
    }
    /** Row lock makes rotation atomic. Replay revocation must commit even when returning 401. */
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public Refresh rotate(String token) {
        if (token == null || token.length() > 256) throw unauthorized();
        var rows = store.jdbc().query("SELECT * FROM refresh_tokens WHERE token_hash=? FOR UPDATE",(r,n)->new Stored(r.getString("owner_id"),r.getString("family_id"),r.getLong("expires_at"),r.getBoolean("used"),r.getBoolean("revoked")),hash(token));
        if (rows.isEmpty()) throw unauthorized();
        Stored old = rows.get(0);
        Store.User user=store.byId(old.owner()).orElseThrow(TokenService::unauthorized);
        if (old.used() || old.revoked() || old.expires() <= System.currentTimeMillis()) {
            store.jdbc().update("UPDATE refresh_tokens SET revoked=TRUE WHERE family_id=?",old.family());
            store.audit(user.email(),"REFRESH_REJECTED","Expired or reused refresh token; family revoked");
            throw unauthorized();
        }
        store.jdbc().update("UPDATE refresh_tokens SET used=TRUE WHERE token_hash=?",hash(token));
        Refresh next=insert(user,old.family(),old.expires());
        store.audit(user.email(),"TOKEN_ROTATED","New access and refresh tokens issued");
        return next;
    }
    @Transactional public void logout(String token) {
        if(token==null || token.length()>256) return;
        var owners=store.jdbc().queryForList("SELECT owner_id FROM refresh_tokens WHERE token_hash=?",String.class,hash(token));
        store.jdbc().update("UPDATE refresh_tokens SET revoked=TRUE WHERE family_id IN (SELECT family_id FROM refresh_tokens WHERE token_hash=?)",hash(token));
        if(!owners.isEmpty()) store.byId(owners.get(0)).ifPresent(u->store.audit(u.email(),"SIGNED_OUT","Refresh session revoked"));
    }
    public void revokeAll(Store.User user) {
        store.jdbc().update("UPDATE refresh_tokens SET revoked=TRUE WHERE owner_id=?",user.id());
        store.audit(user.email(),"SESSIONS_REVOKED","All refresh sessions revoked");
    }
    public List<Map<String,Object>> sessions(Store.User user) {
        return store.jdbc().query("SELECT family_id,MIN(created_at) AS created,MAX(expires_at) AS expires FROM refresh_tokens WHERE owner_id=? AND used=FALSE AND revoked=FALSE AND expires_at>? GROUP BY family_id ORDER BY created DESC",
            (r,n)->Map.<String,Object>of("id",r.getString("family_id"),"createdAt",r.getLong("created"),"expiresAt",r.getLong("expires")),user.id(),System.currentTimeMillis());
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Refresh session expired or revoked. Please sign in again."); }
}
