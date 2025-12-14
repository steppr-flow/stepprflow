package io.stepprflow.core.exception;

/**
 * Exception thrown when a workflow execution cannot be found.
 */
public class WorkflowNotFoundException extends WorkflowException {

    /** The execution ID. */
    private final String executionId;

    /**
     * Constructs a new workflow not found exception.
     *
     * @param execId the execution ID
     */
    public WorkflowNotFoundException(final String execId) {
        super("Workflow execution not found: " + execId);
        this.executionId = execId;
    }

    /**
     * Constructs a new workflow not found exception with message.
     *
     * @param execId the execution ID
     * @param message the detail message
     */
    public WorkflowNotFoundException(final String execId, final String message) {
        super(message);
        this.executionId = execId;
    }

    /**
     * Returns the execution ID.
     *
     * @return the execution ID
     */
    public String getExecutionId() {
        return executionId;
    }
}
