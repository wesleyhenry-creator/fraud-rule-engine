# syntax=docker/dockerfile:1

# ---- Build stage: resolve dependencies (cached layer), compile and run the test suite ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B clean package

# ---- Runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:17-jre
RUN groupadd --system fraud && useradd --system --gid fraud --no-create-home fraud

WORKDIR /app
COPY --from=build /workspace/target/fraud-rule-engine.jar app.jar

USER fraud
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
