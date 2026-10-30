#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
docker info >/dev/null
docker build -t socialflow:local .
docker compose up -d --wait --wait-timeout 240
echo "Allow 30 seconds for startup, then visit http://localhost:8083"
