# Unit 3 — Experiment 2
## ReleasePilot: Continuous Integration and Canary Deployment

**Combines:** Experiments 3.2.1 and 3.2.2  
**CO mapping:** CO6 — BT6

## Aim

To design and implement an automated CI/CD pipeline for a Spring Boot application, using layered testing, Docker packaging, GitHub Actions and controlled canary deployment.

## Objectives

1. Understand continuous integration and early defect detection.
2. Write JUnit 5 unit tests and MockMvc integration tests.
3. Verify an HTTP service contract using Pact.
4. Use Testcontainers to test against a real PostgreSQL database.
5. Containerize the application and publish verified images.
6. Deploy a small candidate cohort before full promotion, with rollback on failure.

## Requirements

Java 17+, Spring Boot, JUnit 5, MockMvc, Pact JVM, Testcontainers, Docker/Compose, Maven, Python 3 for automation scripts, and a GitHub account. Full CD additionally needs a configured dedicated Linux deployment runner.

## Theory

### Continuous Integration

CI automatically builds and tests proposed code changes. A passing build gives evidence that the change meets the checks implemented in the project. It does not prove the absence of every defect.

ReleasePilot runs its quality gates on main pushes and pull requests. Failure blocks container publication and deployment. Reports and consumer contracts are retained as workflow artifacts.

### Unit testing

A unit test checks a small piece of logic independently. QuoteServiceTest verifies calculation boundaries and routing behavior. These tests do not need a web server, database or Docker.

### Integration testing

Integration tests check components together. ApiIntegrationTest loads the real Spring application and uses MockMvc to exercise routes, validation and database persistence. The default test database is isolated H2.

PostgresContainerCheck uses Testcontainers to start a real PostgreSQL container. The application connects using dynamically supplied JDBC settings. This checks the database behavior against the actual engine used by the Docker release services.

### Consumer-driven contract testing

A contract records the response a consumer expects from a provider. The dashboard needs a quote containing sku, quantity, unitPrice, total and currency. The consumer Pact test calls a Pact mock server using that contract and writes a pact file. Provider verification then calls the actual running Spring backend and compares its response against the contract.

This is not just checking a hard-coded JSON file: both consumer generation and provider verification execute. The Java consumer represents the dashboard's HTTP expectations; browser JavaScript is not run by this test.

A Pact Broker is useful for multi-team version exchange, but is not needed here. Maven test generates files in target/pacts; Maven verify runs the provider phase afterward.

### Continuous Deployment and Docker

Docker packages the application and its Java runtime into a consistent image. The supplied image runs as a non-root user and exposes a healthcheck. GitHub Actions builds and tests the code before publishing a uniquely tagged image to GHCR. The deployment job consumes its immutable digest.

With a configured runner and ENABLE_CD=true, successful main-branch builds deploy automatically. If manual environment approval is enabled, delivery waits for that approval. Without a configured target, the workflow remains a CI/image pipeline and CD is skipped explicitly.

### Canary releases

A canary release gives a small user cohort access to a candidate while most traffic remains on stable. The gateway uses a deterministic hash of a visitor key to route its sample quote endpoint. At 10%, approximately one tenth of visitor keys go to candidate. A small observed sample will not always equal exactly 10%.

ReleasePilot allows 0%, 10%, 50% and 100% candidate traffic. Both releases must pass a quote probe. Promotion above 10% requires five successful candidate requests and zero candidate errors in the current stage. A failed candidate probe or routed error triggers automatic rollback to stable.

These thresholds and a ten-second CD observation window are intentionally small for a classroom demonstration. Production deployment policies should use realistic traffic, SLOs, sufficient observation time and infrastructure redundancy.

## Architecture

| Component | Responsibility |
|---|---|
| Dashboard/gateway | Display evidence, control allocation, route quote traffic and record release events |
| Stable backend | Serve the last stable application image |
| Candidate backend | Serve the proposed image |
| PostgreSQL | Shared sample order database for the two backend releases |
| Gateway H2 database | Durable allocation state, request metrics and release history |
| GitHub-hosted runners | Tests, container smoke checks and registry publishing |
| Dedicated deployment runner | Pull verified digest and perform controlled rollout |

Only `/traffic/api/quote` is routed between releases in this lab. Control endpoints and dashboard assets are served by the gateway itself.

## Implementation mapping

| Objective | File/mechanism |
|---|---|
| Unit tests | QuoteServiceTest / JUnit 5 |
| API integration tests | ApiIntegrationTest / MockMvc |
| Contract tests | DashboardConsumerPactTest + QuoteProviderIT |
| Real infrastructure | PostgresContainerCheck / PostgreSQLContainer |
| Automated CI | .github/workflows/pipeline.yml: verify |
| Containerization | Dockerfile and compose.yaml |
| Registry publishing | image job / GHCR |
| Automatic deployment | deploy job + scripts/deploy.py |
| Canary and rollback | RoutingPolicy and RolloutService |
| Observable evidence | scripts/evidence.py and dashboard Quality gates |

## Procedure

1. Start the Compose stack and open localhost:8082.
2. Open Quality gates and distinguish recorded passes from checks marked NOT_RUN.
3. Inspect the sample quote API in API playground.
4. Run `mvn clean verify` and inspect unit, integration and Pact results.
5. With Docker running, run `mvn clean verify -Pdocker-tests` to include PostgreSQL.
6. Open Canary rollout and start 10% candidate traffic.
7. Send 200 synthetic visitors and inspect the measured distribution.
8. Promote to 100%; send another batch to verify candidate routing.
9. Restart the candidate with the failure-injection flag and observe automatic rollback.
10. Restore candidate health.
11. Push the source to GitHub and inspect the workflow test artifacts and GHCR image.
12. Configure a dedicated runner and ENABLE_CD to demonstrate automated deployment.

## Expected results

| Action | Expected observation |
|---|---|
| Valid quote for quantity 2 | Total 998 INR |
| Invalid quantity | Validation failure |
| Changed incompatible response | Pact verification fails |
| Docker unavailable with docker-tests profile | Full infrastructure gate fails, not silently passes |
| Candidate/stable unavailable | Canary start blocked |
| Promotion without sufficient samples | HTTP 409 |
| Healthy candidate with sufficient successes | Promotion accepted |
| Candidate fails during exposure | Automatic 0% candidate allocation |
| Test failure in CI | Downstream image/deployment jobs do not proceed |
| No deployment runner/ENABLE_CD | Hosted deployment not performed |

## Result

The project provides layered test automation, Docker packaging, GitHub Actions definitions and working local canary controls. TEST-RESULTS.md separates executed checks from Docker/hosted steps requiring verification on the user's infrastructure.

## Viva questions

**CI versus CD?** CI validates changes through builds and tests. CD delivers successful builds to an environment; continuous deployment makes that delivery automatic after its gates pass.

**Unit versus integration testing?** Unit tests isolate logic. Integration tests verify components working together, such as HTTP routing, validation and a database.

**Why MockMvc?** It exercises Spring MVC endpoints without requiring a separately managed external web server, while using real Spring configuration.

**Why Pact?** It checks that a service continues to meet the consumer's agreed HTTP expectations and helps detect incompatible changes before deployment.

**Why Testcontainers instead of only H2?** Database engines can differ in SQL and behavior. A temporary PostgreSQL container tests the actual engine used by the backend releases.

**What is an image digest?** A content-based image identifier. Deploying by digest ties the running container to the exact published image, unlike a mutable tag.

**What is a canary?** A small cohort sent to a candidate release before increasing exposure.

**Is the traffic split exactly 10 requests out of every 100?** No. Allocation is based on visitor-key hashing. A sample is approximate; the same key stays in its cohort for a fixed percentage.

**What triggers rollback here?** A failed candidate HTTP probe or a recorded non-200 candidate response while candidate traffic is active.

**Does rollback undo database changes?** No. It changes routing for future requests. Schema and data compatibility must be planned separately.

**Why does the dashboard say NOT_RUN?** The packaged evidence has no report for that test layer. It avoids presenting unexecuted checks as passes.

**Is this production-ready?** It demonstrates the mechanisms. Production needs stronger authentication, availability, rollout observations, migration management and operational controls.

## References

- Pact consumer JUnit 5: https://docs.pact.io/implementation_guides/jvm/consumer/junit5
- Pact provider JUnit 5: https://docs.pact.io/implementation_guides/jvm/provider/junit5
- PostgreSQL Testcontainers: https://java.testcontainers.org/modules/databases/postgres/
- Publishing Docker images with GitHub Actions: https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images
