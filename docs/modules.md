# Steppr Flow Modules

This document describes the architecture and purpose of each module in the Steppr Flow monorepo.

## Module Overview

```
steppr-flow/
├── steppr-flow-core/           # Core workflow engine
├── steppr-flow-spring-kafka/   # Kafka broker implementation
├── steppr-flow-spring-rabbitmq/# RabbitMQ broker implementation
├── steppr-flow-spring-monitor/ # Monitoring & REST API
├── steppr-flow-spring-boot-starter/ # Auto-configuration
├── steppr-flow-dashboard/      # Standalone monitoring server
├── steppr-flow-ui/             # Vue.js dashboard frontend
├── steppr-flow-kafka-sample/   # Kafka usage example
├── steppr-flow-rabbitmq-sample/# RabbitMQ usage example
└── steppr-flow-load-tests/     # Performance benchmarks
```

## Core Modules

### steppr-flow-core

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
    <artifactId>steppr-flow-core</artifactId>
    <version>${steppr-flow.version}</version>
</dependency>
```

---

### steppr-flow-spring-kafka

**Purpose:** Apache Kafka implementation of the message broker interface.

**Key Components:**
- `KafkaMessageBroker` - Kafka producer/consumer implementation
- `KafkaMessageListener` - Listens to workflow topics
- `KafkaBrokerAutoConfiguration` - Spring Boot auto-configuration

**Dependencies:**
- `steppr-flow-core`
- `spring-kafka`

**Usage:**
```xml
<dependency>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>steppr-flow-spring-kafka</artifactId>
    <version>${steppr-flow.version}</version>
</dependency>
```

**Configuration:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
steppr-flow:
  kafka:
    enabled: true
```

---

### steppr-flow-spring-rabbitmq

**Purpose:** RabbitMQ implementation of the message broker interface.

**Key Components:**
- `RabbitMQMessageBroker` - RabbitMQ producer/consumer implementation
- `RabbitMQMessageListener` - Listens to workflow queues
- `RabbitMQQueueInitializer` - Creates queues for workflow topics
- `RabbitMQBrokerAutoConfiguration` - Spring Boot auto-configuration

**Dependencies:**
- `steppr-flow-core`
- `spring-amqp`

**Usage:**
```xml
<dependency>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>steppr-flow-spring-rabbitmq</artifactId>
    <version>${steppr-flow.version}</version>
</dependency>
```

**Configuration:**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
steppr-flow:
  rabbitmq:
    enabled: true
```

---

### steppr-flow-spring-monitor

**Purpose:** Monitoring capabilities, REST API, and WebSocket support.

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
- `steppr-flow-core`
- `spring-data-mongodb`
- `spring-websocket`
- `springdoc-openapi`

**REST Endpoints:**
- `GET /api/workflows` - List workflow executions
- `GET /api/workflows/{id}` - Get execution details
- `POST /api/workflows/{id}/resume` - Resume failed workflow
- `POST /api/workflows/{id}/cancel` - Cancel workflow
- `PATCH /api/workflows/{id}/payload` - Update payload
- `GET /api/metrics/dashboard` - Dashboard metrics

---

### steppr-flow-spring-boot-starter

**Purpose:** Spring Boot starter for easy integration.

**Key Components:**
- `AgentAutoConfiguration` - Auto-configures all Steppr Flow components
- `AgentProperties` - Configuration properties
- `WorkflowRegistrationClient` - Registers workflows with central server

**Dependencies:**
- `steppr-flow-core`
- `steppr-flow-spring-monitor`

**Usage:**
```xml
<dependency>
    <groupId>io.github.stepprflow</groupId>
    <artifactId>steppr-flow-spring-boot-starter</artifactId>
    <version>${steppr-flow.version}</version>
</dependency>
```

---

## Application Modules

### steppr-flow-dashboard

**Purpose:** Standalone monitoring server application.

**Description:** A complete Spring Boot application that provides centralized monitoring for all Steppr Flow-enabled microservices. Deploy alongside Kafka and MongoDB.

**Features:**
- REST API for workflow monitoring
- WebSocket for real-time updates
- MongoDB persistence
- Kafka consumer for workflow events

**Dependencies:**
- `steppr-flow-spring-monitor`
- `steppr-flow-spring-kafka`

---

### steppr-flow-ui

**Purpose:** Vue.js dashboard frontend.

**Technology Stack:**
- Vue 3 with Composition API
- Pinia for state management
- Vue Router
- Tailwind CSS
- Vite build tool

**Features:**
- Real-time workflow monitoring
- Execution history and details
- Payload editor
- Metrics dashboard
- Circuit breaker status

---

## Sample Modules

### steppr-flow-kafka-sample

**Purpose:** Example application demonstrating Kafka integration.

**Scenario:** E-commerce order processing workflow with steps:
1. Validate inventory
2. Process payment
3. Prepare shipping
4. Send notification

---

### steppr-flow-rabbitmq-sample

**Purpose:** Example application demonstrating RabbitMQ integration.

**Scenario:** Same e-commerce workflow as Kafka sample, using RabbitMQ.

---

### steppr-flow-load-tests

**Purpose:** Performance benchmarks using Gatling.

**Tests:**
- `WorkflowThroughputSimulation` - High-volume workflow creation
- `WorkflowExecutionBenchmark` - Step execution performance
- `WorkflowApiSimulation` - API endpoint stress testing

---

## Dependency Graph

```
                    ┌─────────────────────┐
                    │   steppr-flow-core  │
                    └──────────┬──────────┘
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ spring-kafka    │ │ spring-rabbitmq │ │ spring-monitor  │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         └──────────────┬────┴───────────────────┘
                        │
                        ▼
              ┌─────────────────────┐
              │ spring-boot-starter │
              └─────────────────────┘
                        │
          ┌─────────────┴─────────────┐
          │                           │
          ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│    dashboard    │         │ kafka/rabbitmq  │
│   (standalone)  │         │    samples      │
└─────────────────┘         └─────────────────┘
```

## Maven Profiles

- **Default build:** Core modules only
- **`full` profile:** Includes samples and load tests

```bash
# Default build
mvn clean install

# Full build with samples
mvn clean install -Pfull
```
