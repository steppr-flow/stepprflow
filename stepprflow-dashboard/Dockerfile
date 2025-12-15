# Multi-stage build for Steppr Flow Dashboard
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom files first for better caching
COPY pom.xml .
COPY stepprflow-core/pom.xml stepprflow-core/
COPY stepprflow-spring-kafka/pom.xml stepprflow-spring-kafka/
COPY stepprflow-spring-rabbitmq/pom.xml stepprflow-spring-rabbitmq/
COPY stepprflow-ui/pom.xml stepprflow-ui/
COPY stepprflow-spring-boot-starter/pom.xml stepprflow-spring-boot-starter/
COPY stepprflow-spring-monitor/pom.xml stepprflow-spring-monitor/
COPY stepprflow-dashboard/pom.xml stepprflow-dashboard/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl stepprflow-dashboard -am -q || true

# Copy source code
COPY stepprflow-core/src stepprflow-core/src
COPY stepprflow-spring-kafka/src stepprflow-spring-kafka/src
COPY stepprflow-spring-rabbitmq/src stepprflow-spring-rabbitmq/src
COPY stepprflow-spring-boot-starter/src stepprflow-spring-boot-starter/src
COPY stepprflow-spring-monitor/src stepprflow-spring-monitor/src
COPY stepprflow-ui/src stepprflow-ui/src
COPY stepprflow-dashboard/src stepprflow-dashboard/src

# Copy checkstyle config
COPY config/checkstyle/checkstyle.xml config/checkstyle/checkstyle.xml

# Build the application
RUN mvn clean package -pl stepprflow-dashboard -am -DskipTests -q

# Extract layers for optimized Docker caching
FROM eclipse-temurin:21-jdk-alpine AS layers
WORKDIR /app
COPY --from=builder /app/stepprflow-dashboard/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Final runtime image
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Steppr Flow Team"
LABEL description="Steppr Flow Monitoring Dashboard"
LABEL version="1.0.0-SNAPSHOT"

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1000 stepprflow && \
    adduser -u 1000 -G stepprflow -s /bin/sh -D stepprflow

# Copy layers in order of change frequency
COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

# Change ownership
RUN chown -R stepprflow:stepprflow /app

USER stepprflow

# Expose port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
