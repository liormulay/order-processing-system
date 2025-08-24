# Multi-stage build for inventory-service
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy shared library first
COPY shared-lib ./shared-lib/

# Build shared library
RUN cd shared-lib && mvn clean install -DskipTests

# Copy inventory-service source
COPY inventory-service ./inventory-service/

# Build inventory-service
RUN cd inventory-service && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/inventory-service/target/inventory-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8082

# Run the application
CMD ["java", "-jar", "app.jar"]

