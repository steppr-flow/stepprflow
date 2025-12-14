package io.stepprflow.core.exception;

import lombok.Getter;

/**
 * Exception thrown when all retry attempts are exhausted.
 */
@Getter
public class RetryExhaustedException extends WorkflowException {

    /** The execution ID. */
    private final String executionId;

    /** The number of retry attempts made. */
    private final int attempts;

    /**
     * Constructs a new retry exhausted exception.
     *
     * @param execId the execution ID
     * @param attemptCount the number of attempts made
     * @param message the detail message
     */
    public RetryExhaustedException(
            final String execId,
            final int attemptCount,
            final String message) {
        super(String.format("Workflow '%s' retry exhausted after %d attempts: %s",
                execId, attemptCount, message));
        this.executionId = execId;
        this.attempts = attemptCount;
    }

    /**
     * Constructs a new retry exhausted exception with cause.
     *
     * @param execId the execution ID
     * @param attemptCount the number of attempts made
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public RetryExhaustedException(
            final String execId,
            final int attemptCount,
            final String message,
            final Throwable cause) {
        super(String.format("Workflow '%s' retry exhausted after %d attempts: %s",
                execId, attemptCount, message), cause);
        this.executionId = execId;
        this.attempts = attemptCount;
    }

    /**
     * Constructs a new retry exhausted exception without execution ID.
     *
     * @param attemptCount the number of attempts made
     * @param message the detail message
     * @deprecated Use {@link #RetryExhaustedException(String, int, String)} instead.
     */
    @Deprecated
    public RetryExhaustedException(final int attemptCount, final String message) {
        super(String.format("Retry exhausted after %d attempts: %s",
                            attemptCount, message));
        this.executionId = null;
        this.attempts = attemptCount;
    }
}
