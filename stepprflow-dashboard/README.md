# Steppr Flow Dashboard

Standalone monitoring server combining the monitor module and UI dashboard.

## Overview

A complete, ready-to-deploy monitoring solution that bundles:
- REST API for workflow management
- Vue.js dashboard UI
- WebSocket for real-time updates

## Quick Start

### With Docker Compose

```bash
docker-compose up -d
```

Access the dashboard at `http://localhost:8090`

### Standalone JAR

```bash
# Build
mvn package -pl stepprflow-dashboard -am

# Run
java -jar stepprflow-dashboard/target/stepprflow-dashboard-1.0.0-SNAPSHOT.jar
```

## Configuration

```yaml
server:
  port: 8090

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/stepprflow
  kafka:
    bootstrap-servers: localhost:9092

steppr-flow:
  enabled: true
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8090` |
| `SPRING_DATA_MONGODB_URI` | MongoDB connection | `mongodb://localhost:27017/stepprflow` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `LOG_LEVEL` | Logging level | `INFO` |

## Endpoints

| Path | Description |
|------|-------------|
| `/` | Web UI |
| `/api/workflows` | REST API |
| `/api/dashboard` | Dashboard-specific API |
| `/api/metrics` | Workflow metrics |
| `/actuator/health` | Health check |
| `/actuator/prometheus` | Prometheus metrics |

## Docker

```bash
# Build image
docker build -t stepprflow-dashboard -f stepprflow-dashboard/Dockerfile .

# Run with environment variables
docker run -p 8090:8090 \
  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/stepprflow \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  stepprflow-dashboard
```

## Architecture

```
┌──────────────────────────────────────┐
│          stepprflow-dashboard       │
├──────────────────────────────────────┤
│  ┌────────────┐  ┌────────────────┐  │
│  │  REST API  │  │  Vue.js UI     │  │
│  │  /api/*    │  │  /             │  │
│  └────────────┘  └────────────────┘  │
├──────────────────────────────────────┤
│        stepprflow-spring-monitor    │
├──────────────────────────────────────┤
│  MongoDB  │  Kafka/RabbitMQ          │
└──────────────────────────────────────┘
```
