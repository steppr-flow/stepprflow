/**
 * Message broker abstraction layer.
 *
 * <p>This package provides interfaces and implementations for message broker
 * operations, supporting multiple messaging systems like Kafka and RabbitMQ.
 *
 * <p>Key interfaces:
 * <ul>
 *   <li>{@link io.stepprflow.core.broker.MessageBroker} - Send messages</li>
 *   <li>{@link io.stepprflow.core.broker.MessageSubscriber} - Subscribe</li>
 *   <li>{@link io.stepprflow.core.broker.MessageHandler} - Process messages</li>
 *   <li>{@link io.stepprflow.core.broker.MessageContext} - Message metadata</li>
 * </ul>
 */
package io.stepprflow.core.broker;
