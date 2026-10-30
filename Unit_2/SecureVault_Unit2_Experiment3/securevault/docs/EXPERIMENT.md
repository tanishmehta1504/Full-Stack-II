# Unit 2 — Experiment 3
## SecureVault: JWT Authentication, RBAC and Encrypted Credential Management

**Combines:** Experiment 2.3.1 and Experiment 2.3.2  
**Student:** Tanish Mehta  
**Course:** Full Stack Development  
**COs:** CO2–BT2, CO3–BT3, CO4–BT4, CO5–BT5, CO6–BT6

## Aim

To develop secure backend APIs with JWT authentication and role-based authorization, protect stored sensitive credentials using AES encryption, and manage access/refresh token lifecycles through a working dashboard.

## Objectives and implementation mapping

| Experiment objective | Implementation | Where to demonstrate |
|---|---|---|
| Understand JWT authentication | Signed HS256 access tokens with issuer, subject, audience, role, issue time and expiry | Login; Tokens & sessions |
| Configure Spring Security | Stateless SecurityFilterChain, CSRF, authentication/authorization error handling | SecurityConfig.java |
| Implement authentication filters | OncePerRequestFilter validates JWT signature/claims and establishes SecurityContext | JwtAuthenticationFilter.java |
| Enforce RBAC | ROLE_USER / ROLE_ADMIN and @PreAuthorize on privileged methods | Access lab; Accounts |
| Implement AES encryption | AES-256-GCM, fresh nonce for every encryption, authenticated owner/record context | Add → Inspect → Reveal credential |
| Store third-party credentials securely | Encrypt the secret value before inserting into persistent H2 | Credential vault |
| Manage access and refresh tokens | Five-minute JWTs; opaque seven-day refresh tokens in HttpOnly cookies | Tokens & sessions |
| Handle refresh and rotation | One-use refresh tokens, database row locking, hash-only storage, replay revocation | Rotate tokens; integration tests |

## Software used

Java 17+, Spring Boot 3.5.16, Spring Security, Spring JDBC, H2, Java Cryptography Architecture, HTML/CSS/JavaScript, Maven, JUnit 5, MockMvc and Postman.

The frontend is served by the same Spring Boot application. This gives the project one server and one origin, and avoids an unnecessary separate frontend build environment.

## Theory

### Authentication versus authorization

Authentication answers “Who is making this request?” SecureVault verifies a password and issues a signed access token. Authorization answers “What may this identity do?” The server uses the authenticated user's role to determine whether a method may execute.

### JWT authentication

A JWT has a header, payload and signature. SecureVault signs its JWTs with HS256 using a randomly generated, 256-bit signing key. The payload is readable, so it contains identity and authorization information, never credential secrets. The backend validates the signature, expiration, issuer, audience and access-token type before trusting a token. Client-side display of decoded claims does not authenticate a token.

JWT access is stateless with respect to server HTTP sessions: Spring Security does not create a login session. Each protected request presents `Authorization: Bearer <access-token>`. The filter also resolves the subject to the current database user. The database stores refresh-session state to support revocation and rotation.

### Spring Security filter chain and method security

SecurityConfig constructs the filter chain. JwtAuthenticationFilter extends OncePerRequestFilter, checks Bearer tokens, and populates SecurityContext with the user and ROLE_USER/ROLE_ADMIN authorities. Unauthenticated requests to protected APIs receive 401. @EnableMethodSecurity activates @PreAuthorize annotations. Methods annotated with `@PreAuthorize("hasRole('ADMIN')")` return 403 to authenticated USER accounts.

Hiding an admin navigation link is a UI convenience, not the security control. Backend tests send direct requests and confirm the method-level restrictions.

### AES and credential confidentiality

AES is symmetric encryption: the same secret key encrypts and decrypts. AES-GCM also authenticates data, detecting ciphertext modification. Each saved credential uses:

- A separate 256-bit AES key (not the JWT signing key).
- A fresh 12-byte random nonce.
- A 128-bit authentication tag.
- Additional authenticated data: `ownerId:recordId`.

The database stores `v1:Base64(nonce + ciphertext + tag)`. Associated-data binding prevents ciphertext copied to another owner's record from decrypting successfully. The API further scopes every read/delete query by both credential ID and owner ID.

The secret value is encrypted. The provider, label and timestamps are metadata and remain plaintext. Authentication passwords use BCrypt because password verification does not require recovering the original password.

### Token lifecycle

1. A successful login issues a short-lived access JWT and a high-entropy refresh token.
2. The browser holds the access token in memory; the refresh token uses an HttpOnly, SameSite=Strict cookie.
3. Only the SHA-256 hash of the refresh token is persisted. High entropy prevents feasible guessing; BCrypt is used for low-entropy human passwords instead.
4. Refresh locks the database row, validates the token, marks it used and inserts a replacement in one transaction.
5. A replay of a used token revokes the entire refresh family. The revocation transaction commits even though the client receives 401.
6. The family expires at its original deadline; refresh does not create an unlimited sliding session.
7. Logout and revoke-all stop refresh. Already-issued stateless access JWTs remain valid until expiry.

CSRF protection covers state-changing requests because the browser automatically sends the refresh cookie. The client obtains a CSRF token from `/api/csrf` and sends its header on mutations. SameSite cookies are an additional defense.

## Database design

| Table | Purpose | Sensitive handling |
|---|---|---|
| app_users | ID, email, display name, role and join time | BCrypt password hash |
| credentials | Owner, provider, label and encrypted secret | AES-256-GCM ciphertext |
| refresh_tokens | Token hash, owner, family, expiry, used/revoked flags | SHA-256 hash; no raw refresh token |
| audit_events | Actor, action, non-secret detail and time | Does not store raw tokens or passwords |

## Procedure

1. Extract the project and verify Java 17+.
2. Start it using start.bat, start.sh or `java -jar securevault.jar`.
3. Open http://localhost:8080 and sign in using the ADMIN classroom account.
4. Add a sample credential and inspect the encrypted value.
5. Reveal the original sample value to demonstrate correct decryption.
6. Call shared and admin APIs from Access lab.
7. Create a USER account or sign in with the seeded USER account.
8. Confirm USER access to the shared endpoint and denial from the admin endpoint.
9. Verify each account sees only its own vault.
10. Rotate tokens and review the corresponding audit event.
11. Revoke sessions and confirm refresh is rejected.
12. Run `mvn test` or use the included Postman collection for repeatable checks.

## Expected observations

| Action | Expected observation |
|---|---|
| Correct password | 200 and access JWT; refresh cookie set |
| Incorrect password | 401 |
| No JWT on a protected API | 401 |
| Modified or expired JWT | 401 |
| USER calls admin endpoint | 403 |
| ADMIN calls admin endpoint | 200 |
| Save a credential | 201; database stores ciphertext |
| Owner reveals credential | 200; original value recovered |
| Different user accesses record | 404; record existence not disclosed |
| Ciphertext/owner context modified | GCM integrity verification fails |
| Valid refresh | 200; fresh access JWT and refresh cookie |
| Previously consumed refresh reused | 401; refresh family revoked |
| Refresh after logout/revocation | 401 |
| Mutation without CSRF token | 403 |

For the checks actually run during project creation, see TEST-RESULTS.md.

## Result

The project combines the two experiments in one application: JWT-secured APIs, method-level RBAC, encrypted credential storage and controlled token renewal. The dashboard provides a visible demonstration of each feature using backend responses and persistent data.

## Viva preparation

**Why is a JWT signed rather than encrypted here?**  
A signature lets the server verify integrity and authenticity. Its payload is not confidential, so secrets are excluded.

**What is the difference between 401 and 403?**  
401 means authentication is missing or invalid. 403 means the authenticated role lacks permission, or a request fails another security rule such as CSRF.

**Why does @EnableMethodSecurity matter?**  
It enables enforcement of annotations such as @PreAuthorize; annotations alone are insufficient.

**Why use AES-GCM?**  
It provides both confidentiality and tamper detection. A nonce must not be reused with the same key; SecureVault generates a fresh random nonce for each encryption.

**Why not encrypt passwords using AES?**  
The server only needs to verify passwords, not recover them. BCrypt stores a salted, computationally expensive hash.

**Why have two token types?**  
A short access lifetime limits the duration of exposure. A longer refresh session maintains usability and supports controlled renewal and revocation.

**Why does refresh-token replay revoke a family?**  
A consumed token appearing again can indicate theft. Revoking its descendants stops further renewal from that session.

**Does logging out immediately invalidate an access JWT?**  
No. This stateless design revokes refresh and clears the browser's in-memory access token. A copied access JWT remains valid until expiry. Immediate revocation needs additional server-side state.

**Can the administrator read everybody's secrets?**  
No. ADMIN can manage accounts and inspect the global audit log, but vault queries always require record ownership.

**Is the application a third-party OAuth provider/client?**  
No. It securely stores supplied OAuth token/credential values. It does not perform an external OAuth authorization-code exchange.

## References

- Spring Boot 3.5 requirements: https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Spring Security method security: https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html
- Spring Security JWT resource server concepts: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Oracle Java Cipher API: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/Cipher.html

SecureVault uses its own OncePerRequestFilter with Spring's NimbusJwtDecoder to make the authentication-filter step explicit for this experiment.
