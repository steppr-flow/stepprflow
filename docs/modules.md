# Steppr Flow Modules

This document describes the architecture and purpose of each module in the Steppr Flow monorepo.

## Module Overview

```
stepprflow/
├── stepprflow-core/             # Core workflow engine
├── stepprflow-spring-kafka/     # Kafka broker implementation
├── stepprflow-spring-rabbitmq/  # RabbitMQ broker implementation
├── stepprflow-monitoring/       # Monitoring, REST API, dashboard
├── stepprflow-ui/               # Vue.js 3 + Tailwind CSS frontend
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
- `WorkflowRegistrationClient` - Broker-based registration client that automatically registers workflow definitions with the monitoring server via the shared message broker (Kafka or RabbitMQ). No HTTP or server URL configuration needed.
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
stepprflow:
  broker: kafka
  kafka:
    bootstrap-servers: localhost:9092
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
stepprflow:
  broker: rabbitmq
  rabbitmq:
    host: localhost
    port: 5672
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
- `RegistrationMessageHandler` - Handles incoming registration messages from services (REGISTER/HEARTBEAT/DEREGISTER)
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
- `GET /api/workflows` - List workflow executions (paginated, filterable)
- `GET /api/workflows/{id}` - Get execution details
- `GET /api/workflows/recent` - Get 10 most recent executions
- `GET /api/workflows/stats` - Get aggregated statistics
- `POST /api/workflows/{id}/resume` - Resume failed workflow
- `DELETE /api/workflows/{id}` - Cancel workflow
- `PATCH /api/workflows/{id}/payload` - Update payload
- `POST /api/workflows/{id}/payload/restore` - Restore original payload
- `GET /api/dashboard/overview` - Dashboard overview
- `GET /api/dashboard/workflows` - Get registered workflow definitions with step details
- `GET /api/metrics` - Metrics dashboard (global + per workflow)
- `GET /api/metrics/{topic}` - Get metrics for specific workflow
- `GET /api/metrics/summary` - Get global summary
- `GET /api/registry/workflows` - List all registered workflows
- `GET /api/registry/workflows/{topic}` - Get specific workflow registration
- `GET /api/registry/instances` - List all active instances

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
