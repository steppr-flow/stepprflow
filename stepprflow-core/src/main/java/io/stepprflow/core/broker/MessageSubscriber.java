package io.stepprflow.core.broker;

import java.util.List;

/**
 * Interface for subscribing to messages from the broker.
 * Implementations handle the registration of listeners for destinations.
 */
public interface MessageSubscriber {

    /**
     * Subscribe to a single destination with a handler.
     *
     * @param destination the destination to subscribe to
     * @param handler     the handler to process messages
     */
    void subscribe(String destination, MessageHandler handler);

    /**
     * Subscribe to multiple destinations with a single handler.
     *
     * @param destinations the destinations to subscribe to
     * @param handler      the handler to process messages
     */
    void subscribe(List<String> destinations, MessageHandler handler);

    /**
     * Subscribe to destinations matching a pattern.
     *
     * @param pattern the pattern to match
     *                (regex for Kafka, routing pattern for RabbitMQ)
     * @param handler the handler to process messages
     */
    void subscribePattern(String pattern, MessageHandler handler);

    /**
     * Unsubscribe from a destination.
     *
     * @param destination the destination to unsubscribe from
     */
    void unsubscribe(String destination);

    /**
     * Start consuming messages (if not auto-started).
     */
    void start();

    /**
     * Stop consuming messages.
     */
    void stop();

    /**
     * Check if the subscriber is currently active.
     *
     * @return true if actively consuming
     */
    boolean isRunning();
}
