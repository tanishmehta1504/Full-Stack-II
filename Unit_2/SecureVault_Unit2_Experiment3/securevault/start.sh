#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
printf '%s\n' 'Starting SecureVault. After startup, open http://localhost:8080' 'Press Ctrl+C to stop.'
exec java -jar securevault.jar
