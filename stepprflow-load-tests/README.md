# Steppr Flow Load Tests

Performance and load testing suite for Steppr Flow workflows.

## Overview

This module contains load tests to validate Steppr Flow performance under various conditions using Gatling.

## Running Tests

### Prerequisites

- Java 21+
- Running Kafka/RabbitMQ instance
- Running MongoDB instance

### Execute Load Tests

```bash
# Run all load tests
mvn gatling:test

# Run specific simulation
mvn gatling:test -Dgatling.simulationClass=WorkflowLoadSimulation
```

## Test Scenarios

### 1. Throughput Test
- Constant rate of workflow starts
- Measures messages/second

### 2. Stress Test
- Gradually increasing load
- Identifies breaking point

### 3. Soak Test
- Extended duration
- Detects memory leaks

### 4. Spike Test
- Sudden load increase
- Tests recovery

## Configuration

```yaml
# src/test/resources/load-test.yml
load-test:
  target-url: http://localhost:8080
  duration: 5m
  users:
    initial: 10
    max: 100
    ramp-up: 30s
```

## Reports

After execution, reports are generated in:
```
target/gatling/results/
└── workflowloadsimulation-{timestamp}/
    └── index.html
```

## Metrics

| Metric | Target |
|--------|--------|
| Throughput | > 1000 msg/s |
| P99 Latency | < 500ms |
| Error Rate | < 0.1% |
