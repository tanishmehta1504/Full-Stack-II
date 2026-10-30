@echo off
cd /d "%~dp0"
echo Open http://localhost:8083 when Spring Boot has started.
echo Java-only mode uses H2. Publishing needs Kafka at localhost:9092.
java -jar socialflow.jar
pause
