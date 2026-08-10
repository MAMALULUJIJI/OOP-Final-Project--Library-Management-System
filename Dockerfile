# Build the JAR, then ship it on a JRE. Two stages so the runtime image
# carries no Maven, no sources, and no build cache.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies resolve from pom.xml alone, so this layer is cached until the
# pom actually changes — source edits then rebuild in seconds, not minutes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user; the platform never needs root in this container.
RUN useradd --system --uid 10001 spring
USER spring

COPY --from=build /app/target/library-management-system-1.0-SNAPSHOT.jar app.jar

# Documentation only. The platform injects PORT, and application-prod.properties
# binds to it via server.port=${PORT:8080}.
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
