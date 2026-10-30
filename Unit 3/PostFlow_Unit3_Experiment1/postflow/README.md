# PostFlow — Unit 3, Experiment 1

A working Spring Boot + Apache Kafka project combining Experiments **3.1.1 and 3.1.2**. Schedule sample posts, observe exponential retries, inspect the actual Kafka dead-letter queue, replay failed events, and prove idempotency through the dashboard.

## Start on Windows

You need **Java/JDK 17 or newer** and **Docker Desktop with Docker Compose v2**. Maven and Node.js are not required to run the supplied JAR.

1. Extract the entire ZIP into a folder, such as `Documents\postflow`.
2. Open Docker Desktop. Wait until the Docker engine is running. Use Linux containers.
3. In the extracted `postflow` folder, double-click **start.bat**.
4. On the first run, Docker downloads Apache Kafka. The script waits for Kafka to become healthy.
5. Wait for `Started PostFlowApplication` in the terminal. Leave that terminal open.
6. Open **http://localhost:8081** in your browser.
7. Under **Try a failure scenario**, click the arrow beside **Smooth delivery**.

Java and Docker must already be installed. The first Docker image download needs internet. Later starts can use the cached image. No external social account or API key is needed.

On macOS/Linux use `sh start.sh`. Manual commands from the project folder:

```sh
docker compose up -d --wait --wait-timeout 180
java -jar postflow.jar
```

**Stop:** press Ctrl+C in the Java terminal, then run `docker compose stop` to stop Kafka. Restart using start.bat/start.sh. Do not delete the Docker volume or `.data` unless you intentionally want to discard lab data.

## Dashboard features

| Page | What it does |
|---|---|
| Overview | Real status counts, recent events, activity, and scenario launchers |
| Event queue | Search/filter events, view per-event traces, resend published events |
| Dead-letter queue | Original Kafka coordinates, error detail, raw payload and controlled replay |
| Published feed | Local database publications; one per business event ID |
| Event log | Producer ACKs, retries, failed attempts, successful publication and duplicate skips |
| Lab guide | Step-by-step classroom walkthrough and architecture explanation |

All displayed counts come from the backend. The dashboard polls every two seconds. Kafka connectivity is checked separately every five seconds; it may take a few seconds to reflect a change. Lists show the latest 200 events / 100 logs / 100 DLQ records / 100 publications; counters are database-wide.

## Demonstrate both experiments

1. **Normal event:** click Smooth delivery. Open Event queue → Trace. It should publish on attempt 1.
2. **Temporary failure:** launch Temporary failure. It fails attempts 1 and 2, waits approximately 1s and 2s, then succeeds on attempt 3.
3. **Permanent failure:** launch Persistent failure. It makes 4 attempts with waits of 1s, 2s, 4s, then appears in the DLQ. Kafka polling and scheduling can add delay.
4. **Replay:** in Dead-letter queue, inspect the failed payload. Click Fix & replay and confirm. This changes the simulated failure mode to NONE, increments the replay generation and retains the same business event ID.
5. **Duplicate:** from Published feed, click Test duplicate. The overview's skipped-duplicates counter increases; the feed must not gain another copy of that post.
6. **Scheduling:** create a post with a future time. It stays SCHEDULED until due, then enters Kafka.
7. **Broker failure:** run `docker compose stop kafka`. Schedule a post. It stays in the database outbox until delivery can be acknowledged. Run `docker compose start kafka`, then wait for reconnection and the next send attempt (up to 30s backoff, plus reconnect time).

Publishing is an intentional **local simulation**: LinkedIn, Instagram and X are labels for sample posts. The project never sends content to those external services.

## What is implemented

- A persistent H2 database and transactional outbox.
- Actual Kafka producer (`KafkaTemplate`) and consumers (`@KafkaListener`).
- Two topics: `postflow.posts.v1` and `postflow.posts.dlq.v1`, each with 3 partitions.
- Exponential consumer retries: initial attempt + 3 retries, at 1s / 2s / 4s by default.
- `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`; the source offset is not recovered successfully if the DLQ send fails.
- DLQ consumer persists a searchable inspection view using original topic, partition, offset and exception headers.
- Database row locking and a unique processed-event key. The marker and local publication commit in the same transaction.
- Kafka offsets commit after the listener succeeds. A crash between database commit and offset commit can cause redelivery; the marker prevents a duplicate effect.
- Replays preserve the event ID and use a new generation. Stale generations cannot publish again.
- An ACK-first outbox dispatcher. A crash after Kafka ACK but before the sent flag can duplicate delivery, which the consumer handles safely.
- Separate producer outage retries, capped at 30s, that retain the pending event instead of discarding it.
- Malformed JSON is quarantined directly to the DLQ instead of wasting transient-error retries.

## Open and edit the code

Open the whole folder in VS Code or IntelliJ. Java source is under `src/main/java/com/postflow`. The dashboard is under `src/main/resources/static`.

To rebuild or run tests, install **Maven 3.6.3+**, then run:

```sh
mvn test
mvn clean package
java -jar target/postflow-1.0.0.jar
```

Tests start a real embedded Kafka broker (KRaft), and do not need Docker. The first Maven build requires internet to download dependencies. Docker is still the intended broker runtime for your classroom demonstration.

After editing, copy `target/postflow-1.0.0.jar` over the root `postflow.jar` if you want the start scripts to run the new build. Alternatively run `mvn spring-boot:run` while Kafka is running.

## Main files

| File | Responsibility |
|---|---|
| PostFlowApplication.java | Boot entry point and scheduling |
| PostEvent.java | Kafka event envelope |
| EventService.java | Transactional creation, idempotent processing, replay and audit |
| OutboxDispatcher.java | Due-event producer and broker-outage handling |
| KafkaConfig.java | Topics, error handler, exponential backoff and DLQ recovery |
| EventConsumer.java | Main and DLQ listeners |
| BrokerStatus.java | Live broker connectivity |
| ApiController.java | Dashboard REST API |
| LocalApiFilter.java | Local mutation header and browser security headers |
| schema.sql | Database tables and indexes |
| compose.yaml | Apache Kafka 4.1.2 in Docker, single-node KRaft |
| ReliabilityIntegrationTest.java | Broker-backed and HTTP integration tests |
| docs/EXPERIMENT.md | Submission-ready experiment notes and viva |
| docs/API.md | API reference and curl examples |
| docs/TEST-RESULTS.md | Actual verification results and limitations |

## Configuration and persistence

Defaults:

| Environment variable | Default | Purpose |
|---|---|---|
| PORT | 8081 | Spring Boot HTTP port |
| SERVER_ADDRESS | 127.0.0.1 | Local-only server binding |
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka bootstrap address |
| DATABASE_URL | jdbc:h2:file:./.data/postflow;DB_CLOSE_ON_EXIT=FALSE | Persistent app data |
| RETRY_INITIAL_MS | 1000 | Initial consumer retry wait; next waits multiply by 2 |

The dashboard's guide describes the default 1s/2s/4s settings; if you override RETRY_INITIAL_MS, the actual event log reports the configured wait.

The application creates `.data/postflow.mv.db` under the working directory. Docker stores broker logs and KRaft metadata in the named `kafka-data` volume, mounted at `/tmp`. Neither database contents nor broker data is included in this ZIP. Always launch Java from the same project folder to keep the same database.

The app uses an H2 database and one dispatcher process. Kafka has one broker and replication factor 1. This is a learning project, not a highly available deployment. No login is included because this experiment focuses on messaging reliability; the app and broker bind to loopback. Do not expose these unauthenticated lab services to a network.

## Reliability boundaries

- The project is configured for **at-least-once consumption with idempotent local effects**, not global exactly-once delivery.
- Producer `enable.idempotence` protects producer retries within its guarantees; it does not replace application-level event tracking.
- The processed marker and publication are atomic only because both are in the same database. External APIs would need their own idempotency key, deduplication contract or an additional outgoing outbox.
- Consumer backoff is blocking for the affected consumer thread. With 3 partitions / 3 workers, other partitions may continue. Other records in the affected partition wait until recovery. Large production retry schedules should use retry topics/nonblocking patterns or container-pausing backoff.
- Retry counters are persisted for the demo. The error handler's backoff state is in memory, so a process restart can reset the retry budget and yield more than four total historical attempts before recovery. Replays also intentionally start a new generation.
- DLQ recovery and source-offset commits are not one Kafka transaction. A crash may create duplicate DLQ deliveries; the inspection table deduplicates by original topic/partition/offset.
- The DLQ inspection consumer retries failed database writes indefinitely. It never recursively dead-letters into its own topic.
- Failure modes deliberately simulate downstream errors. This is not a real external posting integration.
- A single application instance is supported. Multiple concurrent outbox dispatchers would require row claiming/leases; consumer deduplication would still protect the local publication.
- Outbox and audit history are retained; a production deployment should add retention, monitoring, backups, authentication, TLS and broker replication.

## Troubleshooting

- **Docker is not running:** open Docker Desktop and wait for the engine; rerun start.bat.
- **`java` not recognized:** install JDK 17+ and reopen your terminal.
- **Port 8081 in use:** stop the other app or set PORT=8082 and open localhost:8082.
- **Port 9092 in use:** stop your other Kafka/container service before using this Compose file.
- **Kafka not healthy:** run `docker compose logs kafka`. Ensure Docker has sufficient memory (4 GB allocated is a reasonable lab starting point).
- **Dashboard says disconnected:** verify the Java terminal is open and Docker is running; refresh the page after startup.
- **Post waits in the outbox:** check its scheduled time, broker status and Trace's producer retry messages. Reconnection may take up to the current backoff plus Kafka client reconnect time.
- **Opening index.html shows no data:** use http://localhost:8081; the UI requires its backend.
- **Reset everything:** stop Java first. `docker compose down -v` permanently deletes Kafka data; removing `.data` permanently deletes application data. Only do both when you deliberately want a clean lab.
