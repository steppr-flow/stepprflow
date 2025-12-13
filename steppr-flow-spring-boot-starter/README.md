# Steppr Flow Spring Boot Starter

Spring Boot starter for easy integration of Steppr Flow into your applications.

## Overview

This starter provides auto-configuration for Steppr Flow, making it easy to add workflow orchestration capabilities to any Spring Boot application. It includes everything you need: workflow engine, persistence, monitoring, and metrics.

## What's Included

The starter bundles all necessary modules:

- `steppr-flow-core` - Core workflow engine
- `steppr-flow-spring-kafka` - Kafka broker (default)
- `steppr-flow-spring-monitor` - Persistence, REST API, and monitoring
- `spring-boot-starter-data-mongodb` - MongoDB persistence
- `spring-boot-starter-actuator` - Metrics and health checks

## Features

- Auto-configuration of all Steppr Flow components
- Automatic workflow registration via `@Topic` annotation
- Built-in MongoDB persistence for workflow state
- Resume/replay failed workflows from where they stopped
- REST API for workflow monitoring and control
- Metrics via Micrometer/Actuator
- Support for Kafka (default) and RabbitMQ brokers

## Installation

```xml
<dependency>
    <groupId>io.stepprflow</groupId>
    <artifactId>steppr-flow-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Using RabbitMQ instead of Kafka?** Add:

```xml
<dependency>
    <groupId>io.stepprflow</groupId>
    <artifactId>steppr-flow-spring-rabbitmq</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Requirements

- Java 21+
- MongoDB running on `localhost:27017` (default)
- Kafka on `localhost:9092` (default) OR RabbitMQ on `localhost:5672`

Quick start with Docker:

```bash
# MongoDB (required)
docker run -d --name mongodb -p 27017:27017 mongo:latest

# Kafka
docker run -d --name kafka -p 9092:9092 apache/kafka:latest

# OR RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:management
```

## Configuration

### Minimal (Kafka)

```yaml
stepprflow:
  kafka:
    bootstrap-servers: localhost:9092
    trusted-packages:
      - io.stepprflow.core.model
      - com.mycompany.workflow
```

### Minimal (RabbitMQ)

```yaml
stepprflow:
  broker: rabbitmq
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    trusted-packages:
      - io.stepprflow.core.model
      - com.mycompany.workflow
```

### Full Configuration

```yaml
stepprflow:
  enabled: true
  broker: kafka  # or 'rabbitmq'

  # Kafka configuration (when broker: kafka)
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: my-app-workers
      auto-offset-reset: earliest
      concurrency: 1
    producer:
      acks: all
      retries: 3
    trusted-packages:
      - io.stepprflow.core.model
      - com.mycompany.workflow

  # RabbitMQ configuration (when broker: rabbitmq)
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    exchange: stepprflow.workflows
    prefetch-count: 10
    trusted-packages:
      - io.stepprflow.core.model
      - com.mycompany.workflow

  # MongoDB (defaults shown)
  mongodb:
    uri: mongodb://localhost:27017/stepprflow
    database: stepprflow

  # Retry policy
  retry:
    max-attempts: 3
    initial-delay: 1s
    max-delay: 5m
    multiplier: 2.0

  # Dead Letter Queue
  dlq:
    enabled: true
    suffix: .dlq

  # Monitor module
  monitor:
    enabled: true
    web-socket:
      enabled: true
```

## Usage

### 1. Define a Workflow

```java
@Component
@Topic("order-workflow")
public class OrderWorkflow implements StepprFlow {

    @Step(id = 1, label = "Validate Order")
    public void validate(OrderPayload payload) {
        // Validation logic
    }

    @Step(id = 2, label = "Process Payment")
    public void processPayment(OrderPayload payload) {
        // Payment logic
    }

    @Step(id = 3, label = "Send Confirmation")
    public void sendConfirmation(OrderPayload payload) {
        // Notification logic
    }

    @OnSuccess
    public void onComplete(OrderPayload payload) {
        log.info("Order completed: {}", payload.getOrderId());
    }

    @OnFailure
    public void onFailed(OrderPayload payload, Throwable error) {
        log.error("Order failed: {}", error.getMessage());
    }
}
```

### 2. Start a Workflow

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final WorkflowStarter workflowStarter;

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest request) {
        String executionId = workflowStarter.start("order-workflow", new OrderPayload(request));
        return ResponseEntity.accepted().body(executionId);
    }
}
```

### 3. Monitor Workflows

The starter exposes REST endpoints at `/api/workflows`:

- `GET /api/workflows` - List all executions
- `GET /api/workflows/{id}` - Get execution details
- `POST /api/workflows/{id}/resume` - Resume a failed workflow
- `POST /api/workflows/{id}/cancel` - Cancel a workflow

## Architecture

```
┌──────────────────────────────────────────────────┐
│              Your Spring Boot App                │
├──────────────────────────────────────────────────┤
│     steppr-flow-spring-boot-starter              │
│  ┌────────────────────────────────────────────┐  │
│  │  steppr-flow-core (engine)                 │  │
│  │  steppr-flow-spring-kafka (default broker) │  │
│  │  steppr-flow-spring-monitor (persistence)  │  │
│  │  spring-boot-starter-data-mongodb          │  │
│  │  spring-boot-starter-actuator              │  │
│  └────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────┤
│          Kafka / RabbitMQ    │    MongoDB        │
└──────────────────────────────────────────────────┘
```
