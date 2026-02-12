package io.github.stepprflow.monitor.service;

import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.core.util.NestedPathResolver;
import io.github.stepprflow.monitor.exception.ConcurrentModificationException;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import io.github.stepprflow.monitor.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for payload management operations.
 * Handles payload field updates and restoration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayloadManagementService {

    private final WorkflowExecutionRepository repository;
    private final NestedPathResolver pathResolver;

    /**
     * Update a specific field in the payload with history tracking.
     * Only allowed for failed, paused, or retry pending executions.
     */
    @SuppressWarnings("unchecked")
    public WorkflowExecution updatePayloadField(String executionId, String fieldPath,
            Object newValue, String changedBy, String reason) {
        WorkflowExecution execution = repository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        validateEditableStatus(execution, "update");

        // Get the current payload as a Map (use LinkedHashMap to preserve key order)
        Map<String, Object> payloadMap;
        if (execution.getPayload() instanceof Map) {
            payloadMap = new LinkedHashMap<>((Map<String, Object>) execution.getPayload());
        } else {
            throw new IllegalStateException("Payload must be a Map to update individual fields");
        }

        // Get old value and update
        Object oldValue = pathResolver.getValue(payloadMap, fieldPath);
        pathResolver.setValue(payloadMap, fieldPath, newValue);

        // Create change record
        WorkflowExecution.PayloadChange change = WorkflowExecution.PayloadChange.builder()
                .fieldPath(fieldPath)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(Instant.now())
                .changedBy(changedBy)
                .reason(reason)
                .build();

        // Add change to payload history
        execution.addPayloadChange(change);

        execution.setPayload(payloadMap);
        execution.setUpdatedAt(Instant.now());

        try {
            repository.save(execution);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected while updating payload field for workflow {}", executionId);
            throw new ConcurrentModificationException(executionId, e);
        }

        log.info("Updated payload field '{}' for workflow {} by {}", fieldPath, executionId, changedBy);
        return execution;
    }

    /**
     * Restore payload to its original state by reverting all pending changes.
     */
    @SuppressWarnings("unchecked")
    public WorkflowExecution restorePayload(String executionId) {
        WorkflowExecution execution = repository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

        validateEditableStatus(execution, "restore");

        List<WorkflowExecution.PayloadChange> pendingChanges = execution.getPayloadHistory();
        if (pendingChanges == null || pendingChanges.isEmpty()) {
            return execution;
        }

        // Revert changes in reverse order
        Map<String, Object> payloadMap = (Map<String, Object>) execution.getPayload();
        for (int i = pendingChanges.size() - 1; i >= 0; i--) {
            WorkflowExecution.PayloadChange change = pendingChanges.get(i);
            pathResolver.setValue(payloadMap, change.getFieldPath(), change.getOldValue());
        }

        // Clear pending changes
        execution.setPayloadHistory(new ArrayList<>());
        execution.setPayload(payloadMap);
        execution.setUpdatedAt(Instant.now());

        try {
            repository.save(execution);
        } catch (OptimisticLockingFailureException e) {
            log.warn("Concurrent modification detected while restoring payload for workflow {}", executionId);
            throw new ConcurrentModificationException(executionId, e);
        }

        log.info("Restored payload for workflow {}", executionId);
        return execution;
    }

    /**
     * Validate that execution is in an editable status.
     */
    private void validateEditableStatus(WorkflowExecution execution, String operation) {
        if (execution.getStatus() != WorkflowStatus.FAILED &&
            execution.getStatus() != WorkflowStatus.PAUSED &&
            execution.getStatus() != WorkflowStatus.RETRY_PENDING) {
            throw new IllegalStateException(
                    "Cannot " + operation + " payload for execution with status: " + execution.getStatus());
        }
    }
}
