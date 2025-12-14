/**
 * Health indicators for Spring Boot Actuator integration.
 *
 * <p>This package provides custom health indicators that expose the health status
 * of various StepprFlow components through the Spring Boot Actuator health endpoint.
 *
 * <p>Available health indicators:
 * <ul>
 *   <li>{@link io.stepprflow.monitor.health.BrokerHealthIndicator} - Message broker connectivity</li>
 *   <li>{@link io.stepprflow.monitor.health.CircuitBreakerHealthIndicator} - Circuit breaker states</li>
 *   <li>{@link io.stepprflow.monitor.health.OutboxHealthIndicator} - Outbox queue status</li>
 * </ul>
 *
 * <p>Example health response:
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "components": {
 *     "stepprflowBroker": {
 *       "status": "UP",
 *       "details": {
 *         "brokerType": "kafka",
 *         "available": true
 *       }
 *     },
 *     "stepprflowCircuitBreaker": {
 *       "status": "UP",
 *       "details": {
 *         "broker-kafka": "CLOSED"
 *       }
 *     },
 *     "stepprflowOutbox": {
 *       "status": "UP",
 *       "details": {
 *         "pending": 5,
 *         "failed": 0
 *       }
 *     }
 *   }
 * }
 * }</pre>
 */
package io.stepprflow.monitor.health;
