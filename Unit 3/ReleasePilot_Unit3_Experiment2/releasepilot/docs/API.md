# API reference

Base URL: `http://localhost:8082`.

POST requests require `X-Lab-Request: releasepilot` and JSON bodies. The local control plane has no user login; keep it on loopback.

| Method | Path | Purpose |
|---|---|---|
| GET | /api/info | Name, version label, track and revision |
| GET | /api/health | Application/database health |
| GET | /api/quote?quantity=2 | Direct quote response covered by Pact |
| POST | /api/orders | Persist an order; body `{"quantity":2}` |
| GET | /api/evidence | Recorded Maven suite results packaged with the build |
| GET | /api/rollout | Allocation, upstream probes, current-stage counts and event history |
| POST | /api/rollout | Change allocation; body `{"percent":10}` |
| GET | /traffic/api/quote?quantity=2 | Forward to stable/candidate according to visitor cohort |

Quote response:

```json
{"sku":"LAB-KIT","quantity":2,"unitPrice":499,"total":998,"currency":"INR"}
```

Amounts are integer rupees for this sample. Quantity must be 1–20. Order creation returns HTTP 201 with id, quantity, total and currency.

Allowed canary percentages: 0, 10, 50, 100. The API returns 409 if releases are unhealthy, the rollout has not started at 10%, or the current stage lacks five successful canary samples / contains errors. Invalid percentages return 400. A missing mutation header returns 403.

For a routed request, optionally supply `X-Canary-Key: visitor-123`. Response header `X-Release-Track` reports the backend actually selected. The header is a laboratory cohort selector, not an authentication mechanism.

Gateway metrics are reset logically by each stage's timestamp; historical records remain in the database. Probe health updates every five seconds. Database result rows in traffic/event arrays may have uppercase keys with H2; the supplied dashboard normalizes these keys.
