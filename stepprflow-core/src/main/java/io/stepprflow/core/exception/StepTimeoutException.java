package io.stepprflow.core.exception;

import java.time.Duration;

/**
 * Exception thrown when a workflow step exceeds its configured timeout.
 *
 * <p>This exception is thrown by the StepExecutor when a step takes longer
 * than the timeout specified via the Timeout annotation.
 *
 * <p>Step timeout exceptions are retryable by default, allowing the workflow
 * to retry the step if configured to do so.
 */
public class StepTimeoutException extends WorkflowException {

    /** The step label. */
    private final String stepLabel;

    /** The step ID. */
    private final int stepId;

    /** The configured timeout. */
    private final Duration timeout;

    /** The elapsed time before timeout. */
    private final Duration elapsed;

    /**
     * Constructs a new step timeout exception with elapsed time.
     *
     * @param label the step label
     * @param id the step ID
     * @param timeoutDuration the configured timeout
     * @param elapsedDuration the elapsed time
     */
    public StepTimeoutException(
            final String label,
            final int id,
            final Duration timeoutDuration,
            final Duration elapsedDuration) {
        super(String.format(
            "Step '%s' (id=%d) timed out after %s (timeout: %s)",
            label, id, formatDuration(elapsedDuration), formatDuration(timeoutDuration)
        ));
        this.stepLabel = label;
        this.stepId = id;
        this.timeout = timeoutDuration;
        this.elapsed = elapsedDuration;
    }

    /**
     * Constructs a new step timeout exception.
     *
     * @param label the step label
     * @param id the step ID
     * @param timeoutDuration the configured timeout
     */
    public StepTimeoutException(
            final String label,
            final int id,
            final Duration timeoutDuration) {
        super(String.format(
            "Step '%s' (id=%d) timed out (timeout: %s)",
            label, id, formatDuration(timeoutDuration)
        ));
        this.stepLabel = label;
        this.stepId = id;
        this.timeout = timeoutDuration;
        this.elapsed = null;
    }

    /**
     * Returns the step label.
     *
     * @return the step label
     */
    public String getStepLabel() {
        return stepLabel;
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
     * Returns the configured timeout.
     *
     * @return the timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Returns the elapsed time before timeout.
     *
     * @return the elapsed duration or null
     */
    public Duration getElapsed() {
        return elapsed;
    }

    private static String formatDuration(final Duration duration) {
        if (duration == null) {
            return "unknown";
        }
        long seconds = duration.getSeconds();
        final int secondsPerMinute = 60;
        if (seconds < secondsPerMinute) {
            return seconds + "s";
        }
        long minutes = seconds / secondsPerMinute;
        long remainingSeconds = seconds % secondsPerMinute;
        if (remainingSeconds == 0) {
            return minutes + "m";
        }
        return minutes + "m " + remainingSeconds + "s";
    }
}
