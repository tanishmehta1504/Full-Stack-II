# SecureVault
## Unit 2 · Experiment 3 · Combined 2.3.1 and 2.3.2

A Spring Boot project with a working browser dashboard for JWT authentication, role-based authorization, AES-encrypted credential storage, and rotating refresh tokens.

## Start here — Windows

1. Install **JDK 17 or newer** if you do not already have it. Run `java -version` in a terminal to check.
2. **Extract the entire ZIP** into a normal folder, such as `Documents\securevault`. Do not run it from inside the ZIP.
3. Open the extracted `securevault` folder and double-click **start.bat**.
4. Leave that terminal open. Wait for `Started SecureVaultApplication`.
5. Open **http://localhost:8080** in your browser.
6. Click **Use Admin**, then **Sign in**.

The included `securevault.jar` is already built. You do **not** need Maven, Node.js, npm, MySQL, or an internet connection to run it. Java is required. On macOS/Linux run `sh start.sh`. You can also run `java -jar securevault.jar` from this folder.

| Role | Email | Password |
|---|---|---|
| ADMIN | admin@securevault.local | Admin@12345 |
| USER | user@securevault.local | User@12345 |

These are intentionally public **classroom demo accounts**, not real credentials. The default server listens only on `127.0.0.1`.

## What works

- Login with BCrypt password verification and signed, five-minute access JWTs.
- Responsive dashboard with real counts and activity from the backend.
- Add, search, reveal, inspect ciphertext, and delete your own credentials.
- AES-256-GCM encryption, unique random 96-bit nonces, 128-bit authentication tags, owner/record binding.
- Access token kept only in browser memory; refresh token in an HttpOnly, SameSite=Strict cookie.
- Refresh-token rotation, hashed database storage, fixed seven-day family expiry, replay detection and family revocation.
- Inspect current JWT claims, manually rotate tokens, list/revoke your refresh sessions, and sign out.
- USER/ADMIN access tester with real 200, 401 and 403 responses.
- ADMIN account creation, role assignment and workspace audit log.
- CSRF protection on state-changing requests, including login, refresh and logout.
- Automatic access-token renewal when a normal API request receives 401. The countdown reaching zero alone does not make a request.
- Persistence: the H2 database and encryption keys survive app restarts.

## Five-minute classroom demonstration

1. Sign in as ADMIN. The overview reports real data; the initial vault is empty.
2. Add a credential: provider `GitHub`, label `Full Stack Lab`, secret `sample-token-123`.
3. In **Credential vault**, click **Inspect**. Show the encrypted database value. Click **Reveal** to decrypt it; the displayed secret clears after 20 seconds.
4. Open **Access lab**. The shared and admin endpoints return **200**. The request without a token returns **401**.
5. Open **Tokens & sessions**. Click **Rotate tokens now**; the JWT countdown resets and the audit log records rotation.
6. Open **Accounts** and create a USER account, or use the included USER demo account.
7. Sign out, then sign in as USER. The admin test returns **403**. The admin's credential does not appear in this user's vault.
8. Add a sample credential as USER and inspect/reveal it. This demonstrates both users own separate encrypted records.
9. Click **Revoke all** in Tokens & sessions. Existing access JWTs work until expiry, but refresh no longer works; after expiry a new sign-in is required.
10. Show `docs/EXPERIMENT.md`, the Java source, and the test report when explaining the implementation.

## Edit in VS Code / IntelliJ

Open the whole `securevault` folder. The entry point is `src/main/java/com/securevault/SecureVaultApplication.java`.

Source builds require **Maven 3.6.3+** and internet access for the first dependency download:

```sh
mvn test
mvn clean package
java -jar target/securevault-1.0.0.jar
```

To update the JAR used by the start scripts after editing, copy `target/securevault-1.0.0.jar` to `securevault.jar` in the project root. Alternatively use `mvn spring-boot:run` during development.

The UI is vanilla HTML/CSS/JavaScript served by Spring Boot from `src/main/resources/static`. There is no separate frontend server, build step, or CORS configuration.

## Project structure

```text
securevault/
  start.bat / start.sh            Start the included runnable application
  securevault.jar                Prebuilt Spring Boot JAR
  pom.xml                        Java 17 / Spring Boot 3.5.16 dependencies
  README.md                      Setup and demo guide
  docs/
    EXPERIMENT.md                 Aim, theory, implementation, results and viva
    API.md                       Endpoint reference and Postman instructions
    SecureVault.postman_collection.json
    TEST-RESULTS.md               Verification results and limitations
  src/main/java/com/securevault/
    SecureVaultApplication.java   Entry point
    SecurityConfig.java          Stateless filter chain, CSRF, method security
    JwtAuthenticationFilter.java Bearer token authentication filter
    JwtService.java              JWT issue and signature/claims validation
    TokenService.java            Refresh hashing, rotation, replay and revocation
    CryptoService.java           AES-256-GCM encrypt/decrypt
    KeyMaterial.java             Separate persistent signing/encryption keys
    ApiController.java           REST endpoints and role restrictions
    ApiErrors.java               Consistent validation and error responses
    Store.java                   Parameterized database access and audit events
    DemoData.java                Local classroom accounts
  src/main/resources/
    application.properties       Configurable application settings
    schema.sql                   H2 schema
    static/                      Dashboard HTML, CSS, JS and favicon
  src/test/java/com/securevault/
    SecurityIntegrationTest.java Security regression tests
```

## Persistence and configuration

On first launch, the app creates `.data/securevault.mv.db` and two independent random 256-bit keys in `.secrets/`. **Keep the AES key with this database**: deleting/changing it makes existing ciphertext undecryptable. The JWT key signs access tokens. Do not submit your own populated `.data` or `.secrets` folders. The supplied ZIP contains neither.

Environment variables override defaults:

| Variable | Default | Meaning |
|---|---|---|
| PORT | 8080 | HTTP port |
| SERVER_ADDRESS | 127.0.0.1 | Local bind address |
| ACCESS_SECONDS | 300 | Access JWT lifetime |
| REFRESH_SECONDS | 604800 | Refresh family lifetime (7 days) |
| KEYS_DIR | .secrets | Local key directory |
| JWT_KEY / AES_KEY | generated local keys | Independent Base64-encoded 32-byte keys |
| DEMO_MODE | true | Create the two classroom accounts on first launch |
| SECURE_COOKIES | false | Set true when served over HTTPS |
| DATABASE_URL | file-based H2 | JDBC connection URL |
| DATABASE_USER / DATABASE_PASSWORD | sa / empty | Database account settings |

Example in PowerShell, to demonstrate expiry quickly:

```powershell
$env:ACCESS_SECONDS="30"
java -jar securevault.jar
```

The refresh UI assumes the documented seven-day default in its explanatory text; each actual session expiry is read from the backend.

## Important implementation boundaries

- This is a **local classroom application**, not a complete production identity provider. Demo credentials and local key files are intentional for setup simplicity.
- Before any network deployment: remove seeded demo accounts from the database (setting DEMO_MODE=false does not remove existing accounts), create a deliberate administrator bootstrap process, use HTTPS/Secure cookies, managed keys, production database credentials, backups, centralized rate limiting, and a durable external audit sink. OS administrators with both local keys and database can decrypt data.
- AES encrypts stored OAuth credentials; this experiment does **not** connect to GitHub/Google or implement a third-party OAuth authorization-code flow.
- Passwords use one-way BCrypt hashing, not reversible AES encryption. Refresh tokens are random secrets stored as SHA-256 hashes, not JWTs.
- Access JWTs are stateless; revocation/logout stops refresh immediately but does not revoke already-issued access JWTs. They expire within the configured lifetime. Immediate access revocation would need an additional denylist/session check.
- The login throttle is per IP and per process (20 attempts/minute). No forwarded IP headers are trusted. This is not a distributed production rate limiter.
- Audit logs intentionally omit secret values, raw tokens and passwords. Credential metadata such as provider and label is stored in plaintext; the secret value is encrypted.
- Local Windows key-file protection depends on the folder's NTFS permissions. On POSIX systems, the app restricts the secrets directory and files.

## Troubleshooting

- **`java` is not recognized:** install JDK 17+ and reopen the terminal.
- **Port 8080 is in use:** stop the other app, or set `PORT=8081` and open http://localhost:8081.
- **Cannot connect:** keep start.bat running and use the HTTP localhost address. Do not open index.html directly.
- **Sign-in fails after revocation or expiry:** sign in again; do not use an old refresh cookie in Postman.
- **Data appears to disappear:** launch from the same project folder; database/key paths are relative to the working directory.
- **Reset the classroom demo:** stop the server first, then remove both `.data` and `.secrets` from your extracted project folder. This permanently removes demo data. Restart to create a fresh database.
