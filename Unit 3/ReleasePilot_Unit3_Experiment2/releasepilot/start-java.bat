@echo off
cd /d "%~dp0"
echo Java-only mode: http://localhost:8082
java -jar releasepilot.jar
pause
