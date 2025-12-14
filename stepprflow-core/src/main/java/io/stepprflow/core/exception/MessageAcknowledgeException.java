package io.stepprflow.core.exception;

/**
 * Exception thrown when a message acknowledgment operation fails.
 */
public class MessageAcknowledgeException extends MessageBrokerException {

    /** The delivery tag of the message that failed to acknowledge. */
    private final String deliveryTag;

    /**
     * Constructs a new message acknowledge exception.
     *
     * @param brokerType the type of broker
     * @param deliveryTag the delivery tag of the message
     * @param message the detail message
     */
    public MessageAcknowledgeException(
            final String brokerType,
            final String deliveryTag,
            final String message) {
        super(brokerType,
              String.format("Failed to acknowledge message [deliveryTag=%s]: %s",
                           deliveryTag, message));
        this.deliveryTag = deliveryTag;
    }

    /**
     * Constructs a new message acknowledge exception with cause.
     *
     * @param brokerType the type of broker
     * @param deliveryTag the delivery tag of the message
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MessageAcknowledgeException(
            final String brokerType,
            final String deliveryTag,
            final String message,
            final Throwable cause) {
        super(brokerType,
              String.format("Failed to acknowledge message [deliveryTag=%s]: %s",
                           deliveryTag, message),
              cause);
        this.deliveryTag = deliveryTag;
    }

    /**
     * Returns the delivery tag of the message that failed to acknowledge.
     *
     * @return the delivery tag
     */
    public String getDeliveryTag() {
        return deliveryTag;
    }
}
