# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the app with the Gradle wrapper ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy the build definition first, so downloading dependencies is cached as
# its own layer and only re-runs when these files change.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src
# Tests run in CI and locally against Testcontainers, not inside the image build.
RUN ./gradlew bootJar --no-daemon -x test

# ---- Runtime stage: a JRE only, no build tools, running as a non-root user ----
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --no-create-home certkit
COPY --from=build /workspace/build/libs/*.jar app.jar
USER certkit
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
