# build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as a normal user, not root, so that a security hole in the app gives less power
RUN useradd --system --uid 10001 --no-create-home app

COPY --from=builder /workspace/target/*.jar app.jar

USER app
EXPOSE 8080
# "docker ps" shows healthy / unhealthy (curl is included in the base image)
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
