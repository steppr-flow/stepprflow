/**
 * Kafka message broker implementation for Steppr Flow.
 *
 * <p>This package provides:
 * <ul>
 *   <li>Kafka-based message broker for workflow messages</li>
 *   <li>Auto-configuration for Spring Kafka integration</li>
 *   <li>Message listener for processing workflow steps</li>
 *   <li>Kafka-specific message context implementation</li>
 * </ul>
 *
 * <p>The Kafka broker is activated by default when {@code stepprflow.broker=kafka}
 * or when the property is not set (matchIfMissing=true).
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link io.stepprflow.broker.kafka.KafkaMessageBroker} - Sends workflow messages</li>
 *   <li>{@link io.stepprflow.broker.kafka.KafkaMessageListener} - Receives and processes messages</li>
 *   <li>{@link io.stepprflow.broker.kafka.KafkaMessageContext} - Kafka acknowledgment context</li>
 *   <li>{@link io.stepprflow.broker.kafka.KafkaBrokerAutoConfiguration} - Spring Boot auto-config</li>
 * </ul>
 */
package io.stepprflow.broker.kafka;
