# CI/CD and deployment

## GitHub setup

Create a GitHub repository and push the **contents of the socialflow folder** as its root, including `.github/workflows/ci-cd.yml`. Do not commit `.env`, `.data`, generated JARs or node_modules. The supplied `.gitignore` handles these exclusions. Keep package-lock.json committed.

On a pull request or push to main, the workflow installs dependencies, builds React, runs frontend tests, starts PostgreSQL as a GitHub service, runs real embedded Kafka integration tests and the JaCoCo 80% Java line gate, then builds a container and performs an HTTP/Kafka smoke test. Only successful main builds publish an immutable image to GHCR. Coverage/report artifacts are available on the Actions run.

## Deployment target

A dedicated Linux VM (for example an AWS EC2 VM) with Docker Compose, Git, curl and a GitHub self-hosted Actions runner is the provided target. A static host such as Vercel cannot run this entire JVM/Kafka/PostgreSQL stack.

1. Register a **dedicated** runner labelled `socialflow-deploy`. Do not use a personal machine or run untrusted pull requests on this deployment runner.
2. In repository settings create environment `socialflow-lab`. Configure protection rules appropriate for your project.
3. On the runner create `$HOME/socialflow-deployment/.env` with restrictive permissions (`chmod 600`). Example keys:

```dotenv
DEMO_MODE=false
ADMIN_PASSWORD=replace-with-a-strong-unique-password
DB_PASSWORD=replace-with-a-strong-unique-database-password
JWT_SECRET=replace-with-base64-of-at-least-32-random-bytes
```

Generate the JWT key with `openssl rand -base64 48`. Do not copy a documentation example as a secret. Store credentials outside the repository. This bootstrap creates admin@socialflow.local; change its initial password through a database-managed account procedure before real public operation (no password-change UI is included).
4. Set repository variable `ENABLE_CD` to `true`. The CD job uses the image digest from the successful image job, starts the stack, checks `/api/health` and saves the successful digest. An unhealthy application is rolled back to the previous image when one exists. Database changes are not automatically rolled back.
5. The supplied stack deliberately binds port 8083 to loopback. Access on the VM using an SSH tunnel: `ssh -L 8083:127.0.0.1:8083 your-user@your-vm`. Visit localhost:8083 on your computer.

For classroom demonstration you may intentionally use DEMO_MODE=true on a private VM. Never expose the default demo credentials publicly. The Docker smoke job uses demo mode on an isolated ephemeral runner.

## Before public production use

Configure a TLS reverse proxy and trusted origin, managed secrets, backups/restore drills, schema migrations, dependency/image scanning, login rate limiting, stronger account recovery, audit retention, monitoring/alerts, real social provider adapters, multi-tenant authorization if required, broker replication/authentication and load tests. The current single-broker Compose deployment prioritizes reproducible coursework, not high availability.

The scheduler has a transactional outbox and idempotent local publishing; multiple workers can produce duplicate events safely for this local database side effect. Real external social APIs require their own idempotency/reconciliation strategy. Never assume database uniqueness prevents an external platform from receiving duplicates.

Credentials and infrastructure must be configured by the repository owner. The workflows in this archive have not been run in your GitHub account.
