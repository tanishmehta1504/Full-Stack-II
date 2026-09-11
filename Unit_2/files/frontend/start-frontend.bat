@echo off
title Post Desk Frontend Server
echo =======================================================
echo Serving Post Desk Web Console on http://localhost:5500...
echo =======================================================
python -m http.server 5500
pause
