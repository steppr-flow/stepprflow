# Steppr Flow

A multi-broker workflow orchestration framework for Spring Boot applications.

Steppr Flow enables you to build resilient, async multi-step workflows with support for multiple message brokers (Kafka, RabbitMQ).

## Features

- **Annotation-driven workflows** - Define workflows using simple annotations
- **Multi-broker support** - Kafka and RabbitMQ implementations included
- **Automatic retries** - Built-in retry handling with configurable policies
- **Step-by-step execution** - Each workflow step executes independently
- **Monitoring ready** - Optional monitoring module with MongoDB persistence

## Modules

| Module | Description |
|--------|-------------|
| `steppr-flow-core` | Core framework, annotations, and abstractions |
| `steppr-flow-spring-kafka` | Apache Kafka message broker implementation |
| `steppr-flow-spring-rabbitmq` | RabbitMQ message broker implementation |
| `steppr-flow-spring-monitor` | Monitoring, persistence, and REST API |
| `steppr-flow-spring-boot-starter` | Spring Boot auto-configuration |
| `steppr-flow-dashboard` | Standalone monitoring server |
| `steppr-flow-ui` | Vue.js dashboard UI for workflow monitoring |

## Quick Start

### 1. Add Dependencies

**Maven :**
```xml
<dependency>
    <groupId>io.stepprflow</groupId>
    <artifactId>steppr-flow-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Define a Workflow

```java
@Component
@Topic("order-processing")
public class OrderWorkflow {

    @Step(id = 1, label = "Validate order")
    public OrderPayload validateOrder(OrderPayload payload) {
        // Validation logic
        if (payload.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have items");
        }
        return payload;
    }

    @Step(id = 2, label = "Reserve inventory")
    public OrderPayload reserveInventory(OrderPayload payload) {
        // Reserve items in inventory
        inventoryService.reserve(payload.getItems());
        return payload;
    }

    @Step(id = 3, label = "Process payment")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public OrderPayload processPayment(OrderPayload payload) {
        // Payment processing
        paymentService.charge(payload.getPaymentInfo());
        return payload;
    }

    @Step(id = 4, label = "Send confirmation")
    public OrderPayload sendConfirmation(OrderPayload payload) {
        // Send confirmation email
        notificationService.sendOrderConfirmation(payload);
        return payload;
    }
}
```

### 3. Start a Workflow

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final WorkflowStarter workflowStarter;

    public OrderController(WorkflowStarter workflowStarter) {
        this.workflowStarter = workflowStarter;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderRequest request) {
        OrderPayload payload = new OrderPayload(request);

        String executionId = workflowStarter.start("order-processing", payload);

        return ResponseEntity.accepted().body(executionId);
    }
}
```

### 4. Configure the Broker

**application.yml (Kafka):**
```yaml
steppr-flow:
  enabled: true

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

**application.yml (RabbitMQ):**
```yaml
steppr-flow:
  enabled: true

spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

## Annotations

### @Topic

Marks a class as a workflow handler.

```java
@Component
@Topic(value = "order-processing", description = "Handles order lifecycle")
public class OrderWorkflow { }
```

| Attribute | Description | Default |
|-----------|-------------|---------|
| `value` | Topic/queue name | required |
| `description` | Documentation | `""` |
| `partitions` | Kafka partitions | `1` |
| `replication` | Kafka replication factor | `1` |

### @Step

Marks a method as a workflow step.

```java
@Step(id = 1, label = "Validate", description = "Validates input data")
public Payload validate(Payload payload) {
    return payload;
}
```

| Attribute | Description | Default |
|-----------|-------------|---------|
| `id` | Step order (must be unique) | required |
| `label` | Human-readable name | required |
| `description` | Documentation | `""` |
| `skippable` | Can skip on retry | `false` |
| `continueOnFailure` | Continue if step fails | `false` |

### @Timeout

Sets execution timeout for a step.

```java
@Step(id = 1, label = "External API call")
@Timeout(value = 30, unit = TimeUnit.SECONDS)
public Payload callExternalApi(Payload payload) {
    return payload;
}
```

### @OnSuccess / @OnFailure

Define callbacks for workflow completion.

```java
@OnSuccess
public void onComplete(WorkflowMessage message) {
    log.info("Workflow {} completed", message.getExecutionId());
}

@OnFailure
public void onFailed(WorkflowMessage message) {
    log.error("Workflow {} failed: {}",
        message.getExecutionId(),
        message.getErrorInfo().getMessage());
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Application                            │
├─────────────────────────────────────────────────────────────┤
│  WorkflowStarter  │  @Topic Workflows  │  Step Handlers     │
├─────────────────────────────────────────────────────────────┤
│                    steppr-flow-core                         │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐     │
│  │ StepExecutor │  │WorkflowRegistry│  │MessageBroker │     │
│  └──────────────┘  └────────────────┘  └──────────────┘     │
├─────────────────────────────────────────────────────────────┤
│  steppr-flow-spring-kafka  │  steppr-flow-spring-rabbitmq   │
├─────────────────────────────────────────────────────────────┤
│          Apache Kafka       │        RabbitMQ               │
└─────────────────────────────────────────────────────────────┘
```

## Message Flow

1. **Start**: `WorkflowStarter.start(topic, payload)` creates a `WorkflowMessage` and sends it to the broker
2. **Execute**: `StepExecutor` receives the message, finds the workflow, and executes the current step
3. **Next**: On success, the message is updated and sent back for the next step
4. **Complete**: After the last step, `@OnSuccess` callback is triggered
5. **Failure**: On error, retry logic is applied or `@OnFailure` callback is triggered

## Building

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Skip tests
mvn install -DskipTests
```

## Requirements

- Java 21+
- Spring Boot 3.5+
- Apache Kafka 3.x or RabbitMQ 3.12+
- Docker (for integration tests)

## License

Apache License 2.0

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.
