#!/usr/bin/env bash
# Runs only on a dedicated self-hosted deployment runner with a protected environment.
set -euo pipefail
: "${APP_IMAGE:?APP_IMAGE must be an immutable image digest}"
[[ "$APP_IMAGE" =~ ^ghcr.io/[a-z0-9_./-]+@sha256:[a-f0-9]{64}$ ]] || { echo "Invalid image reference"; exit 1; }
deploy_dir="${DEPLOY_DIR:-$HOME/socialflow-deployment}"
mkdir -p "$deploy_dir"
chmod 700 "$deploy_dir"
# The operator owns .env; never overwrite credentials or database volumes.
test -f "$deploy_dir/.env" || { echo "Create the deployment .env as described in docs/DEPLOYMENT.md"; exit 1; }
cp compose.yaml "$deploy_dir/compose.yaml"
previous_image=""
if test -f "$deploy_dir/current-image"; then previous_image=$(cat "$deploy_dir/current-image"); fi
cd "$deploy_dir"
docker compose --env-file .env pull app
docker compose --env-file .env up -d --wait --wait-timeout 240
healthy=false
for attempt in $(seq 1 60); do
  if curl --fail --silent http://localhost:8083/api/health >/dev/null; then healthy=true; break; fi
  sleep 2
done
if test "$healthy" = true; then
  printf '%s\n' "$APP_IMAGE" > current-image
  echo "Deployment healthy at http://localhost:8083"
else
  echo "Deployment failed health check"
  if test -n "$previous_image"; then APP_IMAGE="$previous_image" docker compose --env-file .env up -d app; fi
  exit 1
fi
