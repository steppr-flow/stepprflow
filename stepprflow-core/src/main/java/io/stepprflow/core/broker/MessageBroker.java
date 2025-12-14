package io.stepprflow.core.broker;

import io.stepprflow.core.model.WorkflowMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for message broker operations.
 * Implementations can be Kafka, RabbitMQ, or any other messaging system.
 */
public interface MessageBroker {

    /**
     * Send a message to a destination (topic/queue) asynchronously.
     *
     * @param destination the destination name
     * @param message     the workflow message to send
     */
    void send(String destination, WorkflowMessage message);

    /**
     * Send a message and return a future for async handling.
     *
     * @param destination the destination name
     * @param message     the workflow message to send
     * @return a future that completes when the message is acknowledged
     */
    CompletableFuture<Void> sendAsync(String destination, WorkflowMessage message);

    /**
     * Send a message synchronously (blocking until acknowledged).
     *
     * @param destination the destination name
     * @param message     the workflow message to send
     */
    void sendSync(String destination, WorkflowMessage message);

    /**
     * Get the broker type identifier.
     *
     * @return the broker type (e.g., "kafka", "rabbitmq")
     */
    String getBrokerType();

    /**
     * Check if the broker is available and connected.
     *
     * @return true if the broker is ready
     */
    default boolean isAvailable() {
        return true;
    }
}
