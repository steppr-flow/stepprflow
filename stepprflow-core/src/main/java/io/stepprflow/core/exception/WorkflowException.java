package io.stepprflow.core.exception;

/**
 * Base exception for workflow errors.
 */
public class WorkflowException extends RuntimeException {

    /**
     * Constructs a new workflow exception with the specified message.
     *
     * @param message the detail message
     */
    public WorkflowException(final String message) {
        super(message);
    }

    /**
     * Constructs a new workflow exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public WorkflowException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
