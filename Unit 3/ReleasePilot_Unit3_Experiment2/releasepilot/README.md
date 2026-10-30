# ReleasePilot — Unit 3, Experiment 2

Combines **3.2.1 (Continuous Integration)** and **3.2.2 (Continuous Deployment)** in a working Spring Boot dashboard and deployment lab.

## What you receive

- A dashboard for recorded test evidence, release health, traffic distribution, promotion and rollback.
- A sample quote/order API that is actually tested and deployed.
- JUnit 5 unit tests and MockMvc API/database integration tests.
- A Pact consumer contract and verification against the real running provider.
- A PostgreSQL Testcontainers test, enabled in the full CI profile.
- A Dockerfile and Compose stack with PostgreSQL, stable backend, candidate backend and gateway.
- GitHub Actions for verification, container smoke tests, publishing to GHCR and optional automatic deployment.
- A prebuilt runnable JAR, documentation, test results and classroom demo instructions.

## Recommended start — full Docker dashboard (Windows)

1. Extract the entire ZIP into a normal folder.
2. Open **Docker Desktop** and wait for its Linux-container engine to be running.
3. Double-click **start-docker.bat** in the extracted `releasepilot` folder.
4. The first run downloads Java/PostgreSQL container images, builds the application image and starts four containers. Keep the setup terminal open until it finishes.
5. Open **http://localhost:8082**.
6. Open **Canary rollout**. Wait until both releases are Healthy.
7. Click **Start 10%**, then **Send test traffic**.
8. Once at least five successful canary requests have been recorded, click **Promote 100%**.

Docker provides Java inside the containers. You do not need Maven or a host Java installation just to run this full prebuilt demo. Install **JDK 17+** and **Maven 3.6.3+** when editing/building the Java source.

On macOS/Linux use `sh start-docker.sh`. Equivalent manual commands:

```sh
docker build -t releasepilot:local .
docker compose up -d --wait --wait-timeout 180
```

Do not run Java-only mode on the same port at the same time. Stop the Docker stack with `docker compose stop`; restart with `docker compose start`. Data stays in Docker volumes.

## Java-only option

Install Java 17+, then double-click **start-java.bat**, or run:

```sh
java -jar releasepilot.jar
```

This serves the dashboard, quote/order API and packaged test evidence at localhost:8082. It does **not** start the separate stable/candidate services or PostgreSQL. The dashboard clearly shows this limitation. Use Docker for the complete canary exercise.

## What the canary controls really do

The gateway forwards **`/traffic/api/quote`** to one of two separate Spring Boot services. A hash of `X-Canary-Key` selects a stable visitor cohort. Without that header, the key is derived from the caller's IP and user agent. The traffic button sends 200 real HTTP requests with distinct synthetic visitor keys.

Allocation is 0%, 10%, 50% or 100% candidate. Actual sample percentages will vary. The displayed counters come from the gateway database, not animations or invented values.

To start a candidate rollout, both backend quote probes must return HTTP 200. To promote above 10%, the current stage must have at least **five successful canary requests and zero canary errors**. Allocation changes begin a new measurement stage. If you choose 50%, send another sample before promoting to 100%.

Every five seconds, the gateway checks active-candidate health and recorded candidate errors. A failed probe or any candidate error causes allocation to return to 0%. Stable receives new traffic; existing in-flight requests are not recalled.

The canary applies to the sample traffic endpoint, not every dashboard asset or control endpoint. Local Docker slots initially use the same app image with different release labels; the CD job supplies different immutable image digests on later releases.

## Demonstrate a failed candidate

After starting a canary and promoting it, open **PowerShell in the project folder**:

```powershell
$env:CANARY_FAIL_QUOTES="true"
docker compose up -d --wait canary
```

The candidate quote endpoint returns 503. The gateway should automatically roll back to stable within the health-check interval, plus request timeout. Open Release activity to see the event. Candidate restarts can also cause a temporary unavailable probe, which triggers the same protection.

Restore the candidate afterward:

```powershell
$env:CANARY_FAIL_QUOTES="false"
docker compose up -d --wait canary
```

On bash:

```sh
CANARY_FAIL_QUOTES=true docker compose up -d --wait canary
CANARY_FAIL_QUOTES=false docker compose up -d --wait canary
```

Do not run regression injection on a real deployment. These commands are for the isolated local lab.

## Run and understand the test layers

From the project folder:

```sh
# JUnit, MockMvc, Pact consumer generation and real provider verification:
mvn clean verify

# Also start a real PostgreSQL container (Docker must be running):
mvn clean verify -Pdocker-tests
```

| Layer | Test file | What it proves |
|---|---|---|
| Unit | QuoteServiceTest | Price totals, invalid quantities, sticky routing and cohort distribution |
| Integration | ApiIntegrationTest | Real Spring routes, MockMvc validation, database writes and local mutation protection |
| Consumer contract | DashboardConsumerPactTest | The dashboard's expected quote response, expressed by a Java consumer test |
| Provider contract | QuoteProviderIT | The actual Spring HTTP provider satisfies the generated Pact |
| Real infrastructure | PostgresContainerCheck | The order API writes successfully to PostgreSQL started by Testcontainers |
| Container delivery | scripts/canary_smoke.py | Actual routing, promotion and rollback in the Compose stack |

`mvn test` runs the first three layers. **`mvn verify` is required for provider verification**. Contract files are generated at `target/pacts` and then read by the Failsafe provider test. There is no Pact Broker account dependency.

The Java consumer test codifies the frontend's HTTP expectations; it does not execute browser JavaScript. The PostgreSQL test is not silently skipped when Docker is unavailable: the `docker-tests` profile must fail rather than claim success.

## Refresh the packaged dashboard's evidence

The dashboard displays build evidence, not a live GitHub connection. After a successful verification:

```sh
python scripts/evidence.py
mvn -DskipTests package
```

Copy `target/releasepilot-1.0.0.jar` to the project root as `releasepilot.jar`. Rebuild the Docker image if using Docker. The evidence script marks absent test layers **NOT_RUN**; it never invents pass results.

In CI, evidence is generated after `mvn clean verify -Pdocker-tests` passes, then included in the image. The separate container smoke job is a downstream release gate and is not retroactively added to that packaged Maven evidence.

## GitHub Actions pipeline

See **docs/GITHUB-SETUP.md** for exact setup.

1. Pull request/main push → unit, integration, Pact and PostgreSQL Testcontainers tests.
2. Successful verification → package the tested application and upload evidence artifacts.
3. Start the Docker stack on a GitHub-hosted runner → verify routing, promotion and injected-regression rollback.
4. On main only, after both gates pass → push the application image to **GitHub Container Registry (GHCR)**.
5. If `ENABLE_CD=true` and a dedicated deployment runner exists → deploy the verified digest, start a 10% canary, send samples, promote to 100%, or restore stable on failure.

No GitHub repository, registry or server was connected or changed while creating this project. Workflows are ready to configure, not evidence of a hosted run.

## Important files

| File | Purpose |
|---|---|
| src/main/java/com/releasepilot | API, quote calculation, rollout logic and traffic forwarding |
| src/main/resources/static | Responsive dashboard |
| src/main/resources/schema.sql | Orders, rollout state, traffic and release events |
| src/test/java/com/releasepilot | JUnit, MockMvc, Pact and Testcontainers tests |
| .github/workflows/pipeline.yml | Complete CI, image publishing and opt-in CD jobs |
| Dockerfile | Non-root Java runtime image with healthcheck |
| compose.yaml | Gateway + stable + canary + PostgreSQL |
| scripts/evidence.py | Generate factual dashboard evidence from JUnit XML |
| scripts/canary_smoke.py | End-to-end release smoke test |
| scripts/deploy.py | Dedicated-host canary deployment and persisted image state |
| docs/EXPERIMENT.md | Aim, theory, implementation, observations and viva |
| docs/TEST-RESULTS.md | Checks actually run and verification limits |

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| PORT | 8082 (8080 inside Docker) | Application HTTP port |
| SERVER_ADDRESS | 127.0.0.1 (0.0.0.0 inside Docker) | Bind address |
| APP_TRACK | standalone | Runtime label |
| APP_VERSION | 1.0.0-local | Runtime version label |
| APP_COMMIT | local | Image/source revision metadata |
| APP_FAIL_QUOTES | false | Intentional local failure injection |
| STABLE_URL / CANARY_URL | localhost:8083 / localhost:8084 | Gateway's configured upstreams |
| DATABASE_URL | file-based H2 under .data | JDBC connection |
| DATABASE_USER / DATABASE_PASSWORD | sa / empty for Java-only H2 | Database credentials |

Compose overrides these for the container network. Stable and candidate share PostgreSQL; the gateway keeps rollout/traffic state in its own H2 volume. Compose's default database password is for this isolated local lab only. The deployment script generates a separate persistent random password.

## Scope and limitations

- This is a single-host classroom canary system, not a production orchestrator or highly available deployment.
- The controls are unauthenticated and protected only by loopback publishing and a required mutation header. Do not expose the gateway publicly. A real control plane needs identity, authorization, TLS and audited access.
- Health decisions use HTTP status and a strict zero-error policy, not production SLOs, latency thresholds or statistical significance. Five candidate successes are a teaching threshold.
- Changes to the database schema must remain compatible with both releases. This project uses a static additive schema; production deployments need versioned migrations and expand/contract discipline.
- Full image promotion leaves traffic on the candidate slot. A subsequent deployment first places the last promoted image in both slots, then replaces only the candidate. First deployment bootstraps both slots from the first verified image because no previous release exists.
- A gateway upgrade itself is not canaried, and single-host deployment may briefly interrupt availability. Local automatic rollback returns traffic to the previous stable slot; it cannot reverse database mutations.
- GitHub-hosted tests/CD, Docker image startup, and visual browser checks must be distinguished from the locally executed tests. Read TEST-RESULTS.md.

## Troubleshooting

- **Port 8082 occupied:** stop the Java-only app before starting Docker, or edit the Compose host port and use that URL.
- **Upstreams unavailable in Java-only mode:** expected; use start-docker.bat for the full stack.
- **Promotion blocked:** start 10% first, send traffic, and check for at least five candidate successes with no errors. A new allocation resets the measurement stage.
- **Pact provider finds no files:** run `mvn clean verify`; consumer generation must precede provider verification.
- **Testcontainers fails:** ensure Docker Desktop is running and you can run `docker info`.
- **Docker startup fails:** inspect `docker compose logs`. All containers must become healthy before the script finishes.
- **Reset local data:** `docker compose down -v` permanently removes the lab volumes. Stop Java and delete its `.data` folder only if you intentionally want to remove Java-only data.
