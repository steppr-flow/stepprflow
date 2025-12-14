package io.stepprflow.monitor.util;

import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Factory for creating WorkflowMessage instances from WorkflowExecution data.
 * Centralizes message construction to avoid code duplication.
 */
@Component
public class WorkflowMessageFactory {

    /**
     * Create a message for resuming a workflow execution from a specific step.
     *
     * @param execution the workflow execution to resume
     * @param fromStep  the step to resume from
     * @return a new WorkflowMessage with IN_PROGRESS status
     */
    public WorkflowMessage createResumeMessage(WorkflowExecution execution, int fromStep) {
        if (execution == null) {
            throw new IllegalArgumentException("Execution cannot be null");
        }

        return WorkflowMessage.builder()
                .executionId(execution.getExecutionId())
                .correlationId(execution.getCorrelationId())
                .topic(execution.getTopic())
                .currentStep(fromStep)
                .totalSteps(execution.getTotalSteps())
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(execution.getPayload())
                .payloadType(execution.getPayloadType())
                .securityContext(execution.getSecurityContext())
                .metadata(execution.getMetadata())
                .createdAt(execution.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Create a message for retrying a workflow execution.
     * Uses the current step from the execution.
     *
     * @param execution the workflow execution to retry
     * @return a new WorkflowMessage with IN_PROGRESS status and retry info
     */
    public WorkflowMessage createRetryMessage(WorkflowExecution execution) {
        if (execution == null) {
            throw new IllegalArgumentException("Execution cannot be null");
        }

        return WorkflowMessage.builder()
                .executionId(execution.getExecutionId())
                .correlationId(execution.getCorrelationId())
                .topic(execution.getTopic())
                .currentStep(execution.getCurrentStep())
                .totalSteps(execution.getTotalSteps())
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(execution.getPayload())
                .payloadType(execution.getPayloadType())
                .securityContext(execution.getSecurityContext())
                .metadata(execution.getMetadata())
                .retryInfo(execution.getRetryInfo())
                .createdAt(execution.getCreatedAt())
                .updatedAt(Instant.now())
                .build();
    }
}
