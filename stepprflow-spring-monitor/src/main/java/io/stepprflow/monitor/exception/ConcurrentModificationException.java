package io.stepprflow.monitor.exception;

/**
 * Exception thrown when a concurrent modification is detected during payload updates.
 * This happens when two processes try to update the same workflow execution simultaneously.
 */
public class ConcurrentModificationException extends RuntimeException {

    private final String executionId;

    public ConcurrentModificationException(String executionId) {
        super("Execution " + executionId + " was modified by another process. Please refresh and try again.");
        this.executionId = executionId;
    }

    public ConcurrentModificationException(String executionId, Throwable cause) {
        super("Execution " + executionId + " was modified by another process. Please refresh and try again.", cause);
        this.executionId = executionId;
    }

    public String getExecutionId() {
        return executionId;
    }
}
