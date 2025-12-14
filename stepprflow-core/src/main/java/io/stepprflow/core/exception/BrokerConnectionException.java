package io.stepprflow.core.exception;

/**
 * Exception thrown when unable to connect to a message broker.
 */
public class BrokerConnectionException extends MessageBrokerException {

    /** The bootstrap servers or connection URL. */
    private final String bootstrapServers;

    /**
     * Constructs a new broker connection exception.
     *
     * @param brokerType the type of broker
     * @param bootstrapServers the bootstrap servers or connection URL
     * @param message the detail message
     */
    public BrokerConnectionException(
            final String brokerType,
            final String bootstrapServers,
            final String message) {
        super(brokerType,
              String.format("Failed to connect to %s: %s", bootstrapServers, message));
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * Constructs a new broker connection exception with cause.
     *
     * @param brokerType the type of broker
     * @param bootstrapServers the bootstrap servers or connection URL
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public BrokerConnectionException(
            final String brokerType,
            final String bootstrapServers,
            final String message,
            final Throwable cause) {
        super(brokerType,
              String.format("Failed to connect to %s: %s", bootstrapServers, message),
              cause);
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * Returns the bootstrap servers or connection URL.
     *
     * @return the bootstrap servers
     */
    public String getBootstrapServers() {
        return bootstrapServers;
    }
}
