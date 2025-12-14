/**
 * RabbitMQ message broker implementation for Steppr Flow.
 *
 * <p>This package provides:
 * <ul>
 *   <li>RabbitMQ-based message broker for workflow messages</li>
 *   <li>Auto-configuration for Spring AMQP integration</li>
 *   <li>Message listener for processing workflow steps</li>
 *   <li>Queue initializer for automatic queue/exchange setup</li>
 *   <li>RabbitMQ-specific message context implementation</li>
 * </ul>
 *
 * <p>The RabbitMQ broker is activated when {@code stepprflow.broker=rabbitmq}.
 *
 * <p>Key components:
 * <ul>
 *   <li>{@link io.stepprflow.broker.rabbitmq.RabbitMQMessageBroker} - Sends workflow messages</li>
 *   <li>{@link io.stepprflow.broker.rabbitmq.RabbitMQMessageListener} - Receives and processes messages</li>
 *   <li>{@link io.stepprflow.broker.rabbitmq.RabbitMQMessageContext} - RabbitMQ acknowledgment context</li>
 *   <li>{@link io.stepprflow.broker.rabbitmq.RabbitMQQueueInitializer} - Queue/exchange setup</li>
 *   <li>{@link io.stepprflow.broker.rabbitmq.RabbitMQBrokerAutoConfiguration} - Spring Boot auto-config</li>
 * </ul>
 */
package io.stepprflow.broker.rabbitmq;
