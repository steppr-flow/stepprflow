package io.stepprflow.core.event;

import io.stepprflow.core.model.WorkflowMessage;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a workflow message is received.
 * This allows other modules (like stepprflow-monitor) to react to workflow
 * message processing without tight coupling.
 */
public class WorkflowMessageEvent extends ApplicationEvent {

    /**
     * The workflow message.
     */
    private final WorkflowMessage message;

    /**
     * Constructor.
     *
     * @param source the source object
     * @param message the workflow message
     */
    public WorkflowMessageEvent(final Object source,
                                final WorkflowMessage message) {
        super(source);
        this.message = message;
    }

    /**
     * Get the workflow message.
     *
     * @return the workflow message
     */
    public WorkflowMessage getMessage() {
        return message;
    }
}
