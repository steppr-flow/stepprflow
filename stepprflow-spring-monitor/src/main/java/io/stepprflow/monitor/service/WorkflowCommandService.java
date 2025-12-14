package io.stepprflow.monitor.service;

import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.exception.MessageSendException;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.exception.ConcurrentModificationException;
import io.stepprflow.monitor.exception.WorkflowResumeException;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.outbox.OutboxService;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import io.stepprflow.monitor.util.WorkflowMessageFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for workflow command operations.
 * Handles state-changing operations like resume and cancel.
 *
 * <p>When the Transactional Outbox is enabled, messages are written to the outbox
 * collection in the same transaction as execution updates, ensuring consistency.
 * A background relay process then sends messages to the broker.
 */
@Service
@Slf4j
public class WorkflowCommandService {

    private final WorkflowExecutionRepository repository;
    private final MessageBroker messageBroker;
    private final WorkflowMessageFactory messageFactory;
    private final OutboxService outboxService;

    @Autowired
    public WorkflowCommandService(
            WorkflowExecutionRepository repository,
            MessageBroker messageBroker,
            WorkflowMessageFactory messageFactory,
            @Autowired(required = false) OutboxService outboxService) {
        this.repository = repository;
        this.messageBroker = messageBroker;
        this.messageFactory = messageFactory;
        this.outboxService = outboxService;
    }

    /**
     * Resume a failed or paused workflow.
     *
     * <p>When Transactional Outbox is enabled:
     * <ol>
     *   <li>Validate execution status</li>
     *   <li>Create and persist a new execution attempt</li>
     *   <li>Write resume message to outbox (same transaction)</li>
     *   <li>Background relay sends to broker</li>
     * </ol>
     *
     * <p>When Outbox is disabled (fallback):
     * <ol>
     *   <li>Validate execution status</li>
     *   <li>Create and persist a new execution attempt</li>
     *   <li>Send the resume message to the broker synchronously</li>
     * </ol>
     *
     * @param executionId the workflow execution ID
     * @param fromStep optional step to resume from (defaults to current step)
     * @param resumedBy identifier of who initiated the resume
     * @throws IllegalArgumentException if execution not found
     * @throws IllegalStateException if execution status doesn't allow resume
     * @throws ConcurrentModificationException if concurrent modification detected
     * @throws WorkflowResumeException if broker send fails (only when outbox disabled)
     */
    public void resume(String executionId, Integer fromStep, String resumedBy) {
        WorkflowExecution execution = repository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() != WorkflowStatus.FAILED &&
            execution.getStatus() != WorkflowStatus.PAUSED &&
            execution.getStatus() != WorkflowStatus.RETRY_PENDING) {
            throw new IllegalStateException("Cannot resume execution with status: " + execution.getStatus());
        }

        int step = fromStep != null ? fromStep : execution.getCurrentStep();

        // Create a new execution attempt (persisted to DB)
        int attemptNumber = createExecutionAttempt(execution, step, resumedBy);

        // Create the resume message
        WorkflowMessage message = messageFactory.createResumeMessage(execution, step);

        log.info("Resuming workflow {} from step {} (attempt {})", executionId, step, attemptNumber);

        if (isOutboxEnabled()) {
            // Use Transactional Outbox for reliable delivery
            String outboxId = outboxService.enqueueResume(execution.getTopic(), message);
            log.info("Resume message enqueued to outbox (id={}) for workflow {}", outboxId, executionId);
        } else {
            // Fallback to direct send
            sendDirectly(executionId, attemptNumber, execution.getTopic(), message);
        }
    }

    /**
     * Check if outbox is enabled and available.
     */
    private boolean isOutboxEnabled() {
        return outboxService != null && outboxService.isEnabled();
    }

    /**
     * Send message directly to broker (fallback when outbox is disabled).
     */
    private void sendDirectly(String executionId, int attemptNumber, String topic, WorkflowMessage message) {
        try {
            messageBroker.sendSync(topic, message);
            log.info("Resume message sent successfully for workflow {}", executionId);
        } catch (MessageSendException e) {
            log.error("Failed to send resume message for workflow {} (attempt {}). " +
                      "Execution attempt is persisted. Retry the resume operation.",
                      executionId, attemptNumber, e);
            throw new WorkflowResumeException(executionId, attemptNumber, messageBroker.getBrokerType(), e);
        }
    }

    /**
     * Cancel a running workflow.
     *
     * @param executionId the workflow execution ID
     * @throws IllegalArgumentException if execution not found
     * @throws IllegalStateException if execution is already completed or cancelled
     * @throws ConcurrentModificationException if concurrent modification detected
     */
    public void cancel(String executionId) {
        WorkflowExecution execution = repository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        if (execution.getStatus() == WorkflowStatus.COMPLETED ||
            execution.getStatus() == WorkflowStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel execution with status: " + execution.getStatus());
        }

        execution.setStatus(WorkflowStatus.CANCELLED);
        execution.setUpdatedAt(Instant.now());
        execution.setCompletedAt(Instant.now());

        try {
            repository.save(execution);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected while cancelling workflow {}", executionId);
            throw new ConcurrentModificationException(executionId, e);
        }

        log.info("Cancelled workflow {}", executionId);
    }

    /**
     * Create a new execution attempt, moving pending payload changes to this attempt.
     *
     * @param execution the workflow execution
     * @param startStep the step to start from
     * @param resumedBy identifier of who initiated the resume
     * @return the attempt number
     * @throws ConcurrentModificationException if concurrent modification detected
     */
    private int createExecutionAttempt(WorkflowExecution execution, int startStep, String resumedBy) {
        // Determine attempt number
        int attemptNumber = execution.getNextAttemptNumber();

        // Get pending payload changes and clear them
        List<WorkflowExecution.PayloadChange> pendingChanges = execution.getPayloadHistory();
        execution.setPayloadHistory(new ArrayList<>());

        // Create the attempt
        WorkflowExecution.ExecutionAttempt attempt = WorkflowExecution.ExecutionAttempt.builder()
                .attemptNumber(attemptNumber)
                .startedAt(Instant.now())
                .startStep(startStep)
                .resumedBy(attemptNumber > 1 ? resumedBy : null)
                .payloadChanges(!pendingChanges.isEmpty() ? pendingChanges : null)
                .build();

        execution.addExecutionAttempt(attempt);
        execution.setUpdatedAt(Instant.now());

        try {
            repository.save(execution);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected while creating execution attempt for workflow {}",
                    execution.getExecutionId());
            throw new ConcurrentModificationException(execution.getExecutionId(), e);
        }

        log.info("Created execution attempt {} for workflow {}", attemptNumber, execution.getExecutionId());
        return attemptNumber;
    }
}
