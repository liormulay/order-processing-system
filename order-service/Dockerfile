# Multi-stage build for order-service
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy shared library first
COPY shared-lib ./shared-lib/

# Build shared library
RUN cd shared-lib && mvn clean install -DskipTests

# Copy order-service source
COPY order-service ./order-service/

# Build order-service
RUN cd order-service && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/order-service/target/order-service-1.0.0.jar app.jar

# Expose port
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "app.jar"]

