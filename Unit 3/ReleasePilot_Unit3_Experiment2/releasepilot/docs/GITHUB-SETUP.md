# GitHub Actions and deployment setup

These instructions are for your account. The supplied workflow has not been run in a hosted GitHub repository during project creation.

## 1. Push the source

1. Create a GitHub repository (private is appropriate for a class assignment).
2. Open the extracted `releasepilot` folder in VS Code.
3. Initialize Git if needed and publish the project to the repository's **main** branch. Ensure `.github/workflows/pipeline.yml` is included.
4. Do not commit `.data`, `.env`, deployment.json, target output or the large prebuilt root JAR. The included .gitignore excludes these. CI builds its own JAR.
5. Open the repository's **Actions** tab → **Build, verify, package and deploy**.

The verify job runs on an Ubuntu GitHub-hosted runner, where Docker is available. It executes `mvn clean verify -Pdocker-tests`. A failed test stops the downstream jobs.

## 2. Inspect the evidence

The workflow uploads:

- `verified-app`: runnable JAR with recorded build evidence.
- `test-reports-and-pacts`: Surefire/Failsafe reports, generated consumer contract and evidence JSON.
- `container-smoke-logs`: Compose logs from the routing/promotion/rollback check.

Make an intentional change to the total calculation in QuoteService and push a test branch/PR to see the unit/contract gates fail. Restore it before merging. Do not change expected contract values merely to hide an accidental incompatibility.

## 3. Registry publishing

After verification and Docker smoke tests pass, main-branch runs publish to:

`ghcr.io/<your-owner>/<your-repository>:sha-<commit>`

The workflow uses GitHub's GITHUB_TOKEN with `packages: write` only in the image job. No personal token is hard-coded. Ensure your repository/organization policy permits package publishing and Actions to run. A private GHCR package must allow access from its originating repository.

Deployment uses the resulting **image digest**, not a mutable latest tag. Pull requests do not receive image-publishing or deployment jobs.

## 4. Enable deployment only when a host is ready

Prepare a **dedicated Linux machine** with Docker Engine/Compose, Python 3 and a GitHub Actions self-hosted runner. Do not use a public/untrusted PR runner as a deployment machine. The runner account needs Docker access and outbound access to GitHub/GHCR.

In your repository:

1. Settings → Actions → Runners → New self-hosted runner. Follow GitHub's generated commands on the dedicated machine.
2. Add the custom label **releasepilot-deploy** (keep the standard `self-hosted` and `linux` labels).
3. Settings → Environments → create **lab**. Add approval requirements if desired; with approvals enabled this is an approved delivery flow rather than unattended continuous deployment.
4. Settings → Secrets and variables → Actions → Variables → create **ENABLE_CD**, value **true**.
5. Push a trusted change to main or dispatch the workflow from main.

Before this variable is enabled, test/image jobs can run and deployment is intentionally skipped. No external-server credentials are needed in the project: the deployment job runs on the target host itself and uses its Docker installation.

## 5. What deploy.py does

Deployment state is stored outside the runner checkout at `~/releasepilot-deployment/deployment.json`, with owner-only file permissions. It contains the last promoted image digest and generated database password. Do not commit or share it.

The script:

1. Restores the last promoted image into both release slots and starts/updates the gateway.
2. Routes new traffic to stable (0% canary).
3. Pulls the new verified digest into the canary slot.
4. Starts 10% canary, sends 200 synthetic visitors, checks responses and promotes to 100%.
5. Observes for ten seconds and verifies that automatic rollback did not occur.
6. Records the new promoted digest. On failure it requests 0% canary and exits nonzero.

The first deployment has no previous version; both slots bootstrap from the first verified image. Later runs compare a new candidate to the last promoted version. This short observation window is for the experiment, not a production rollout policy.

## 6. View the remote dashboard safely

The Compose gateway is published only on the deployment host's loopback port 8082. From your computer, establish an SSH tunnel to the host (replace the example):

```sh
ssh -L 8082:127.0.0.1:8082 your-user@your-host
```

Then open http://localhost:8082. Use another local port if it is already occupied. Do not publish this unauthenticated control panel publicly.

## 7. Rollback

Use the dashboard's Roll back button, or execute on the deployment host:

```sh
curl -X POST http://localhost:8082/api/rollout \
  -H 'Content-Type: application/json' \
  -H 'X-Lab-Request: releasepilot' \
  -d '{"percent":0}'
```

The stable slot remains the previous release until the next deployment. Rollback affects future routed quote requests; it does not undo database writes or cancel in-flight requests.
