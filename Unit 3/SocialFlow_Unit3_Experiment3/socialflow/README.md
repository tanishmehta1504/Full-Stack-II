# SocialFlow — Unit 3, Experiment 3

A complete classroom social-media management workspace combining Experiments 3.3.1 and 3.3.2. React + Redux Toolkit frontend, Spring Boot/Security backend, PostgreSQL, Kafka, JWT, Swagger, automated tests and CI/CD configuration.

## Start on Windows (recommended)

1. Extract the ZIP completely.
2. Install/start Docker Desktop with Linux containers; allow roughly 4 GB memory.
3. Double-click `start-docker.bat`. The first run downloads container images.
4. After startup, open **http://localhost:8083**. Allow Spring Boot another 30 seconds after containers start.
5. Sign in with `admin@socialflow.local` / `Admin@12345`.

macOS/Linux: run `bash start-docker.sh`. You do not need Node, Maven or Java on the host for the Docker route: the package contains the built application.

| Demo user | Password | Permission |
|---|---|---|
| admin@socialflow.local | Admin@12345 | All posts, team administration, failure simulation |
| editor@socialflow.local | Editor@12345 | Create/manage own posts, comment on all posts |
| viewer@socialflow.local | Viewer@12345 | Read content, calendar, analytics and team |

Demo mode must stay local. `DEMO_MODE=false` requires a random JWT_SECRET and a strong ADMIN_PASSWORD; start with a fresh database to avoid retaining seeded demo users. Existing users are never silently overwritten by a configuration change.

## Demonstration in 10 minutes

1. Overview → Create post → write a title and content → Save draft.
2. Open the draft, add a team comment, then sign in as Editor to see shared content. Editor cannot modify an Admin-owned post.
3. As Editor, create your own post and schedule it about one minute ahead. The calendar shows the scheduled date in your browser timezone.
4. Wait until due. The scheduler sends the durable event to Kafka; the consumer adds one demo publication and the dashboard changes to Published.
5. Open Analytics: published counts, seven-day chart, channel breakdown and the publication feed use actual stored records.
6. As Admin, create a post with “Test a publishing failure” checked. Retries use 1s, 2s and 4s backoff, then the DLQ marks the post Failed. Open it and Retry publishing to complete successfully.
7. Team → add an Editor or Viewer; disable that account to revoke its API access immediately, even for an already issued JWT.
8. Swagger: **http://localhost:8083/swagger-ui.html**. Call `/api/login`, copy `token`, select Authorize and paste the token (without the Bearer prefix).

## How publishing works

All channels are explicitly **demo channels**. They publish inside this application's database/feed, not LinkedIn, Instagram or X. No invented follower/engagement data is shown. Real provider APIs, OAuth approvals, rate limits and external idempotency adapters are deployment-specific extensions.

Post workflow: DRAFT → SCHEDULED → PUBLISHED, or SCHEDULED → FAILED → SCHEDULED. Cancellation returns scheduled posts to DRAFT and increments the generation so stale messages cannot publish. The database transaction stores posts and outbox entries together. The outbox marks a message sent only after a broker acknowledgement. Row locks plus the publication's unique post ID prevent duplicate demo publications.

## Build and test from source

Requires JDK 17+, Maven 3.9+, Node 22+ and npm. Run:

```bash
cd frontend
npm ci
npm test
npm run build
cd ..
mvn clean verify
```

React assets are emitted into Spring Boot's static resources. Maven packages the same-origin frontend and API together. Copy `target/socialflow-1.0.0.jar` to `socialflow.jar` before building the Docker image.

The default integration suite runs a real embedded KRaft Kafka broker with H2. CI sets TEST_DB_URL to a PostgreSQL service and runs the same workflow tests there. JaCoCo enforces **80% line coverage across the entire Java application**, with no class exclusions. Frontend tests enforce 80% coverage on the API/Redux module; this is not whole-UI coverage. See `docs/TEST-RESULTS.md` for actual measured results and unrun checks.

Frontend development: `npm run dev` inside frontend; Vite proxies `/api` to port 8083. Java-only: `java -jar socialflow.jar` or `start-java.bat`. Java-only uses a persistent H2 database; Kafka must be reachable at localhost:9092 for publishing. Without Kafka, drafts remain usable and scheduled events remain in the outbox. The recommended all-in-one Compose configuration does not expose Kafka to the host.

## Files and troubleshooting

- `frontend/src/main.jsx`: dashboard, composer, calendar, team and analytics.
- `frontend/src/store.js`: Redux Toolkit state and authenticated API calls.
- `src/main/java/com/socialflow`: backend services and controllers.
- `docs/EXPERIMENT.md`: report content, objective mapping and viva questions.
- `docs/DEPLOYMENT.md`: GitHub Actions and deployment setup.
- `docs/ARCHITECTURE.md`: API, security, reliability and limitations.
- `docs/TEST-RESULTS.md`: verification evidence.

Stop without deleting data: `docker compose stop`. Restart: `docker compose start`. Inspect failures: `docker compose logs app` and `docker compose logs kafka`. Port 8083 must be free. For a clean start, `docker compose down -v` deletes all database, Kafka and application volumes; use only when you intentionally want to erase the workspace.

JWT expires after 30 minutes; sign in again. Tokens are held in memory, so refreshing the browser also requires signing in. Team accounts belong to one shared classroom workspace, not separate tenants. No password-reset/email service is configured.

This is a working capstone with production-oriented patterns, not a claim of production certification. Use the deployment checklist before any real public use. No cloud deployment has been performed from this chat.
