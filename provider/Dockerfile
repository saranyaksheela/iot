# Multi-stage Dockerfile for Provider Service
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds, run tests separately)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built fat jar from build stage (contains all dependencies)
COPY --from=build /app/target/provider-*.jar /app/provider.jar

# Copy configuration files
COPY --from=build /app/src/main/resources/auth-config.json /app/config/auth-config.json
COPY --from=build /app/src/main/resources/db-config.json /app/config/db-config.json
COPY --from=build /app/src/main/resources/logback.xml /app/config/logback.xml

# Create the expected directory structure and copy config files there too
RUN mkdir -p /app/src/main/resources
COPY --from=build /app/src/main/resources/auth-config.json /app/src/main/resources/auth-config.json
COPY --from=build /app/src/main/resources/db-config.json /app/src/main/resources/db-config.json
COPY --from=build /app/src/main/resources/logback.xml /app/src/main/resources/logback.xml

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app /app/src

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Run the application (fat JAR contains all dependencies)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/provider.jar"]
