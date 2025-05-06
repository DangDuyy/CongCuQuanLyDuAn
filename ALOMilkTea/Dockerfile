# Stage 1: Build the application using Maven
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Set working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Package the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Run the application using JDK runtime
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy JAR file from previous build stage
COPY --from=build /app/target/ALOMilkTea-0.0.1-SNAPSHOT.jar app.jar

# Expose port defined in application.properties (8888)
EXPOSE 8888

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
