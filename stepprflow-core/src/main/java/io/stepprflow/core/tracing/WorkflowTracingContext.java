package io.stepprflow.core.tracing;

import io.micrometer.observation.Observation;
import lombok.Getter;
import lombok.Setter;

/**
 * Observation context for workflow step execution.
 * Holds all the contextual information needed for tracing.
 */
@Getter
@Setter
public class WorkflowTracingContext extends Observation.Context {

    /**
     * The execution ID.
     */
    private String executionId;

    /**
     * The correlation ID.
     */
    private String correlationId;

    /**
     * The workflow topic.
     */
    private String topic;

    /**
     * The step ID.
     */
    private int stepId;

    /**
     * The step label.
     */
    private String stepLabel;

    /**
     * The total number of steps.
     */
    private int totalSteps;

    /**
     * The status of the step execution.
     */
    private String status = "IN_PROGRESS";

    /**
     * Constructor.
     *
     * @param executionId the execution ID
     * @param correlationId the correlation ID
     * @param topic the workflow topic
     * @param stepId the step ID
     * @param stepLabel the step label
     * @param totalSteps the total number of steps
     */
    public WorkflowTracingContext(final String executionId,
                                   final String correlationId,
                                   final String topic,
                                   final int stepId,
                                   final String stepLabel,
                                   final int totalSteps) {
        this.executionId = executionId;
        this.correlationId = correlationId;
        this.topic = topic;
        this.stepId = stepId;
        this.stepLabel = stepLabel;
        this.totalSteps = totalSteps;
    }

    /**
     * Mark the step execution as successful.
     */
    public void markSuccess() {
        this.status = "SUCCESS";
    }

    /**
     * Mark the step execution as failed.
     */
    public void markFailed() {
        this.status = "FAILED";
    }

    /**
     * Mark the step execution as timed out.
     */
    public void markTimeout() {
        this.status = "TIMEOUT";
    }
}
