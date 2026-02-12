# Steppr Flow Modules

This document describes the architecture and purpose of each module in the Steppr Flow monorepo.

## Module Overview

```
stepprflow/
├── stepprflow-core/             # Core workflow engine
├── stepprflow-spring-kafka/     # Kafka broker implementation
├── stepprflow-spring-rabbitmq/  # RabbitMQ broker implementation
├── stepprflow-monitoring/       # Monitoring, REST API, dashboard
├── stepprflow-samples/          # Sample application (Kafka & RabbitMQ)
└── stepprflow-load-tests/       # Performance benchmarks
```

## Core Modules

### stepprflow-core

**Purpose:** Core workflow engine with broker-agnostic abstractions.

**Key Components:**
- `WorkflowRegistry` - Registers and manages workflow definitions
- `StepExecutor` - Executes individual workflow steps
- `WorkflowMessage` - Message format for workflow state
- `MessageBroker` - Abstract interface for message brokers
- Annotations: `@Step`, `@Topic`, `@OnSuccess`, `@OnFailure`

**Dependencies:** None (standalone)

**Usage:**
```xml
<dependency>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>stepprflow-core</artifactId>
    <version>${stepprflow.version}</version>
</dependency>
```

---

### stepprflow-spring-kafka

**Purpose:** Apache Kafka implementation of the message broker interface.

**Key Components:**
- `KafkaMessageBroker` - Kafka producer/consumer implementation
- `KafkaMessageListener` - Listens to workflow topics
- `KafkaBrokerAutoConfiguration` - Spring Boot auto-configuration

**Dependencies:**
- `stepprflow-core`
- `spring-kafka`

**Usage:**
```xml
<dependency>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>stepprflow-spring-kafka</artifactId>
    <version>${stepprflow.version}</version>
</dependency>
```

**Configuration:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
stepprflow:
  broker: kafka
```

---

### stepprflow-spring-rabbitmq

**Purpose:** RabbitMQ implementation of the message broker interface.

**Key Components:**
- `RabbitMQMessageBroker` - RabbitMQ producer/consumer implementation
- `RabbitMQMessageListener` - Listens to workflow queues
- `RabbitMQQueueInitializer` - Creates queues for workflow topics
- `RabbitMQBrokerAutoConfiguration` - Spring Boot auto-configuration

**Dependencies:**
- `stepprflow-core`
- `spring-boot-starter-amqp`

**Usage:**
```xml
<dependency>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>stepprflow-spring-rabbitmq</artifactId>
    <version>${stepprflow.version}</version>
</dependency>
```

**Configuration:**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
stepprflow:
  broker: rabbitmq
```

---

### stepprflow-monitoring

**Purpose:** Monitoring, persistence (MongoDB), REST API, WebSocket, and dashboard.

**Key Components:**
- `WorkflowQueryService` - Query operations for workflow executions
- `WorkflowCommandService` - State-changing operations (resume, cancel)
- `PayloadManagementService` - Payload editing and restoration
- `ExecutionPersistenceService` - MongoDB persistence and event handling
- `RetrySchedulerService` - Automatic retry scheduling
- `WorkflowController` - REST API for workflow operations
- `DashboardController` - Dashboard and overview endpoints
- `MetricsController` - Metrics and statistics endpoints
- `WorkflowBroadcaster` - Real-time updates via WebSocket

**Dependencies:**
- `stepprflow-core`
- `spring-data-mongodb`
- `spring-websocket`
- `spring-kafka` (for monitoring Kafka listener)
- `springdoc-openapi`

**REST Endpoints:**
- `GET /api/workflows` - List workflow executions
- `GET /api/workflows/{id}` - Get execution details
- `POST /api/workflows/{id}/resume` - Resume failed workflow
- `DELETE /api/workflows/{id}` - Cancel workflow
- `PATCH /api/workflows/{id}/payload` - Update payload
- `GET /api/dashboard/overview` - Dashboard overview
- `GET /api/metrics` - Workflow metrics

---

## Optional Modules (profile: `full`)

### stepprflow-samples

**Purpose:** Example application demonstrating both Kafka and RabbitMQ integration.

**Scenario:** E-commerce order processing workflow with steps:
1. Validate inventory
2. Process payment
3. Prepare shipping
4. Send notification

**Profiles:**
- `kafka` - Run with Apache Kafka
- `rabbitmq` - Run with RabbitMQ

---

### stepprflow-load-tests

**Purpose:** Performance benchmarks using Gatling.

**Tests:**
- `WorkflowThroughputSimulation` - High-volume workflow creation
- `WorkflowExecutionBenchmark` - Step execution performance
- `WorkflowApiSimulation` - API endpoint stress testing

---

## Dependency Graph

```
                    ┌─────────────────────┐
                    │   stepprflow-core    │
                    └──────────┬──────────┘
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  spring-kafka   │ │ spring-rabbitmq │ │   monitoring    │
└────────┬────────┘ └────────┬────────┘ └─────────────────┘
         │                   │
         └──────────┬────────┘
                    │
                    ▼
          ┌─────────────────┐
          │     samples     │
          └─────────────────┘
```

## Maven Profiles

- **Default build:** Core modules only (`stepprflow-core`, `stepprflow-spring-kafka`, `stepprflow-spring-rabbitmq`, `stepprflow-monitoring`)
- **`full` profile:** Includes samples and load tests

```bash
# Default build
mvn clean install

# Full build with samples
mvn clean install -Pfull
```
