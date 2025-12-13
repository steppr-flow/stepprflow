# Steppr Flow Broker Kafka

Apache Kafka implementation of the Steppr Flow message broker.

## Overview

This module provides Kafka-based message transport for Steppr Flow workflows, enabling distributed, high-throughput workflow execution.

## Installation

```xml
<dependency>
    <groupId>io.stepprflow</groupId>
    <artifactId>steppr-flow-spring-kafka</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
stepprflow:
  enabled: true
  broker: kafka
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: stepprflow-workers
      auto-offset-reset: earliest
      concurrency: 3
    producer:
      acks: all
      retries: 3
    trusted-packages:
      - io.stepprflow.core.model
      - com.yourcompany.workflow
```

> **Note:** All Kafka configuration is under `stepprflow.kafka.*`. You do not need to configure `spring.kafka.*` separately.

### Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `stepprflow.kafka.bootstrap-servers` | Kafka broker addresses | `localhost:9092` |
| `stepprflow.kafka.consumer.group-id` | Consumer group ID | `stepprflow` |
| `stepprflow.kafka.consumer.concurrency` | Number of consumer threads | `1` |
| `stepprflow.kafka.producer.acks` | Producer acknowledgment | `all` |
| `stepprflow.kafka.trusted-packages` | Packages for deserialization | `[]` |

## Features

- **Partitioned topics**: Workflows can be distributed across partitions
- **Consumer groups**: Multiple instances share the workload
- **Exactly-once semantics**: With proper Kafka configuration
- **Dead Letter Queue**: Failed messages sent to `*.DLT` topics

## Topic Naming

| Topic Pattern | Description |
|---------------|-------------|
| `{workflow-topic}` | Main workflow messages |
| `{workflow-topic}.DLT` | Dead Letter Topic for failed messages |

## Usage

Steppr Flow auto-configures automatically with Spring Boot. No additional annotations required:

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

Then define your workflows using `@Topic` and `@Step` annotations:

```java
@Component
@Topic("order-workflow")
public class OrderWorkflow {

    @Step(id = 1, label = "Validate")
    public void validate(OrderPayload payload) {
        // Validation logic
    }

    @Step(id = 2, label = "Process")
    public void process(OrderPayload payload) {
        // Processing logic
    }
}
```

## Docker Compose

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
```
