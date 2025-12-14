package io.stepprflow.core.exception;

/**
 * Exception thrown when a message rejection operation fails.
 */
public class MessageRejectException extends MessageBrokerException {

    /** The delivery tag of the message that failed to reject. */
    private final String deliveryTag;

    /** Whether the message was requested to be requeued. */
    private final boolean requeue;

    /**
     * Constructs a new message reject exception.
     *
     * @param brokerType the type of broker
     * @param deliveryTag the delivery tag of the message
     * @param requeue whether requeue was requested
     * @param message the detail message
     */
    public MessageRejectException(
            final String brokerType,
            final String deliveryTag,
            final boolean requeue,
            final String message) {
        super(brokerType,
              String.format("Failed to reject message [deliveryTag=%s, requeue=%s]: %s",
                           deliveryTag, requeue, message));
        this.deliveryTag = deliveryTag;
        this.requeue = requeue;
    }

    /**
     * Constructs a new message reject exception with cause.
     *
     * @param brokerType the type of broker
     * @param deliveryTag the delivery tag of the message
     * @param requeue whether requeue was requested
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public MessageRejectException(
            final String brokerType,
            final String deliveryTag,
            final boolean requeue,
            final String message,
            final Throwable cause) {
        super(brokerType,
              String.format("Failed to reject message [deliveryTag=%s, requeue=%s]: %s",
                           deliveryTag, requeue, message),
              cause);
        this.deliveryTag = deliveryTag;
        this.requeue = requeue;
    }

    /**
     * Returns the delivery tag of the message that failed to reject.
     *
     * @return the delivery tag
     */
    public String getDeliveryTag() {
        return deliveryTag;
    }

    /**
     * Returns whether the message was requested to be requeued.
     *
     * @return true if requeue was requested
     */
    public boolean isRequeue() {
        return requeue;
    }
}
