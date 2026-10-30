# API guide

Base URL: `http://localhost:8081`.

All mutation requests require **`X-Lab-Request: postflow`**. JSON bodies use **`Content-Type: application/json`**. There is no login: this is a loopback-only classroom application, not an internet-facing service. No CORS is enabled.

| Method | Endpoint | Purpose | Success |
|---|---|---|---|
| GET | /api/snapshot | Broker status, counters, latest events, logs, publications and DLQ records | 200 |
| POST | /api/events | Persist a post and transactional outbox entry | 202 |
| GET | /api/events/{id} | Event details, complete activity and outbox delivery state | 200 |
| POST | /api/events/{id}/duplicate | Queue the same published event ID again | 202 |
| POST | /api/dead-letters/{id}/replay | Fix the simulated failure and enqueue the next generation | 202 |

Create body:

```json
{
  "title": "A reliable post",
  "content": "Learning retries, DLQ, and idempotency with Kafka.",
  "channel": "LinkedIn",
  "failureMode": "TRANSIENT",
  "scheduledAt": null
}
```

- `title`: required, up to 120 characters.
- `content`: required, up to 2,000 characters.
- `channel`: LinkedIn, Instagram or X (local labels only).
- `failureMode`: NONE, TRANSIENT (first two attempts fail), or ALWAYS.
- `scheduledAt`: optional Unix timestamp in milliseconds; null/past time means now. Future schedules are limited to one year.

The response returns `id` and `message`. HTTP 202 means accepted into the database outbox, not already delivered to Kafka or published.

Snapshot/database row keys use lowercase snake_case, for example `event_id`, `created_at` and `failure_mode`. New-post input uses the camelCase fields above.

## Postman

Import `PostFlow.postman_collection.json`. Start the application first, then run Create temporary-failure event. Its test script saves the returned ID. Wait a few seconds, then call Inspect event; it should reach PUBLISHED after retries. The permanent-failure request stores another ID; Inspect DLQ snapshot captures its DLQ record ID once the record has arrived. Then run Replay dead letter.

Kafka processing is asynchronous. Do not run every request instantly and expect the later assertions to pass; wait for the indicated state. Duplicate and replay requests deliberately return 409 if the event is not ready for that action.

## Example curl (bash)

```sh
curl -X POST http://localhost:8081/api/events \
  -H 'Content-Type: application/json' \
  -H 'X-Lab-Request: postflow' \
  -d '{"title":"Retry demo","content":"A sample post","channel":"LinkedIn","failureMode":"TRANSIENT"}'
```

Read a trace using the returned ID:

```sh
curl http://localhost:8081/api/events/REPLACE_WITH_EVENT_ID
```

After publication, resend the same event:

```sh
curl -X POST http://localhost:8081/api/events/REPLACE_WITH_EVENT_ID/duplicate \
  -H 'X-Lab-Request: postflow'
```

## Kafka inspection

From the project folder:

```sh
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic postflow.posts.dlq.v1 --from-beginning --property print.headers=true
```

The console consumer has its own offsets; reading the DLQ this way does not consume it away from the application. Press Ctrl+C to stop.

## Errors

- 400: invalid input or unsupported schedule.
- 403: missing required mutation header.
- 404: unknown event/DLQ ID.
- 409: duplicate requested before publication, malformed replay, repeated replay, or stale DLQ generation.

Unexpected database/server failures return 5xx. Pending outbox entries are not marked sent on a failed producer send. The dashboard distinguishes application connectivity from broker connectivity.
