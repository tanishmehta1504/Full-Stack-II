#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
command -v java >/dev/null || { echo 'Install Java 17 or newer.'; exit 1; }
docker info >/dev/null 2>&1 || { echo 'Start Docker first.'; exit 1; }
docker compose up -d --wait --wait-timeout 180
printf '%s\n' 'Starting PostFlow. After startup, open http://localhost:8081'
exec java -jar postflow.jar
