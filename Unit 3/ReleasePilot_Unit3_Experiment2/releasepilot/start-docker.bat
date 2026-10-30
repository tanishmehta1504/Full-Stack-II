@echo off
cd /d "%~dp0"
docker info >nul 2>nul
if errorlevel 1 (
 echo Start Docker Desktop first, then try again.
 pause
 exit /b 1
)
docker build -t releasepilot:local .
if errorlevel 1 goto failed
docker compose up -d --wait --wait-timeout 180
if errorlevel 1 goto failed
echo Open http://localhost:8082 in your browser.
echo Stop later using: docker compose stop
pause
exit /b 0
:failed
echo Setup failed. Inspect: docker compose logs
pause
exit /b 1
