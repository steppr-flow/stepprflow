# Steppr Flow Monitor

Monitoring, persistence, and REST API module for Steppr Flow workflows.

## Overview

This module provides:
- MongoDB persistence for workflow executions
- REST API for querying and managing workflows
- Real-time updates via WebSocket
- Metrics and statistics

## Installation

```xml
<dependency>
    <groupId>io.stepprflow</groupId>
    <artifactId>steppr-flow-spring-monitor</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
stepprflow:
  monitor:
    enabled: true
    retry:
      enabled: true
      poll-interval: 30s
    cleanup:
      enabled: true
      retention-days: 30

  # MongoDB configuration (defaults shown)
  mongodb:
    uri: mongodb://localhost:27017/stepprflow
    database: stepprflow
```

> **Note:** MongoDB configuration is handled internally by Steppr Flow via `stepprflow.mongodb.*` properties. You do not need to configure `spring.data.mongodb` separately.

## REST API

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/workflows` | List executions (paginated) |
| `GET` | `/api/workflows/{id}` | Get execution by ID |
| `GET` | `/api/workflows/stats` | Get statistics |
| `GET` | `/api/workflows/recent` | Get recent executions |
| `POST` | `/api/workflows/{id}/resume` | Resume failed execution |
| `DELETE` | `/api/workflows/{id}` | Cancel execution |
| `PATCH` | `/api/workflows/{id}/payload` | Update payload field |

### Query Parameters

```
GET /api/workflows?page=0&size=20&topic=order-processing&statuses=FAILED,RETRY_PENDING&sortBy=createdAt&direction=DESC
```

| Parameter | Description | Default |
|-----------|-------------|---------|
| `page` | Page number (0-based) | `0` |
| `size` | Page size (1-100) | `20` |
| `topic` | Filter by topic | - |
| `statuses` | Filter by status (comma-separated) | - |
| `sortBy` | Sort field | `createdAt` |
| `direction` | Sort direction (ASC/DESC) | `DESC` |

### Example Response

```json
{
  "content": [
    {
      "executionId": "exec-123",
      "correlationId": "corr-456",
      "topic": "order-processing",
      "status": "COMPLETED",
      "currentStep": 4,
      "totalSteps": 4,
      "payload": { "orderId": "ORD-001" },
      "createdAt": "2024-01-15T10:30:00Z",
      "completedAt": "2024-01-15T10:30:05Z"
    }
  ],
  "totalElements": 150,
  "totalPages": 8
}
```

## WebSocket

Subscribe to real-time updates:

```javascript
const ws = new WebSocket('ws://localhost:8090/ws/workflows');
ws.onmessage = (event) => {
  const update = JSON.parse(event.data);
  console.log('Workflow update:', update);
};
```

## Services

| Service | Description |
|---------|-------------|
| `WorkflowQueryService` | Read operations |
| `WorkflowCommandService` | State changes (resume, cancel) |
| `PayloadManagementService` | Payload updates with optimistic locking |

## Data Model

```
WorkflowExecution
├── executionId (PK)
├── correlationId
├── topic
├── status
├── currentStep / totalSteps
├── payload / originalPayload
├── payloadHistory[]
├── stepHistory[]
├── retryInfo
├── errorInfo
├── createdAt / updatedAt / completedAt
└── version (optimistic locking)
```
