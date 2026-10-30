# Verification results

Verified on 26 October 2026 using Java 17 and Maven.

## Automated tests

| Suite | Tests | Result |
|---|---:|---|
| Unit tests | 6 | PASSED |
| MockMvc integration | 5 | PASSED |
| Pact consumer | 1 | PASSED |
| Pact provider | 1 | PASSED |
| PostgreSQL Testcontainers | 0 | NOT_RUN |

All 13 executed tests passed, with no failures, errors or skipped tests. Pact consumer generated a contract that the provider verification checked against a running Spring Boot server.

## Packaged application checks

- Packaged JAR starts three independent healthy Spring Boot instances
- Packaged dashboard HTML, JavaScript and CSS are served
- Real stable/canary HTTP routing and promotion to 100 percent pass
- Candidate outage/regression causes automatic rollback; stable still responds
- An unhealthy candidate cannot begin a new rollout
- Packaged application accepts and persists a validated order

These checks used three real Java processes and HTTP requests. They did not use Docker.

## Checks still required

- PostgreSQL Testcontainers: run `mvn clean verify -Pdocker-tests` with Docker running.
- Docker image and Compose stack: run the included Docker launcher.
- Hosted GitHub Actions, GHCR publishing and deployment: configure the repository and runner using GITHUB-SETUP.md.
- Browser visual appearance: no browser-based visual inspection was available. Static dashboard assets were served successfully.

The dashboard displays recorded build evidence. It is not connected to a live GitHub account. Deployment files are included; no external deployment has been performed.
