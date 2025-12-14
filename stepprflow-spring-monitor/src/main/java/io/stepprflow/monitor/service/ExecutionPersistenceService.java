package io.stepprflow.monitor.service;

import io.stepprflow.core.event.WorkflowMessageEvent;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.core.service.WorkflowRegistry;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import io.stepprflow.monitor.websocket.WorkflowBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service that persists workflow execution state to MongoDB.
 *
 * This service listens for WorkflowMessageEvent from the broker module
 * and persists workflow state changes.
 */
@Service
@Slf4j
public class ExecutionPersistenceService {

    private final WorkflowExecutionRepository repository;
    private final WorkflowBroadcaster broadcaster;
    private final WorkflowRegistry workflowRegistry;

    @Autowired
    public ExecutionPersistenceService(
            WorkflowExecutionRepository repository,
            @Autowired(required = false) WorkflowBroadcaster broadcaster,
            WorkflowRegistry workflowRegistry) {
        this.repository = repository;
        this.broadcaster = broadcaster;
        this.workflowRegistry = workflowRegistry;
    }

    /**
     * Event listener that persists workflow messages to MongoDB.
     * Runs asynchronously to avoid blocking the Kafka consumer thread.
     */
    @Async
    @EventListener
    public void handleWorkflowMessageEvent(WorkflowMessageEvent event) {
        log.info("Received WorkflowMessageEvent: executionId={}, status={}",
                event.getMessage().getExecutionId(), event.getMessage().getStatus());
        onWorkflowMessage(event.getMessage());
    }

    /**
     * Process a workflow message and persist state changes.
     *
     * @param message the workflow message to persist
     */
    public void onWorkflowMessage(WorkflowMessage message) {
        if (message == null) {
            return;
        }

        log.debug("Persisting workflow state: executionId={}, step={}, status={}",
                message.getExecutionId(), message.getCurrentStep(), message.getStatus());

        WorkflowExecution execution = repository.findById(message.getExecutionId())
                .orElse(createNewExecution(message));

        updateExecution(execution, message);
        repository.save(execution);
        log.info("Persisted workflow execution: executionId={}, status={}",
                message.getExecutionId(), message.getStatus());

        // Notify via WebSocket (if available)
        if (broadcaster != null) {
            broadcaster.broadcastUpdate(execution);
        }
    }

    private WorkflowExecution createNewExecution(WorkflowMessage message) {
        Instant now = Instant.now();

        // Create the first execution attempt
        WorkflowExecution.ExecutionAttempt firstAttempt = WorkflowExecution.ExecutionAttempt.builder()
                .attemptNumber(1)
                .startedAt(now)
                .startStep(1)
                .build();

        List<WorkflowExecution.ExecutionAttempt> attempts = new ArrayList<>();
        attempts.add(firstAttempt);

        return WorkflowExecution.builder()
                .executionId(message.getExecutionId())
                .correlationId(message.getCorrelationId())
                .topic(message.getTopic())
                .totalSteps(message.getTotalSteps())
                .payload(message.getPayload())
                .payloadType(message.getPayloadType())
                .securityContext(message.getSecurityContext())
                .metadata(message.getMetadata())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt() : now)
                .stepHistory(new ArrayList<>())
                .executionAttempts(attempts)
                .build();
    }

    private void updateExecution(WorkflowExecution execution, WorkflowMessage message) {
        execution.setStatus(message.getStatus());
        execution.setCurrentStep(message.getCurrentStep());
        execution.setRetryInfo(message.getRetryInfo());
        execution.setErrorInfo(message.getErrorInfo());
        execution.setUpdatedAt(Instant.now());

        // Add step to history for all statuses that indicate step activity
        if (message.getStatus() == WorkflowStatus.PENDING ||
            message.getStatus() == WorkflowStatus.IN_PROGRESS ||
            message.getStatus() == WorkflowStatus.COMPLETED ||
            message.getStatus() == WorkflowStatus.FAILED ||
            message.getStatus() == WorkflowStatus.RETRY_PENDING) {

            addStepToHistory(execution, message);
        }

        // Set completion time and duration
        if (message.getStatus() == WorkflowStatus.COMPLETED ||
            message.getStatus() == WorkflowStatus.FAILED ||
            message.getStatus() == WorkflowStatus.CANCELLED) {

            Instant completedAt = Instant.now();
            execution.setCompletedAt(completedAt);

            if (execution.getCreatedAt() != null) {
                execution.setDurationMs(
                        completedAt.toEpochMilli() - execution.getCreatedAt().toEpochMilli());
            }

            // Finalize the current execution attempt
            finalizeCurrentAttempt(execution, message);
        }
    }

    /**
     * Finalize the current execution attempt with result information.
     */
    private void finalizeCurrentAttempt(WorkflowExecution execution, WorkflowMessage message) {
        execution.getCurrentAttempt().ifPresent(currentAttempt -> {
            // Only update if not already finalized
            if (currentAttempt.getResult() == null) {
                currentAttempt.setEndedAt(Instant.now());
                currentAttempt.setResult(message.getStatus());
                currentAttempt.setEndStep(message.getCurrentStep());

                if (message.getErrorInfo() != null) {
                    currentAttempt.setErrorMessage(message.getErrorInfo().getMessage());
                }
            }
        });
    }

    private void addStepToHistory(WorkflowExecution execution, WorkflowMessage message) {
        int currentStepId = message.getCurrentStep();
        Instant now = Instant.now();

        // Mark previous steps as PASSED if they are still IN_PROGRESS or PENDING
        execution.markPreviousStepsAsPassed(currentStepId, now);

        // Find or create step entry for current step
        WorkflowExecution.StepExecution stepExecution = execution.findStepByStepId(currentStepId)
                .orElseGet(() -> {
                    String stepLabel = getStepLabel(message.getTopic(), currentStepId);
                    WorkflowExecution.StepExecution newStep = WorkflowExecution.StepExecution.builder()
                            .stepId(currentStepId)
                            .stepLabel(stepLabel)
                            .startedAt(now)
                            .attempt(1)
                            .build();
                    execution.addStepExecution(newStep);
                    return newStep;
                });

        // Update current step status based on workflow status
        updateStepStatus(stepExecution, message, now);
    }

    private void updateStepStatus(WorkflowExecution.StepExecution stepExecution,
                                  WorkflowMessage message, Instant now) {
        if (message.getStatus() == WorkflowStatus.COMPLETED
                || message.getStatus() == WorkflowStatus.FAILED) {
            stepExecution.setStatus(message.getStatus());
            stepExecution.setCompletedAt(now);
            if (stepExecution.getStartedAt() != null) {
                stepExecution.setDurationMs(now.toEpochMilli() - stepExecution.getStartedAt().toEpochMilli());
            }
        } else {
            stepExecution.setStatus(message.getStatus());
        }

        if (message.getErrorInfo() != null) {
            stepExecution.setErrorMessage(message.getErrorInfo().getMessage());
        }

        if (message.getRetryInfo() != null) {
            stepExecution.setAttempt(message.getRetryInfo().getAttempt());
        }
    }

    /**
     * Get step label from workflow definition.
     */
    private String getStepLabel(String topic, int stepId) {
        if (workflowRegistry == null) {
            return null;
        }

        WorkflowDefinition definition = workflowRegistry.getDefinition(topic);
        if (definition == null) {
            return null;
        }

        return definition.getSteps().stream()
                .filter(s -> s.getId() == stepId)
                .findFirst()
                .map(StepDefinition::getLabel)
                .orElse(null);
    }
}
