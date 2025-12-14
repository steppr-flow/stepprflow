package io.stepprflow.monitor.exception;

import lombok.Getter;

/**
 * Exception thrown when a workflow resume operation fails after the execution
 * attempt has been persisted but before the broker message was sent.
 *
 * <p>This indicates a partial failure state where:
 * <ul>
 *   <li>The execution attempt has been saved to the database</li>
 *   <li>The broker message could not be sent</li>
 * </ul>
 *
 * <p>Recovery options:
 * <ul>
 *   <li>Retry the resume operation (the system will detect and handle the existing attempt)</li>
 *   <li>Manually investigate the workflow state</li>
 * </ul>
 */
@Getter
public class WorkflowResumeException extends RuntimeException {

    private final String executionId;
    private final int attemptNumber;
    private final String brokerType;

    public WorkflowResumeException(String executionId, int attemptNumber,
                                   String brokerType, Throwable cause) {
        super(String.format(
                "Failed to send resume message for workflow %s (attempt %d) to %s broker. " +
                "The execution attempt has been persisted. Retry the resume operation.",
                executionId, attemptNumber, brokerType), cause);
        this.executionId = executionId;
        this.attemptNumber = attemptNumber;
        this.brokerType = brokerType;
    }
}
