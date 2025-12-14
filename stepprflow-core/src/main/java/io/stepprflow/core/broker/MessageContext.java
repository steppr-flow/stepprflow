package io.stepprflow.core.broker;

import java.util.Map;

/**
 * Context information for a received message.
 * Provides metadata and acknowledgment operations.
 */
public interface MessageContext {

    /**
     * Get the source destination (topic/queue) of this message.
     *
     * @return the destination name
     */
    String getDestination();

    /**
     * Get message headers/properties.
     *
     * @return map of header names to values
     */
    Map<String, String> getHeaders();

    /**
     * Get a specific header value.
     *
     * @param key the header name
     * @return the header value, or null if not present
     */
    default String getHeader(final String key) {
        Map<String, String> headers = getHeaders();
        return headers != null ? headers.get(key) : null;
    }

    /**
     * Get the message key (partition key for Kafka, routing key for RabbitMQ).
     *
     * @return the message key
     */
    String getMessageKey();

    /**
     * Acknowledge successful processing of the message.
     */
    void acknowledge();

    /**
     * Reject the message, optionally requeueing it.
     *
     * @param requeue whether to requeue the message for reprocessing
     */
    void reject(boolean requeue);

    /**
     * Get the broker-specific offset or delivery tag.
     *
     * @return the offset/tag as a string
     */
    String getOffset();
}
