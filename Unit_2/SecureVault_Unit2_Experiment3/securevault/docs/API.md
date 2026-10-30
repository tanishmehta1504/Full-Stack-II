# API and Postman guide

Base URL: `http://localhost:8080`.

## Request conventions

1. GET `/api/csrf` first. Keep its cookie and the returned `token`.
2. On POST and DELETE, send `X-XSRF-TOKEN: <token>` and retain cookies.
3. Login with JSON `{"email":"admin@securevault.local","password":"Admin@12345"}`.
4. Use `Authorization: Bearer <accessToken>` on protected endpoints.
5. Refresh/logout use the HttpOnly `sv_refresh` cookie, not a Bearer refresh JWT.
6. JSON bodies use `Content-Type: application/json`.

The included Postman collection handles the CSRF token and access-token variables. Postman's cookie jar retains cookies automatically. Import the JSON, ensure the Java server is running, then run the collection in order. Keep cookie support enabled and use the same hostname throughout (`localhost`), since cookies are host-bound. The collection creates and deletes a sample vault record. It does not contact third-party providers.

## Endpoint reference

| Method | Path | Access | Request / response |
|---|---|---|---|
| GET | /api/csrf | Public | `token`, `headerName`; sets CSRF cookie |
| POST | /api/auth/login | Public + CSRF | email/password → JWT, expiresIn, user; refresh cookie |
| POST | /api/auth/refresh | Refresh cookie + CSRF | New JWT + rotated cookie |
| POST | /api/auth/logout | Refresh cookie + CSRF | Revokes current refresh family and clears cookie |
| GET | /api/me | USER, ADMIN | Public user profile; no password hash |
| GET | /api/vault | USER, ADMIN | Own credential metadata only |
| POST | /api/vault | USER, ADMIN + CSRF | provider, label, secret → saved ID |
| POST | /api/vault/{id}/reveal | Owner + CSRF | `secret` plaintext, no-store response |
| GET | /api/vault/{id}/ciphertext | Owner | `ciphertext`, `algorithm`, `format` |
| DELETE | /api/vault/{id} | Owner + CSRF | Delete own credential |
| GET | /api/sessions | USER, ADMIN | Own active refresh sessions |
| POST | /api/sessions/revoke-all | USER, ADMIN + CSRF | Revoke own refresh sessions |
| GET | /api/activity | USER, ADMIN | Latest 100 own audit events |
| GET | /api/test/user | USER, ADMIN | Role test, 200 |
| GET | /api/test/admin | ADMIN | Role test, 200 or 403 |
| GET | /api/admin/users | ADMIN | User profiles |
| POST | /api/admin/users | ADMIN + CSRF | email, name, password, role (USER or ADMIN) |
| GET | /api/admin/audit | ADMIN | Latest 100 global audit events |

Authentication and refresh failures use 401; role/CSRF failures use 403. Missing or foreign vault IDs use 404. Invalid input uses 400. Duplicate email uses 409. The per-IP login limit returns 429.

## Postman collection sequence

The collection gets CSRF state, logs in as ADMIN, checks admin access, saves/inspects/reveals/deletes a credential, rotates tokens, signs out, logs in as USER, checks shared access, verifies admin denial, verifies unauthenticated denial, and signs out.

The integration suite additionally tests expired/modified JWTs, cross-user vault access, encryption tamper detection, refresh replay, password hashing, CSRF failure, duplicate accounts, and revoke-all.

## Reproduce refresh-token replay manually

Use a disposable local test session. After login, copy the `sv_refresh` cookie value **A** from Postman's cookie manager. Call refresh once, receiving **B**. Replace the cookie with **A** and call refresh: expect 401. Replace it with **B** and call refresh: expect 401 because replay revoked the family. Preserve the CSRF cookie/header throughout. Do not share these raw token values or store them in screenshots.

## HTTP caching and browser storage

Spring Security emits no-store cache-control headers for protected responses. The dashboard never writes access tokens or secrets into localStorage/sessionStorage. A secret is shown only after an owner-authorized request and cleared from the dialog after 20 seconds. Browser memory is still readable by scripts in that page, so CSP and HTML escaping are also used.
