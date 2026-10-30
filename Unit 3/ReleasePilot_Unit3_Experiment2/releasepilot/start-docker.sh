#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
docker build -t releasepilot:local .
docker compose up -d --wait --wait-timeout 180
printf '%s\n' 'Open http://localhost:8082. Stop later with docker compose stop.'
