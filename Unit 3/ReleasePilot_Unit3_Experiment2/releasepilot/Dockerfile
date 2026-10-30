FROM eclipse-temurin:17-jre-jammy
RUN groupadd -g 10001 app && useradd -u 10001 -g app app && mkdir -p /app/.data && chown -R app:app /app
ARG COMMIT=local
ENV APP_COMMIT=$COMMIT
WORKDIR /app
COPY --chown=app:app releasepilot.jar /app/app.jar
USER app
ENV SERVER_ADDRESS=0.0.0.0 PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 CMD ["java", "-jar", "/app/app.jar", "--healthcheck"]
ENTRYPOINT ["java","-jar","/app/app.jar"]
