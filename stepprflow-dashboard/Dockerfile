# Multi-stage build for Steppr Flow Dashboard
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom files first for better caching
COPY pom.xml .
COPY steppr-flow-core/pom.xml steppr-flow-core/
COPY steppr-flow-spring-kafka/pom.xml steppr-flow-spring-kafka/
COPY steppr-flow-spring-rabbitmq/pom.xml steppr-flow-spring-rabbitmq/
COPY steppr-flow-spring-monitor/pom.xml steppr-flow-spring-monitor/
COPY steppr-flow-ui/pom.xml steppr-flow-ui/
COPY steppr-flow-spring-boot-starter/pom.xml steppr-flow-spring-boot-starter/
COPY steppr-flow-dashboard/pom.xml steppr-flow-dashboard/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl steppr-flow-dashboard -am -q || true

# Copy source code
COPY steppr-flow-core/src steppr-flow-core/src
COPY steppr-flow-spring-kafka/src steppr-flow-spring-kafka/src
COPY steppr-flow-spring-monitor/src steppr-flow-spring-monitor/src
COPY steppr-flow-ui/src steppr-flow-ui/src
COPY steppr-flow-dashboard/src steppr-flow-dashboard/src

# Copy checkstyle config
COPY config/checkstyle/checkstyle.xml config/checkstyle/checkstyle.xml

# Build the application
RUN mvn clean package -pl steppr-flow-dashboard -am -DskipTests -q

# Extract layers for optimized Docker caching
FROM eclipse-temurin:21-jdk-alpine AS layers
WORKDIR /app
COPY --from=builder /app/steppr-flow-dashboard/target/*.jar app.jar
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
