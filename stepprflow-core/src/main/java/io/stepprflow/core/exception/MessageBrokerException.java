package io.stepprflow.core.exception;

/**
 * Exception thrown when a message broker operation fails.
 *
 * <p>This is the base exception for all broker-related errors, including
 * connection issues, send failures, and receive failures.
 */
public class MessageBrokerException extends WorkflowException {

    /** The type of broker (e.g., "kafka", "rabbitmq"). */
    private final String brokerType;

    /**
     * Constructs a new message broker exception with the specified message.
     *
     * @param message the detail message
     */
    public MessageBrokerException(final String message) {
        super(message);
        this.brokerType = null;
    }

    /**
     * Constructs a new message broker exception with message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MessageBrokerException(final String message, final Throwable cause) {
        super(message, cause);
        this.brokerType = null;
    }

    /**
     * Constructs a new message broker exception with broker type and message.
     *
     * @param brokerType the type of broker
     * @param message the detail message
     */
    public MessageBrokerException(final String brokerType, final String message) {
        super(String.format("[%s] %s", brokerType, message));
        this.brokerType = brokerType;
    }

    /**
     * Constructs a new message broker exception with all details.
     *
     * @param brokerType the type of broker
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MessageBrokerException(
            final String brokerType,
            final String message,
            final Throwable cause) {
        super(String.format("[%s] %s", brokerType, message), cause);
        this.brokerType = brokerType;
    }

    /**
     * Returns the broker type (e.g., "kafka", "rabbitmq") if known.
     *
     * @return the broker type or null if unknown
     */
    public String getBrokerType() {
        return brokerType;
    }
}
