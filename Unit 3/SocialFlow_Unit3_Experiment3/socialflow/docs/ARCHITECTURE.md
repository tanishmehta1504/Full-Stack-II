# Architecture and API

React + Redux Toolkit → authenticated same-origin REST API → Spring Security/JWT → Workspace service → PostgreSQL.

Scheduling transaction → durable outbox → Kafka topic `socialflow.posts` → consumer → atomic publication/post update. Failed delivery retries at 1, 2 and 4 seconds, then moves to `socialflow.posts.dlq`. The DLQ listener marks the matching generation Failed. A manual retry creates a new generation, leaving stale events harmless.

## API reference

| Method | Path | Access / purpose |
|---|---|---|
| POST | /api/login | Public; email/password → JWT and user |
| GET | /api/health | Public database liveness probe |
| GET | /api/me | Current member |
| GET | /api/workspace | Posts, counts, seven-day publication timestamps, audit activity |
| POST | /api/posts | Admin/Editor; title, content, channel |
| PUT | /api/posts/{id} | Admin or author; drafts only |
| POST | /api/posts/{id}/schedule | Admin or author; dueAt epoch milliseconds, failDemo boolean |
| POST | /api/posts/{id}/cancel | Admin or author; scheduled posts only |
| GET | /api/posts/{id}/comments | All authenticated members |
| POST | /api/posts/{id}/comments | Admin/Editor; body |
| GET | /api/members | All authenticated members |
| POST | /api/members | Admin; name, email, password, role EDITOR or VIEWER |
| POST | /api/members/{id}/toggle | Admin; enable/disable non-admin member |
| GET | /v3/api-docs | OpenAPI specification |

## Security

Passwords use BCrypt. Access tokens use HS256 signed JWTs with issuer and expiration validation. Non-demo deployments require a base64 random signing key; demo mode generates a persistent key in `.data/jwt.key`. Roles and active state are loaded from the database for each API request, so deactivation takes effect immediately. Method annotations enforce role permissions; post mutations additionally enforce ownership. Input validation, parameterized SQL, React escaping, same-origin API design and a CSP are included.

Tokens are memory-only; no cookie-based authentication is used. CSRF protection is disabled for the stateless bearer API. CORS is not broadly enabled. Login throttling and secure public ingress are deployment responsibilities. The workspace is shared, so Viewer intentionally sees team email addresses and shared content.

## Analytics semantics

Total/status counts query all posts. The post list and channel breakdown show at most the latest 500 posts. The seven-day chart uses actual publication timestamps and the browser timezone. Success percentage means Published / (Published + Failed), not eventual delivery probability. No engagement metrics are invented.

## Known limits

Only text posts and local demo channels are supported. The calendar is month-based. No uploads, real provider OAuth, email invitations, password reset, approval policy or multi-tenant organizations are included. A single shared database is required for worker coordination. API health checks database availability, not full Kafka end-to-end readiness; the smoke test separately verifies publishing.
