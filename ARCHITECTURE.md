# StepprFlow - Architecture Documentation

## Overview

**StepprFlow** is a multi-broker workflow orchestration framework for Spring Boot. It enables creating resilient asynchronous workflows with Kafka and RabbitMQ support.

---

## Module Structure

```
stepprflow-parent/
├── stepprflow-core              # Foundation (annotations, abstractions, models)
├── stepprflow-spring-kafka      # Kafka implementation
├── stepprflow-spring-rabbitmq   # RabbitMQ implementation
├── stepprflow-spring-boot-starter  # Starter for apps (includes core + kafka + monitor + agent)
├── stepprflow-spring-monitor    # Monitoring, MongoDB persistence, REST API, WebSocket
├── stepprflow-dashboard         # Standalone monitoring server (Docker)
├── stepprflow-ui                # Vue.js frontend
└── stepprflow-samples           # Examples (Kafka/RabbitMQ profiles)
```

---

## Dependency Graph

```
                         ┌─────────────────┐
                         │ stepprflow-core │
                         └────────┬────────┘
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
              ▼                   ▼                   ▼
   ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
   │ stepprflow-      │ │ stepprflow-      │ │ stepprflow-      │
   │ spring-kafka     │ │ spring-rabbitmq  │ │ spring-monitor   │
   └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
            │                    │                    │
            │                    │                    │
            └──────────┬─────────┴──────────┬─────────┘
                       │                    │
                       ▼                    ▼
          ┌──────────────────────┐ ┌──────────────────────┐
          │ stepprflow-spring-   │ │ stepprflow-dashboard │
          │ boot-starter         │ │ (Docker container)   │
          └──────────┬───────────┘ └──────────────────────┘
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
┌──────────────────────┐ ┌──────────────────────┐
│ User applications    │ │ stepprflow-samples   │
│                      │ │ (kafka/rmq profiles) │
└──────────────────────┘ └──────────────────────┘
```

---

## Module Responsibilities

| Module | Responsibility |
|--------|----------------|
| **core** | Annotations (`@Topic`, `@Step`), interfaces (`MessageBroker`, `StepprFlow`), models (`WorkflowMessage`), base services |
| **spring-kafka** | `KafkaMessageBroker`, `KafkaMessageListener`, Kafka auto-configuration |
| **spring-rabbitmq** | `RabbitMQMessageBroker`, `RabbitMQMessageListener`, RabbitMQ auto-configuration |
| **spring-monitor** | MongoDB persistence, REST API (`/api/workflows`, `/api/registry`), real-time WebSocket, metrics |
| **spring-boot-starter** | Aggregator: includes core + kafka + monitor + auto-registration agent with dashboard |
| **dashboard** | Spring Boot monitoring application deployable in Docker (includes monitor + UI) |
| **ui** | Vue.js interface for workflow visualization |
| **samples** | Usage examples (`kafka` and `rabbitmq` profiles) |

---

## Workflow Execution Flow

```
1. START
   Application → WorkflowStarter.start("order-workflow", payload)
                          │
                          ▼
   WorkflowMessage created (executionId, status=PENDING, step=1)
                          │
                          ▼
   MessageBroker.send() → Kafka Topic / RabbitMQ Queue

2. CONSUMPTION
   KafkaMessageListener receives message
                          │
                          ├──────────────────────────────┐
                          │                              │
                          ▼                              ▼
   StepExecutor.execute()              ExecutionPersistenceService
   - Loads WorkflowDefinition          - Saves to MongoDB
   - Deserializes payload              - Step history tracking
   - Invokes @Step method
   - Handles success/failure
                          │
                          ▼
3. RESULT
   ├─ SUCCESS → Next step or COMPLETED
   ├─ RETRYABLE FAILURE → Retry with exponential backoff
   └─ FATAL FAILURE → Dead Letter Queue (DLQ)

4. CALLBACKS
   @OnSuccess or @OnFailure invoked at the end
```

---

## Core Data Model

### WorkflowMessage
```
executionId      : Unique UUID
correlationId    : Trace ID
topic            : Workflow name
currentStep      : Current step (1-based)
totalSteps       : Total number of steps
status           : PENDING | IN_PROGRESS | COMPLETED | FAILED | RETRY_PENDING
payload          : Business data (JSON)
retryInfo        : Attempts, delay, previous error
errorInfo        : Code, message, stacktrace
metadata         : Additional context
```

### WorkflowExecution (MongoDB)
```javascript
{
  _id: "executionId",
  topic: "order-workflow",
  status: "COMPLETED",
  stepHistory: [
    { stepId: 1, stepLabel: "Validate", status: "PASSED", durationMs: 45 },
    { stepId: 2, stepLabel: "Process", status: "PASSED", durationMs: 1200 }
  ],
  payload: { ... },
  createdAt: timestamp,
  completedAt: timestamp,
  durationMs: 1245
}
```

---

## Essential Configuration

```yaml
stepprflow:
  enabled: true
  broker: kafka  # or rabbitmq

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: my-app-workers
    trusted-packages:
      - io.stepprflow.core.model
      - com.mycompany.model

  retry:
    max-attempts: 3
    initial-delay: 1s
    multiplier: 2.0
    max-delay: 30s

  dlq:
    enabled: true
    suffix: ".dlq"

  mongodb:
    uri: mongodb://localhost:27017/stepprflow

  # Agent auto-registration with dashboard
  agent:
    server-url: http://localhost:8090
    auto-register: true
    heartbeat-interval-seconds: 30
```

---

## Usage Example

```java
@Service
@Topic("order-workflow")
public class OrderWorkflow implements StepprFlow {

    @Step(id = 1, label = "Validate Order")
    public void validate(OrderPayload payload) {
        if (payload.getItems().isEmpty())
            throw new IllegalArgumentException("No items");
    }

    @Step(id = 2, label = "Process Payment")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void payment(OrderPayload payload) {
        paymentService.charge(payload);
    }

    @Step(id = 3, label = "Ship Order")
    public void ship(OrderPayload payload) {
        shippingService.createShipment(payload);
    }

    @OnSuccess
    public void onComplete(WorkflowMessage msg) {
        log.info("Order {} completed", msg.getExecutionId());
    }

    @OnFailure
    public void onFailed(WorkflowMessage msg) {
        // Compensation logic
    }
}
```

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21+ |
| Framework | Spring Boot 3.5+ |
| Default Broker | Apache Kafka |
| Alternative Broker | RabbitMQ |
| Persistence | MongoDB |
| Metrics | Micrometer |
| Resilience | Resilience4j |
| Frontend | Vue.js 3 |
| Build | Maven |
| Tests | Testcontainers |

---

## Architectural Patterns

- **Pub/Sub**: Communication via Kafka topics / RabbitMQ queues
- **Event Sourcing**: `WorkflowMessageEvent` for asynchronous reactions
- **State Machine**: `WorkflowStatus` transitions
- **Strategy**: `MessageBroker` abstracts Kafka/RabbitMQ
- **Exponential Backoff Retry**: Transient failure handling
- **Circuit Breaker**: Protection against failure cascades
- **Dead Letter Queue**: Failed message isolation
- **Saga (coming-soon)**: Workflow steps as compensable actions