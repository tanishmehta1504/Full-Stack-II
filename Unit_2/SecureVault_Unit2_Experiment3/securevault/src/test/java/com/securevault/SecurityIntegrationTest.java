package com.securevault;

import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:securitytests;DB_CLOSE_DELAY=-1","app.keys-dir=target/test-keys","app.demo=true"})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @Autowired CryptoService crypto;
    @Autowired KeyMaterial keys;
    record Auth(String access,Cookie refresh) {}
    RequestPostProcessor csrf() throws Exception {
        var response=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String token=json.readTree(response.getContentAsString()).get("token").asText();
        String header=response.getHeader("Set-Cookie");
        assertNotNull(header);
        Cookie cookie=new Cookie("XSRF-TOKEN",header.split(";",2)[0].split("=",2)[1]);
        return request -> {
            var cookies=new ArrayList<Cookie>();
            if(request.getCookies()!=null) cookies.addAll(Arrays.asList(request.getCookies()));
            cookies.add(cookie);request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN",token);return request;
        };
    }
    Auth login(String role) throws Exception {
        String email=role+"@securevault.local";
        String password=role.equals("admin")?"Admin@12345":"User@12345";
        var r=mvc.perform(post("/api/auth/login").with(csrf()).with(req->{req.setRemoteAddr(UUID.randomUUID().toString());return req;})
            .contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password",password))))
            .andExpect(status().isOk()).andReturn();
        String raw=r.getResponse().getHeader("Set-Cookie");
        assertNotNull(raw);assertTrue(raw.contains("HttpOnly"));assertTrue(raw.contains("SameSite=Strict"));
        String value=raw.split(";",2)[0].split("=",2)[1];
        return new Auth(json.readTree(r.getResponse().getContentAsString()).get("accessToken").asText(),new Cookie("sv_refresh",value));
    }
    @Test void unauthenticatedAndTamperedRequestsFail() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        Auth a=login("user");String[] p=a.access.split("\\.");
        String payload=Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"fake-admin\"}".getBytes());
        mvc.perform(get("/api/me").header("Authorization","Bearer "+p[0]+"."+payload+"."+p[2])).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization","Bearer nonsense")).andExpect(status().isUnauthorized());
    }
    @Test void rolesAreEnforcedByBackend() throws Exception {
        Auth user=login("user"),admin=login("admin");
        mvc.perform(get("/api/test/user").header("Authorization","Bearer "+user.access)).andExpect(status().isOk());
        mvc.perform(get("/api/test/admin").header("Authorization","Bearer "+user.access)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+user.access)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header("Authorization","Bearer "+user.access)).andExpect(status().isForbidden());
        mvc.perform(get("/api/test/admin").header("Authorization","Bearer "+admin.access)).andExpect(status().isOk());
    }
    @Test void csrfIsRequiredIncludingLoginAndRefresh() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"admin@securevault.local\",\"password\":\"Admin@12345\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh")).andExpect(status().isForbidden());
        Auth user=login("user");mvc.perform(post("/api/sessions/revoke-all").header("Authorization","Bearer "+user.access)).andExpect(status().isForbidden());
    }
    @Test void bearerRequestsDoNotClearBrowserCsrfCookie() throws Exception {
        Auth user=login("user");
        mvc.perform(get("/api/me").with(csrf()).header("Authorization","Bearer "+user.access))
            .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
    }
    @Test void browserCsrfCookieFlowWorks() throws Exception {
        var c=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn().getResponse();
        String csrfToken=json.readTree(c.getContentAsString()).get("token").asText();
        String header=c.getHeader("Set-Cookie");assertNotNull(header);
        Cookie cookie=new Cookie("XSRF-TOKEN",header.split(";",2)[0].split("=",2)[1]);
        mvc.perform(post("/api/auth/login").cookie(cookie).header("X-XSRF-TOKEN",csrfToken).contentType("application/json")
            .content("{\"email\":\"user@securevault.local\",\"password\":\"User@12345\"}")).andExpect(status().isOk());
    }
    @Test void vaultEncryptsAtRestAndIsolatesOwners() throws Exception {
        Auth user=login("user"),admin=login("admin");String secret="sample-oauth-secret-"+UUID.randomUUID();
        var saved=mvc.perform(post("/api/vault").with(csrf()).header("Authorization","Bearer "+user.access).contentType("application/json")
            .content(json.writeValueAsString(Map.of("provider","GitHub","label","Test credential","secret",secret)))).andExpect(status().isCreated()).andReturn();
        String id=json.readTree(saved.getResponse().getContentAsString()).get("id").asText();
        String stored=db.queryForObject("SELECT encrypted_value FROM credentials WHERE id=?",String.class,id);
        assertTrue(stored.startsWith("v1:"));assertFalse(stored.contains(secret));
        var list=mvc.perform(get("/api/vault").header("Authorization","Bearer "+user.access)).andExpect(status().isOk()).andReturn();
        assertFalse(list.getResponse().getContentAsString().contains(secret));
        mvc.perform(post("/api/vault/"+id+"/reveal").with(csrf()).header("Authorization","Bearer "+user.access)).andExpect(status().isOk()).andExpect(jsonPath("$.secret").value(secret));
        mvc.perform(post("/api/vault/"+id+"/reveal").with(csrf()).header("Authorization","Bearer "+admin.access)).andExpect(status().isNotFound());
        mvc.perform(get("/api/vault/"+id+"/ciphertext").header("Authorization","Bearer "+admin.access)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/vault/"+id).with(csrf()).header("Authorization","Bearer "+admin.access)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/vault/"+id).with(csrf()).header("Authorization","Bearer "+user.access)).andExpect(status().isOk());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM credentials WHERE id=?",Integer.class,id));
    }
    @Test void aesUsesRandomNoncesAndRejectsTamperingOrWrongContext() {
        String a=crypto.encrypt("test secret","owner:record"),b=crypto.encrypt("test secret","owner:record");
        assertNotEquals(a,b);assertEquals("test secret",crypto.decrypt(a,"owner:record"));
        assertThrows(IllegalStateException.class,()->crypto.decrypt(a,"another:record"));
        byte[] bytes=Base64.getDecoder().decode(a.substring(3));bytes[bytes.length-1]^=1;
        assertThrows(IllegalStateException.class,()->crypto.decrypt("v1:"+Base64.getEncoder().encodeToString(bytes),"owner:record"));
    }
    @Test void rotationRejectsReplayAndRevokesReplacement() throws Exception {
        Auth a=login("user");String hash=TokenService.hash(a.refresh.getValue());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE token_hash=?",Integer.class,hash));
        var r=mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(a.refresh)).andExpect(status().isOk()).andReturn().getResponse();
        String value=r.getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1];assertNotEquals(a.refresh.getValue(),value);
        assertTrue(db.queryForObject("SELECT used FROM refresh_tokens WHERE token_hash=?",Boolean.class,hash));
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(a.refresh)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(new Cookie("sv_refresh",value))).andExpect(status().isUnauthorized());
    }
    @Test void logoutRevokesRefreshButJwtRemainsValidUntilExpiry() throws Exception {
        Auth a=login("user");
        mvc.perform(post("/api/auth/logout").with(csrf()).cookie(a.refresh)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(a.refresh)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization","Bearer "+a.access)).andExpect(status().isOk());
    }
    @Test void expiredRefreshTokensAreRejected() throws Exception {
        Auth a=login("user");db.update("UPDATE refresh_tokens SET expires_at=? WHERE token_hash=?",System.currentTimeMillis()-1000,TokenService.hash(a.refresh.getValue()));
        mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(a.refresh)).andExpect(status().isUnauthorized());
    }
    @Test void expiredJwtAndWrongAudienceAreRejected() throws Exception {
        String owner=db.queryForObject("SELECT id FROM app_users WHERE email='user@securevault.local'",String.class);
        for(boolean expired:List.of(true,false)) {
            var claims=new JWTClaimsSet.Builder().issuer("securevault").subject(owner).audience(expired?"securevault-api":"other-api")
                .claim("token_use","access").issueTime(Date.from(Instant.now().minusSeconds(120))).expirationTime(Date.from(Instant.now().plusSeconds(expired?-60:60))).build();
            SignedJWT jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),claims);jwt.sign(new MACSigner(keys.jwt));
            mvc.perform(get("/api/me").header("Authorization","Bearer "+jwt.serialize())).andExpect(status().isUnauthorized());
        }
    }
    @Test void onlyAdminCanCreateAccountsAndPasswordsAreHashed() throws Exception {
        Auth user=login("user"),admin=login("admin");String email="test-"+UUID.randomUUID()+"@example.com";
        String body=json.writeValueAsString(Map.of("email",email,"name","New User","password","NewUser@12345","role","USER"));
        mvc.perform(post("/api/admin/users").with(csrf()).header("Authorization","Bearer "+user.access).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/users").with(csrf()).header("Authorization","Bearer "+admin.access).contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertTrue(db.queryForObject("SELECT password_hash FROM app_users WHERE email=?",String.class,email).startsWith("$2"));
        mvc.perform(post("/api/admin/users").with(csrf()).header("Authorization","Bearer "+admin.access).contentType("application/json").content(body)).andExpect(status().isConflict());
    }
    @Test void revokeAllStopsEveryRefreshSession() throws Exception {
        Auth a=login("admin"),b=login("admin");
        mvc.perform(post("/api/sessions/revoke-all").with(csrf()).header("Authorization","Bearer "+a.access)).andExpect(status().isOk());
        for(Auth session:List.of(a,b))mvc.perform(post("/api/auth/refresh").with(csrf()).cookie(session.refresh)).andExpect(status().isUnauthorized());
    }
    @Test void invalidVaultInputAndIncorrectPasswordAreRejected() throws Exception {
        Auth a=login("user");
        mvc.perform(post("/api/vault").with(csrf()).header("Authorization","Bearer "+a.access).contentType("application/json").content("{\"provider\":\"\",\"label\":\"x\",\"secret\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"email\":\"user@securevault.local\",\"password\":\"incorrect\"}")).andExpect(status().isUnauthorized());
    }
    @Test void staticDashboardIsPublicAndHasSecurityHeaders() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(header().exists("Content-Security-Policy"));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/style.css")).andExpect(status().isOk());
    }
}
