package io.stepprflow.core.exception;

/**
 * Exception thrown when a message cannot be sent to the broker.
 */
public class MessageSendException extends MessageBrokerException {

    /** The topic the message was being sent to. */
    private final String topic;

    /** The execution ID if available. */
    private final String executionId;

    /**
     * Constructs a new message send exception.
     *
     * @param brokerType the type of broker
     * @param topicName the topic name
     * @param message the detail message
     */
    public MessageSendException(
            final String brokerType,
            final String topicName,
            final String message) {
        super(brokerType,
              String.format("Failed to send to topic '%s': %s", topicName, message));
        this.topic = topicName;
        this.executionId = null;
    }

    /**
     * Constructs a new message send exception with cause.
     *
     * @param brokerType the type of broker
     * @param topicName the topic name
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MessageSendException(
            final String brokerType,
            final String topicName,
            final String message,
            final Throwable cause) {
        super(brokerType,
              String.format("Failed to send to topic '%s': %s", topicName, message),
              cause);
        this.topic = topicName;
        this.executionId = null;
    }

    /**
     * Constructs a new message send exception with execution ID.
     *
     * @param brokerType the type of broker
     * @param topicName the topic name
     * @param execId the execution ID
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MessageSendException(
            final String brokerType,
            final String topicName,
            final String execId,
            final String message,
            final Throwable cause) {
        super(brokerType,
              String.format("Failed to send to topic '%s' [%s]: %s",
                           topicName, execId, message),
              cause);
        this.topic = topicName;
        this.executionId = execId;
    }

    /**
     * Returns the topic the message was being sent to.
     *
     * @return the topic name
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Returns the execution ID if available.
     *
     * @return the execution ID or null
     */
    public String getExecutionId() {
        return executionId;
    }
}
