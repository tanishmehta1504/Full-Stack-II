# Verification report

Verified on 26 October 2026.

## Results

| Check | Result |
|---|---|
| com.socialflow.TokenTest | 3 tests passed |
| com.socialflow.WorkflowTest | 10 tests passed |
| Frontend API/Redux tests | 5 passed |
| React dashboard workflow tests | 3 passed |
| Whole-backend Java line coverage | **93.63%** (294/314 lines), 80% gate passed |
| Whole-backend Java branch coverage | 80.56% |
| Frontend API/Redux module coverage | 100% lines, statements, functions and branches |
| Production React build | Passed |
| Packaged JAR | Starts; login, draft persistence and React assets verified |
| Python, shell, Compose and Actions syntax | Parsed/checked |

## Test scope

The 10 integration tests run a real Spring Boot HTTP server, H2 database and embedded KRaft Kafka broker. They cover role and ownership restrictions, account creation/revocation, collaborative comments, schedule timing, cancellation, duplicate events, failure retries/DLQ/replay, validation, Swagger and dashboard responses. Three token tests cover missing/short keys, expiry and issuer validation.

Eight frontend tests cover token transport, API error/expiry handling, Redux loading and logout cleanup, login-to-draft UI flow, calendar/analytics/team navigation and Viewer restrictions. The 100% frontend figure applies only to `store.js`, not all React components.

The final coverage report combines the integration and token test runs over the same Java application classes. The whole-backend JaCoCo gate has no application-class exclusions. A clean CI run executes all 13 backend tests in one build.

## Evidence included

- `coverage/backend/index.html`: JaCoCo report; open locally in a browser.
- `coverage/frontend/index.html`: coverage report for API/Redux module.

## Not verified here

- Docker image/Compose execution: Docker is unavailable in this runtime.
- PostgreSQL execution: CI is configured to run the same tests with a real PostgreSQL service, but local verification used H2.
- Hosted GitHub Actions, GHCR publishing and VM deployment: repository/runner configuration is required.
- Browser visual inspection: managed preview could not launch the JVM. React component behavior was tested in jsdom, not a real browser.
- Real social-media publishing: deliberately not implemented; all publishing is to the clearly labelled local demo feed.

No public deployment was performed. See DEPLOYMENT.md for the remaining setup and operational requirements.
