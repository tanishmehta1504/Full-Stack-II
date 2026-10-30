@echo off
cd /d "%~dp0"
docker info >nul 2>nul
if errorlevel 1 (
 echo Start Docker Desktop and try again.
 pause
 exit /b 1
)
docker build -t socialflow:local .
if errorlevel 1 goto failed
docker compose up -d --wait --wait-timeout 240
if errorlevel 1 goto failed
echo Allow 30 seconds for Spring Boot to start, then open http://localhost:8083
echo Admin: admin@socialflow.local / Admin@12345
echo Stop later with: docker compose stop
pause
exit /b 0
:failed
echo Setup failed. Check: docker compose logs
pause
exit /b 1
