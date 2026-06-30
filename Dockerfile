# ── Build ──
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ── Runtime ──
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/config /app/data
COPY docker/config/ /app/config/
COPY --from=build /app/target/*.jar app.jar

ENV DEEPSEEK_API_KEY=sk-your-key-here
ENV RSSAGENT_ROUTES_URL=http://rsshub:1200/rsshub/routes/zh

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
