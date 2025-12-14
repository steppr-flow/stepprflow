package io.stepprflow.core.exception;

/**
 * Exception thrown when a step execution fails.
 */
public class StepExecutionException extends WorkflowException {

    /** The step ID. */
    private final int stepId;

    /** The step label. */
    private final String stepLabel;

    /**
     * Constructs a new step execution exception.
     *
     * @param label the step label
     * @param id the step ID
     * @param message the detail message
     */
    public StepExecutionException(
            final String label,
            final int id,
            final String message) {
        super(String.format("Step '%s' (id=%d) failed: %s", label, id, message));
        this.stepId = id;
        this.stepLabel = label;
    }

    /**
     * Constructs a new step execution exception with cause.
     *
     * @param label the step label
     * @param id the step ID
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public StepExecutionException(
            final String label,
            final int id,
            final String message,
            final Throwable cause) {
        super(String.format("Step '%s' (id=%d) failed: %s", label, id, message), cause);
        this.stepId = id;
        this.stepLabel = label;
    }

    /**
     * Constructs a new step execution exception.
     *
     * @param id the step ID
     * @param label the step label
     * @param message the detail message
     * @deprecated Use {@link #StepExecutionException(String, int, String)} instead.
     */
    @Deprecated
    public StepExecutionException(
            final int id,
            final String label,
            final String message) {
        this(label, id, message);
    }

    /**
     * Constructs a new step execution exception with cause.
     *
     * @param id the step ID
     * @param label the step label
     * @param message the detail message
     * @param cause the cause of this exception
     * @deprecated Use {@link #StepExecutionException(String, int, String, Throwable)}
     *             instead.
     */
    @Deprecated
    public StepExecutionException(
            final int id,
            final String label,
            final String message,
            final Throwable cause) {
        this(label, id, message, cause);
    }

    /**
     * Returns the step ID.
     *
     * @return the step ID
     */
    public int getStepId() {
        return stepId;
    }

    /**
     * Returns the step label.
     *
     * @return the step label
     */
    public String getStepLabel() {
        return stepLabel;
    }
}
