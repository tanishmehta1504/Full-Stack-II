# Verification results

Verified on 18 October 2026 with Java 17 on Linux.

**10 tests passed. Zero failures, zero errors, zero skipped.**

The suite starts a real embedded Kafka KRaft broker, real Spring Boot HTTP server and H2 database. Kafka delivery, the error handler, DLQ publishing and idempotency decisions are not mocked. Test retry delays use 100/200/400 ms for speed; the shipped defaults are 1/2/4 seconds.

| Test | Result |
|---|---|
| exhaustedEventGoesToActualDlqAndCanBeReplayedOnce | PASS |
| normalPostPublishesOnceThroughRealKafka | PASS |
| concurrentProcessingStillHasOnePublication | PASS |
| malformedJsonIsQuarantinedWithoutBlockingHealthyMessages | PASS |
| tenDuplicateDeliveriesDoNotRepeatTheBusinessEffect | PASS |
| transientFailureUsesExponentialBackoffAndEventuallySucceeds | PASS |
| unacknowledgedSendRemainsInDurableOutbox | PASS |
| dashboardAndLiveSnapshotAreServed | PASS |
| invalidInputAndCrossOriginStyleMutationsAreRejected | PASS |
| scheduledPostStaysInOutboxUntilDue | PASS |

## Additional validation

- JavaScript syntax checked with `node --check`.
- Postman collection parsed as valid JSON (9 interactive requests).
- Docker Compose configuration parsed as YAML and checked for the expected Kafka service, local port mapping and persistent volume.
- Maven packages the Spring Boot executable JAR.

## Verification limits

- Docker is not installed in this environment. The supplied Kafka 4.1.2 Compose deployment, its healthcheck, volume ownership, and Windows launch script have **not** been executed here. Run start.bat with Docker Desktop on your computer to verify that setup.
- The embedded test broker version is managed by the Spring Kafka test dependencies; it is not the Docker image.
- No browser automation or visual layout verification was available. HTML/CSS/JS are served successfully over HTTP, but browser interactions require a local check.
- The outbox outage test uses an unreachable broker endpoint and verifies retention after a failed send. It is not a Docker stop/start test.
- The tests use isolated databases; a full application restart with the file-backed database was not exercised in this run.
- Postman itself was not run; equivalent API paths were exercised by the integration tests.
- This is a local publishing simulation, not an integration with external social networks or a production load/security audit.
