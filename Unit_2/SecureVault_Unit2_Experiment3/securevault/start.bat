@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java is not installed or is not on PATH. Install JDK 17 or newer, then reopen this file.
  pause
  exit /b 1
)
echo Starting SecureVault...
echo After the server starts, open http://localhost:8080 in your browser.
echo Keep this window open. Press Ctrl+C to stop.
java -jar securevault.jar
pause
