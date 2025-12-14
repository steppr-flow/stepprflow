package io.stepprflow.core.exception;

/**
 * Exception thrown when a workflow definition is invalid or cannot be found.
 */
public class WorkflowDefinitionException extends WorkflowException {

    /** The workflow topic. */
    private final String topic;

    /**
     * Constructs a new workflow definition exception.
     *
     * @param message the detail message
     */
    public WorkflowDefinitionException(final String message) {
        super(message);
        this.topic = null;
    }

    /**
     * Constructs a new workflow definition exception with topic.
     *
     * @param topicName the workflow topic
     * @param message the detail message
     */
    public WorkflowDefinitionException(final String topicName, final String message) {
        super(String.format("Workflow '%s': %s", topicName, message));
        this.topic = topicName;
    }

    /**
     * Constructs a new workflow definition exception with topic and cause.
     *
     * @param topicName the workflow topic
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public WorkflowDefinitionException(
            final String topicName,
            final String message,
            final Throwable cause) {
        super(String.format("Workflow '%s': %s", topicName, message), cause);
        this.topic = topicName;
    }

    /**
     * Returns the workflow topic if known.
     *
     * @return the topic or null
     */
    public String getTopic() {
        return topic;
    }
}
