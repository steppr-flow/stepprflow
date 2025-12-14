package io.stepprflow.core.model;

/**
 * Workflow execution status.
 */
public enum WorkflowStatus {

    /**
     * Workflow created but not yet started.
     */
    PENDING,

    /**
     * Workflow is currently executing.
     */
    IN_PROGRESS,

    /**
     * Workflow completed successfully.
     */
    COMPLETED,

    /**
     * Workflow failed after all retries exhausted.
     */
    FAILED,

    /**
     * Workflow was cancelled.
     */
    CANCELLED,

    /**
     * Workflow is waiting for retry.
     */
    RETRY_PENDING,

    /**
     * Workflow timed out.
     */
    TIMED_OUT,

    /**
     * Workflow is paused.
     */
    PAUSED,

    /**
     * Workflow was skipped.
     */
    SKIPPED,

    /**
     * Step passed successfully (used for intermediate steps).
     */
    PASSED
}
