package com.securevault;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    public record Login(@NotBlank @Email @Size(max=160) String email, @NotBlank @Size(max=72) String password) {}
    public record NewUser(@NotBlank @Email @Size(max=160) String email, @NotBlank @Size(max=80) String name,
                          @Size(min=10,max=72) @NotBlank String password, @Pattern(regexp="USER|ADMIN") @NotNull String role) {}
    public record VaultInput(@NotBlank @Size(max=80) String provider, @NotBlank @Size(max=100) String label,
                             @NotBlank @Size(max=2000) String secret) {}
    private record Attempt(long start, int count) {}
    private final ConcurrentHashMap<String,Attempt> attempts=new ConcurrentHashMap<>();
    private final Store store; private final PasswordEncoder passwords; private final JwtService jwt;
    private final TokenService tokens; private final CryptoService crypto; private final boolean secureCookies;
    private final String dummyHash;
    public ApiController(Store store,PasswordEncoder passwords,JwtService jwt,TokenService tokens,CryptoService crypto,@Value("${app.secure-cookies}") boolean secureCookies) {
        this.store=store; this.passwords=passwords; this.jwt=jwt; this.tokens=tokens; this.crypto=crypto; this.secureCookies=secureCookies;
        dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @PostMapping("/auth/login") public Map<String,Object> login(@Valid @RequestBody Login input,HttpServletRequest request,HttpServletResponse response) {
        long now=System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> now-e.getValue().start()>60000);
        if(attempts.size()>10000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Try again in one minute");
        Attempt attempt=attempts.compute(request.getRemoteAddr(),(k,old)->old==null?new Attempt(now,1):new Attempt(old.start(),old.count()+1));
        if(attempt.count()>20) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many login attempts. Try again in one minute.");
        var found=store.byEmail(input.email());
        boolean valid=input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72
            && passwords.matches(input.password(),found.map(Store.User::passwordHash).orElse(dummyHash));
        if(found.isEmpty() || !valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Incorrect email or password");
        Store.User user=found.get();
        var refresh=tokens.create(user); setCookie(response,refresh);
        store.audit(user.email(),"SIGNED_IN","Password verified; JWT and refresh session issued");
        return authBody(user);
    }
    @PostMapping("/auth/refresh") public Map<String,Object> refresh(@CookieValue(name="sv_refresh",required=false) String value,HttpServletResponse response) {
        try { var refresh=tokens.rotate(value); setCookie(response,refresh); return authBody(refresh.user()); }
        catch(ResponseStatusException e) { clearCookie(response); throw e; }
    }
    @PostMapping("/auth/logout") public Map<String,String> logout(@CookieValue(name="sv_refresh",required=false) String value,HttpServletResponse response) {
        tokens.logout(value); clearCookie(response); return Map.of("message","Signed out. Refresh session revoked.");
    }
    private Map<String,Object> authBody(Store.User user) { return Map.of("accessToken",jwt.issue(user),"expiresIn",jwt.lifetime,"user",store.publicUser(user)); }
    private void setCookie(HttpServletResponse response,TokenService.Refresh refresh) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("sv_refresh",refresh.token()).httpOnly(true).secure(secureCookies)
            .sameSite("Strict").path("/api/auth").maxAge(Duration.ofMillis(Math.max(0,refresh.expiresAt()-System.currentTimeMillis()))).build().toString());
    }
    private void clearCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("sv_refresh","").httpOnly(true).secure(secureCookies).sameSite("Strict").path("/api/auth").maxAge(0).build().toString());
    }
    @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal Store.User user) { return store.publicUser(user); }
    @GetMapping("/vault") public List<Map<String,Object>> vault(@AuthenticationPrincipal Store.User user) {
        return store.jdbc().query("SELECT id,provider,label,created_at FROM credentials WHERE owner_id=? ORDER BY created_at DESC",
            (r,n)->Map.<String,Object>of("id",r.getString("id"),"provider",r.getString("provider"),"label",r.getString("label"),"createdAt",r.getLong("created_at")),user.id());
    }
    @PostMapping("/vault") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> save(@AuthenticationPrincipal Store.User user,@Valid @RequestBody VaultInput input) {
        String id=UUID.randomUUID().toString();
        String encrypted=crypto.encrypt(input.secret(),user.id()+":"+id);
        store.jdbc().update("INSERT INTO credentials VALUES(?,?,?,?,?,?)",id,user.id(),input.provider(),input.label(),encrypted,System.currentTimeMillis());
        store.audit(user.email(),"CREDENTIAL_SAVED","Secret encrypted with AES-256-GCM");
        return Map.of("id",id,"message","Credential encrypted and saved");
    }
    @PostMapping("/vault/{id}/reveal") public Map<String,String> reveal(@AuthenticationPrincipal Store.User user,@PathVariable String id) {
        String encrypted=ownedCiphertext(user,id);
        String secret=crypto.decrypt(encrypted,user.id()+":"+id);
        store.audit(user.email(),"CREDENTIAL_REVEALED","Owner requested decryption");
        return Map.of("secret",secret);
    }
    @GetMapping("/vault/{id}/ciphertext") public Map<String,String> ciphertext(@AuthenticationPrincipal Store.User user,@PathVariable String id) {
        return Map.of("ciphertext",ownedCiphertext(user,id),"algorithm","AES-256-GCM","format","v1:Base64(12-byte nonce + ciphertext + 16-byte authentication tag)");
    }
    private String ownedCiphertext(Store.User user,String id) {
        var list=store.jdbc().queryForList("SELECT encrypted_value FROM credentials WHERE id=? AND owner_id=?",String.class,id,user.id());
        if(list.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Credential not found");
        return list.get(0);
    }
    @DeleteMapping("/vault/{id}") public Map<String,String> delete(@AuthenticationPrincipal Store.User user,@PathVariable String id) {
        int count=store.jdbc().update("DELETE FROM credentials WHERE id=? AND owner_id=?",id,user.id());
        if(count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Credential not found");
        store.audit(user.email(),"CREDENTIAL_DELETED","Encrypted record removed"); return Map.of("message","Credential deleted");
    }
    @GetMapping("/sessions") public List<Map<String,Object>> sessions(@AuthenticationPrincipal Store.User user) { return tokens.sessions(user); }
    @PostMapping("/sessions/revoke-all") public Map<String,String> revoke(@AuthenticationPrincipal Store.User user) {
        tokens.revokeAll(user); return Map.of("message","All refresh sessions revoked. Existing access tokens expire within "+jwt.lifetime+" seconds.");
    }
    @GetMapping("/activity") public List<Map<String,Object>> activity(@AuthenticationPrincipal Store.User user) { return store.events(user.email(),false); }
    @GetMapping("/test/user") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Map<String,String> userTest() { return Map.of("message","Access granted to USER and ADMIN","rule","hasAnyRole('USER','ADMIN')"); }
    @GetMapping("/test/admin") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,String> adminTest() { return Map.of("message","Admin-only API access granted","rule","hasRole('ADMIN')"); }
    @GetMapping("/admin/users") @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String,Object>> users() { return store.users(); }
    @PostMapping("/admin/users") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createUser(@AuthenticationPrincipal Store.User admin,@Valid @RequestBody NewUser input) {
        if(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must fit within 72 UTF-8 bytes");
        Store.User user=store.createUser(input.email(),input.name(),passwords.encode(input.password()),input.role());
        store.audit(admin.email(),"ACCOUNT_CREATED","Created a "+input.role()+" account"); return store.publicUser(user);
    }
    @GetMapping("/admin/audit") @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String,Object>> audit(@AuthenticationPrincipal Store.User user) { return store.events(user.email(),true); }
}
