# Unit 3 — Experiment 1
## PostFlow: Reliable Event-Driven Scheduled Post Processing

**Combined experiments:** 3.1.1 and 3.1.2  
**COs:** CO2–BT2, CO3–BT3, CO5–BT5, CO6–BT6

## Aim

To build an event-driven scheduled-post application using Apache Kafka and implement reliability through exponential retries, dead-letter queues and idempotent processing.

## Objectives

1. Understand the Kafka producer–consumer model and asynchronous processing.
2. Handle temporary failures with exponential backoff.
3. Route exhausted events to a DLQ without silently discarding them.
4. Prevent repeated business effects when Kafka delivers an event more than once.
5. Test normal operation, failure, recovery, scheduling and duplicate-delivery scenarios through a working dashboard.

## Software

Java/JDK 17+, Spring Boot 3.5.16, Spring Kafka, Apache Kafka 4.1.2 in Docker, H2, Maven, JUnit 5, an IDE and a browser. The frontend uses HTML, CSS and JavaScript served by Spring Boot.

## Theory in simple language

### Event-driven communication

An event describes something that should happen. In this application, it describes a post that is ready to be published. The producer sends the event to Kafka. A consumer receives it later and performs the work. The user does not have to wait for that processing to finish before the create-post request returns.

Kafka separates accepting a request from processing it. The event key is the stable event ID. A Kafka record also has a topic, partition and offset, which identify its position in the broker log.

### Retries and exponential backoff

A temporary network problem may disappear on a later attempt. Retrying immediately and continuously can overwhelm a struggling system. Exponential backoff waits increasingly longer between attempts.

With an initial wait of 1 second, multiplier 2 and three retries:

| Attempt | Wait before attempt | If it fails |
|---|---|---|
| 1 | None | Wait 1 second |
| 2 | 1 second | Wait 2 seconds |
| 3 | 2 seconds | Wait 4 seconds |
| 4 | 4 seconds | Send to DLQ |

The wait formula is `initialDelay × 2^(retryNumber − 1)`. Three retries means **four total attempts**, not three. Actual elapsed time also includes processing and Kafka polling.

### Dead-letter queue

A DLQ is a separate Kafka topic used to retain messages that cannot be processed normally. The system can then move on while an operator investigates the failed event. A DLQ does not automatically fix anything; it preserves the failed payload and useful error context.

PostFlow uses `postflow.posts.dlq.v1`. Its observer stores the event payload, reason, original topic, partition and offset in a database view for the dashboard. The actual failed message remains in Kafka subject to Kafka retention.

The Fix & replay operation is an explicit classroom remediation: it removes the simulated fault and creates another delivery with the same event ID and a new generation. Malformed/unrecognized payloads require manual investigation and cannot be replayed from this button.

### At-least-once delivery and idempotency

This project commits Kafka offsets after listener processing. If the process crashes after saving a publication but before the offset is committed, Kafka can deliver that record again. At-least-once delivery therefore requires duplicate-safe business logic.

An operation is idempotent when repeating it does not repeat its business effect. PostFlow uses an event ID as its idempotency key. A database transaction locks the event row, checks the processed-events table, and either skips the duplicate or inserts both the processed marker and publication. A unique event ID is an additional database constraint.

This gives one local publication per event ID. It is not a claim of global exactly-once execution, and it does not automatically protect a separate external API.

### Transactional outbox

Saving a post and sending it directly to Kafka are two different systems. If the application saves the post but crashes before sending it, the event could otherwise be lost. PostFlow writes the post and an outbox row in one database transaction.

A background dispatcher sends due outbox rows. It marks a row sent only after Kafka acknowledges it. A crash between ACK and marking sent may cause a duplicate send, which the idempotent consumer handles. When Kafka is offline, the outbox keeps the event and schedules another producer send attempt.

## Architecture and flow

1. Browser submits a sample post to Spring Boot.
2. The API transaction inserts the event and outbox row into H2.
3. When due, the dispatcher publishes the event to `postflow.posts.v1` using KafkaTemplate.
4. A KafkaListener calls the transactional event processor.
5. Successful processing inserts the processed marker and local publication atomically.
6. Retryable failures use DefaultErrorHandler and exponential backoff.
7. After retries are exhausted, DeadLetterPublishingRecoverer publishes to the DLQ and verifies the send result.
8. A separate DLQ listener records the failed payload and metadata for inspection.
9. The operator can fix the simulated fault and replay it with the same business identity.
10. The dashboard polls backend state every two seconds.

Kafka topics have three partitions. The main listener has three worker threads. This allows independent partitions to process concurrently; blocking retries can still delay other events assigned to the affected worker.

## Database tables

| Table | Purpose |
|---|---|
| events | Post content, event ID, status, generation, attempts and duplicate count |
| outbox | Pending Kafka envelopes, due time, ACK time and producer retry state |
| processed_events | Durable idempotency keys |
| publications | Local publishing side effect, unique by event ID |
| dead_letters | DLQ inspection records with Kafka source coordinates |
| activity | Per-event processing timeline |

## Requirement mapping

| Requirement | Implementation | Dashboard evidence |
|---|---|---|
| Producer–consumer communication | KafkaTemplate and KafkaListener | Kafka ACK and publication trace |
| Exponential retries | ExponentialBackOffWithMaxRetries(3) | Attempt log and increasing waits |
| DLQ routing | DeadLetterPublishingRecoverer | Failed record with source metadata |
| Idempotency | Row lock, processed key, atomic publication | Duplicate counter rises; feed stays unchanged |
| Failure testing | NONE, TRANSIENT and ALWAYS modes | One-click scenarios |
| Controlled recovery | Fixed fault, same ID, next generation | Replay transitions to PUBLISHED |
| Broker downtime | Persistent outbox and ACK-first marking | Event remains queued until broker returns |

## Procedure

1. Start Docker Desktop and run the supplied startup script.
2. Open http://localhost:8081.
3. Run Smooth delivery. Confirm publication on attempt 1.
4. Run Temporary failure. Confirm two failures followed by one success.
5. Run Persistent failure. Confirm four attempts and a DLQ record.
6. Inspect the failed JSON, error reason and Kafka coordinates.
7. Use Fix & replay. Confirm the same event ID publishes under generation 1.
8. Send a duplicate of a published event. Confirm only one publication exists and a DUPLICATE_SKIPPED activity appears.
9. Schedule a future post and check that no publication occurs before the selected time.
10. Stop and restart the Kafka container to demonstrate outbox retention.
11. Run `mvn test` to execute the repeatable broker-backed tests.

## Expected observations

| Scenario | Expected result |
|---|---|
| NONE | Published on attempt 1 |
| TRANSIENT | Two failures, then published on attempt 3 |
| ALWAYS | Four failures, then DLQ |
| Replay after fixing simulated fault | Generation increments; same event ID publishes once |
| Duplicate published event | No extra publication; duplicate count increments |
| Malformed JSON | Direct quarantine to DLQ |
| Kafka unavailable | Unacknowledged outbox row retained for later send |
| Future schedule | No dispatch before due time |
| Repeated replay request | HTTP 409; no second replay is queued |

See TEST-RESULTS.md for the tests actually executed and environment limitations. Docker outage steps are a classroom demonstration procedure; Docker execution must be verified on your computer.

## Result

PostFlow combines asynchronous Kafka communication with retry handling, DLQ recovery and database-backed idempotency. The dashboard exposes the processing state and gives observable evidence for the experiment objectives. The local single-broker setup demonstrates the mechanisms without claiming production high availability.

## Viva questions

**What is the difference between a producer and a consumer?**  
A producer sends events to Kafka. A consumer reads and processes them independently.

**Why does an event have a stable ID?**  
Its ID identifies the business operation even if the message is delivered again or replayed.

**Why use exponential backoff?**  
It allows temporary problems time to recover and reduces repeated pressure on a failing service.

**How many total attempts are made with three retries?**  
Four: one initial attempt plus three retries.

**Does a DLQ delete a failed event?**  
No. It stores a failed record for inspection and recovery while allowing normal processing to continue.

**Why can Kafka deliver duplicates?**  
With at-least-once processing, a consumer may finish the business operation and crash before committing its offset. The broker then delivers it again.

**Why is a Set in Java not enough for idempotency?**  
An in-memory Set disappears on restart and is not shared safely across processes. A durable database key survives restart and can enforce uniqueness.

**Why must the processed marker and publication be in the same transaction?**  
Marking first could lose the publication on failure; publishing first without atomic marking could allow duplicates. Both must commit or roll back together.

**Does Kafka producer idempotence replace consumer idempotency?**  
No. It addresses certain duplicate producer sends, not repeated application-side effects after consumer crashes or explicit replays.

**What is an outbox?**  
A database table that records events in the same transaction as the application's data, allowing a separate sender to deliver them reliably later.

**What changes during replay?**  
The simulated failure mode is fixed and the generation increases. The event ID is preserved so duplicate protection remains meaningful.

**Would this guarantee exactly-once posting to a social network?**  
No. The local publication and marker share a database transaction. An external service needs its own idempotency contract or another reliable handoff design.

## References

- Spring Kafka exception handling: https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html
- Apache Kafka Docker image documentation: https://kafka.apache.org/41/getting-started/docker/
- Apache Kafka broker configuration: https://kafka.apache.org/41/configuration/broker-configs/
