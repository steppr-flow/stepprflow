# Steppr Flow Spring RabbitMQ Sample

A sample Spring Boot application demonstrating Steppr Flow workflow orchestration with RabbitMQ.

## Overview

This sample implements an order processing workflow with 5 steps:

1. **Validate Order** - Validates order data and business rules
2. **Reserve Inventory** - Reserves products in inventory
3. **Process Payment** - Charges customer payment method
4. **Create Shipment** - Creates shipping label
5. **Send Confirmation** - Sends order confirmation email

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker

## Quick Start

### 1. Start Infrastructure

```bash
# MongoDB (required for persistence)
docker run -d --name mongodb -p 27017:27017 mongo:latest

# RabbitMQ with management UI
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management-alpine
```

### 2. Build and Run

```bash
# From project root
mvn clean install -DskipTests

# Run the sample
cd steppr-flow-spring-rabbitmq-sample
mvn spring-boot:run
```

### 3. Create an Order

```bash
curl -X POST http://localhost:8011/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "customer@example.com",
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Wireless Mouse",
        "quantity": 2,
        "price": 29.99
      },
      {
        "productId": "PROD-002",
        "productName": "USB Keyboard",
        "quantity": 1,
        "price": 49.99
      }
    ],
    "payment": {
      "cardLast4": "4242",
      "cardType": "VISA"
    },
    "shipping": {
      "street": "123 Main Street",
      "city": "Springfield",
      "state": "IL",
      "zipCode": "62701",
      "country": "USA"
    }
  }'
```

Response:
```json
{
  "orderId": "ORD-A1B2C3D4",
  "executionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Order received and processing started"
}
```

### 4. Watch the Logs

You'll see the workflow executing step by step:

```
[Step 1] Validating order: ORD-A1B2C3D4
[Step 1] Order ORD-A1B2C3D4 validated successfully
[Step 2] Reserving inventory for order: ORD-A1B2C3D4
[Step 2] Reserved 2 units of product PROD-001
[Step 2] Reserved 1 units of product PROD-002
[Step 3] Processing payment for order: ORD-A1B2C3D4 (amount: 109.97)
[Step 3] Payment processed for order ORD-A1B2C3D4 (transaction: TXN-12345678)
[Step 4] Creating shipment for order: ORD-A1B2C3D4
[Step 4] Shipment created for order ORD-A1B2C3D4 (tracking: TRACK-ABCDEF1234)
[Step 5] Sending confirmation for order: ORD-A1B2C3D4
[Step 5] Confirmation sent for order ORD-A1B2C3D4
=== ORDER WORKFLOW COMPLETED ===
```

### 5. Check Workflow State in MongoDB

```bash
docker exec -it mongodb mongosh stepprflow --eval "db.workflowExecutions.find().pretty()"
```

## Monitoring API

The starter includes a REST API for workflow monitoring:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workflows` | List all executions |
| GET | `/api/workflows/{id}` | Get execution details |
| POST | `/api/workflows/{id}/resume` | Resume a failed workflow |
| POST | `/api/workflows/{id}/cancel` | Cancel a workflow |

## RabbitMQ Management

Access the management UI at `http://localhost:15672` (guest/guest) to:
- View queues and exchanges
- Monitor message rates
- Inspect dead letter queues

## Sample API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create a new order |

## Testing Failures

The PaymentService has a 10% chance of failing to simulate real-world conditions. When a payment fails:

1. The workflow stops at step 3
2. `@OnFailure` callback is triggered
3. The state is persisted in MongoDB

To resume after fixing the issue:

```bash
curl -X POST "http://localhost:8011/api/workflows/{executionId}/resume"
```

## Testing Inventory Issues

Use `PROD-004` which is out of stock:

```bash
curl -X POST http://localhost:8011/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "customer@example.com",
    "items": [
      {
        "productId": "PROD-004",
        "productName": "Out of Stock Item",
        "quantity": 1,
        "price": 99.99
      }
    ],
    "payment": { "cardLast4": "4242", "cardType": "VISA" },
    "shipping": {
      "street": "123 Main St",
      "city": "Springfield",
      "state": "IL",
      "zipCode": "62701",
      "country": "USA"
    }
  }'
```

## Cleanup

```bash
docker stop mongodb rabbitmq
docker rm mongodb rabbitmq
```
