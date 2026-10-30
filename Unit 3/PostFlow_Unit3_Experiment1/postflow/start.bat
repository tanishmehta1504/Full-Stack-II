@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
 echo Install JDK 17 or newer, then reopen this file.
 pause
 exit /b 1
)
docker info >nul 2>nul
if errorlevel 1 (
 echo Open Docker Desktop and wait until its engine is running, then try again.
 pause
 exit /b 1
)
echo Starting Kafka. The first run downloads its Docker image.
docker compose up -d --wait --wait-timeout 180
if errorlevel 1 (
 echo Kafka did not become healthy. Run: docker compose logs kafka
 pause
 exit /b 1
)
echo Starting PostFlow. After startup, open http://localhost:8081
echo Keep this window open. Press Ctrl+C to stop the Java application.
java -jar postflow.jar
pause
