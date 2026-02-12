/**
 * Transactional Outbox pattern implementation for reliable message delivery.
 *
 * <p>This package provides components for the Transactional Outbox pattern:
 * <ul>
 *   <li>{@link io.github.stepprflow.monitor.outbox.OutboxMessage} - Outbox message entity</li>
 *   <li>{@link io.github.stepprflow.monitor.outbox.OutboxMessageRepository} - Repository for outbox messages</li>
 *   <li>{@link io.github.stepprflow.monitor.outbox.OutboxService} - Service for writing to outbox</li>
 *   <li>{@link io.github.stepprflow.monitor.outbox.OutboxRelayService} - Background relay service</li>
 * </ul>
 *
 * <p>The pattern ensures that database changes and message broker sends are eventually
 * consistent by writing messages to an outbox collection first, then having a background
 * process relay them to the broker.
 */
package io.github.stepprflow.monitor.outbox;
