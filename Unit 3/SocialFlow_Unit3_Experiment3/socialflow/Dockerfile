FROM eclipse-temurin:17-jre-jammy
RUN groupadd -g 10001 socialflow && useradd -u 10001 -g socialflow -m socialflow && mkdir -p /app/.data && chown -R socialflow:socialflow /app
WORKDIR /app
COPY --chown=socialflow:socialflow socialflow.jar app.jar
USER socialflow
ENV BIND_ADDRESS=0.0.0.0 PORT=8083
EXPOSE 8083
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
