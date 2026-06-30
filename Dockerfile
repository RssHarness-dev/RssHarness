# ── Runtime only (build JAR locally first: ./mvnw package -DskipTests) ──
FROM docker.m.daocloud.io/eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/config /app/data
COPY docker/config/ /app/config/
COPY target/*.jar app.jar

ENV DEEPSEEK_API_KEY=sk-your-key-here
ENV RSSAGENT_ROUTES_URL=http://rsshub:1200/rsshub/routes/zh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
