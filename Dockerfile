# ── Build stage ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ── Runtime stage ──
FROM eclipse-temurin:21-jre
WORKDIR /app

# Config & data dirs; Docker-specific config
RUN mkdir -p /app/config /app/data
COPY docker/config/ /app/config/
COPY --from=build /app/target/*.jar app.jar

# API key & RSSHub URL — configure via docker run -e or docker-compose
ENV DEEPSEEK_API_KEY=sk-your-key-here
ENV RSSAGENT_ROUTES_URL=http://rsshub:1200/rsshub/routes/zh
ENV JAVA_OPTS="-Xmx256m"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
