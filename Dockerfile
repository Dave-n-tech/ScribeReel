# --- Stage 1: Build the Spring Boot Application ---
FROM maven:3.9.6-eclipse-temurin-17-jammy AS build
WORKDIR /app

# Copy the build configuration files
COPY pom.xml .
# (Optional) Pre-download dependencies to speed up future builds
RUN mvn dependency:go-offline -B

# Copy source code and build the application jar
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Create the Final Runtime Image ---
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Update packages and install ffmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    rm -rf /var/lib/apt/lists/*

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render injects a dynamic $PORT variable. We set Spring Boot to listen to it.
ENV PORT=10000
EXPOSE 10000

# Run the application using Render's assigned port
ENTRYPOINT ["java", "-jar", "-Dserver.port=${PORT}", "app.jar"]
