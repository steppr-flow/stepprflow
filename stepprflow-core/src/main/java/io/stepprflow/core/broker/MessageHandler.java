package io.stepprflow.core.broker;

import io.stepprflow.core.model.WorkflowMessage;

/**
 * Handler for incoming workflow messages.
 * Implementations process messages from the broker.
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * Handle an incoming workflow message.
     *
     * @param message the received workflow message
     * @param context the message context with metadata and ack/nack operations
     */
    void handle(WorkflowMessage message, MessageContext context);
}
