# Unit 3 — Experiment 3: Integrated Social Media Management System

## Aim

To integrate frontend and backend components into a complete full-stack social-media management application with secure multi-user workflows, scheduling, asynchronous publishing, analytics and automated delivery.

## Objectives and implementation mapping

| Experiment objective | Implemented component |
|---|---|
| Connect React with backend APIs | Same-origin REST calls through Redux Toolkit state/API layer |
| Authentication flow using JWT | Login, BCrypt, signed expiring access token, method security |
| Post composer | Create/edit drafts, channel selection, preview, validation |
| Calendar and scheduling | Monthly calendar, local-time input, epoch-based scheduling |
| Fetch/display analytics | Database-backed totals, published feed, seven-day chart |
| Multi-user collaboration | Admin/Editor/Viewer, shared comments, ownership restrictions |
| Kafka auto-publishing | Scheduler, transactional outbox, Kafka consumer |
| Reliability | Exponential retries, DLQ, generation validation, unique publication |
| API documentation | Generated OpenAPI and Swagger UI |
| ≥80% coverage | Enforced whole-backend JaCoCo line-coverage gate |
| Deployment through CI/CD | GitHub Actions tests, image publication and optional VM deployment |

CO mapping: 3.3.1 — CO5/BT5 and CO6/BT6. 3.3.2 — CO6/BT6.

## Theory

A full-stack application joins a user interface, business logic, storage and background processing into one coherent workflow. React renders the interface. Redux Toolkit manages server snapshots and API errors. Spring Boot exposes REST resources; Spring Security authenticates bearer tokens and checks roles. PostgreSQL persists collaborative data. Kafka separates scheduling from publishing so temporary delivery failures do not erase user intent.

An outbox makes the database mutation and intent to publish atomic. Kafka delivery may repeat, so the consumer locks the post and checks its status and generation before writing a publication. A unique publication key prevents repeated local business effects. Retrying exhausted failures through a DLQ preserves visibility and avoids endlessly blocking the main queue.

## Procedure

1. Start the supplied Docker stack and sign in as Admin.
2. Create an Editor and Viewer; verify their access differences.
3. Create a draft, edit its content and add collaborative feedback.
4. Schedule a post and observe its calendar entry.
5. Allow the scheduler and Kafka consumer to publish it to the demo feed.
6. Confirm status counts and analytics change from real stored data.
7. Cancel a future post and verify it returns to Draft.
8. Inject a failure as Admin, observe Failed after retries, then retry successfully.
9. Inspect Swagger and demonstrate authorized/unauthorized API requests.
10. Run frontend tests and Maven verify; inspect JaCoCo's report.
11. Push source to GitHub and configure the deployment environment as documented.

## Expected observations

A Viewer receives 403 when attempting to create content. An Editor cannot edit an Admin-owned draft. Valid JWTs allow shared reading; expired or invalid tokens return 401. A scheduled post publishes at or after its due time. Duplicate events leave one publication. Cancellation invalidates older generations. Failed publishing becomes visible and can be retried. Exact test/coverage results are recorded in TEST-RESULTS.md.

## Result

The project integrates frontend, authenticated APIs, data persistence, collaboration, scheduling, Kafka publishing and operational analytics. Automated workflows and deployment configuration are provided. Actual external social publishing and public production operation require further provider and infrastructure configuration.

## Viva questions

1. Why use JWT? It lets each API request carry a signed, expiring authentication credential without a server-side HTTP session.
2. Why read the role from the database? It allows account disablement and current authorization decisions to take effect without waiting for token expiry.
3. Why an outbox? It prevents losing the publishing intent between saving a post and sending to Kafka.
4. What is idempotency? Repeated delivery leaves the same final business result.
5. Why use a DLQ? To preserve failed work for inspection/retry after bounded attempts.
6. What is the difference between UI permissions and backend authorization? UI controls guide users; server checks prevent unauthorized requests regardless of the client.
7. What does 80% coverage mean here? At least 80% of Java application's executable source lines were exercised by tests; coverage alone does not prove correctness.
8. Why is demo publication not a real social integration? Provider authentication, API calls, rate limits and analytics ingestion must be implemented for each external platform.

## References

- https://docs.spring.io/spring-kafka/reference/kafka/annotation-error-handling.html
- https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- https://redux-toolkit.js.org/usage/usage-guide/
- https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html
- https://springdoc.org/
