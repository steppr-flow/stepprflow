package io.stepprflow.monitor.service;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade service for workflow monitoring operations.
 * Delegates to specialized services for specific responsibilities.
 *
 * @deprecated This class is maintained for backward compatibility.
 *             Prefer using the specialized services directly:
 *             - {@link WorkflowQueryService} for read operations
 *             - {@link WorkflowCommandService} for state changes
 *             - {@link PayloadManagementService} for payload updates
 */
@Deprecated
@RequiredArgsConstructor
@Slf4j
public class WorkflowMonitorService {

    private final WorkflowQueryService queryService;
    private final WorkflowCommandService commandService;
    private final PayloadManagementService payloadService;

    /**
     * Get execution by ID.
     * @deprecated Use {@link WorkflowQueryService#getExecution(String)} instead.
     */
    @Deprecated
    public Optional<WorkflowExecution> getExecution(String executionId) {
        return queryService.getExecution(executionId);
    }

    /**
     * Find executions with filtering.
     * @deprecated Use {@link WorkflowQueryService#findExecutions(String, List, Pageable)} instead.
     */
    @Deprecated
    public Page<WorkflowExecution> findExecutions(String topic, List<WorkflowStatus> statuses, Pageable pageable) {
        return queryService.findExecutions(topic, statuses, pageable);
    }

    /**
     * Resume a failed or paused workflow.
     * @deprecated Use {@link WorkflowCommandService#resume(String, Integer, String)} instead.
     */
    @Deprecated
    public void resume(String executionId, Integer fromStep) {
        commandService.resume(executionId, fromStep, "UI User");
    }

    /**
     * Cancel a running workflow.
     * @deprecated Use {@link WorkflowCommandService#cancel(String)} instead.
     */
    @Deprecated
    public void cancel(String executionId) {
        commandService.cancel(executionId);
    }

    /**
     * Get dashboard statistics.
     * @deprecated Use {@link WorkflowQueryService#getStatistics()} instead.
     */
    @Deprecated
    public Map<String, Object> getStatistics() {
        return queryService.getStatistics();
    }

    /**
     * Get recent executions.
     * @deprecated Use {@link WorkflowQueryService#getRecentExecutions()} instead.
     */
    @Deprecated
    public List<WorkflowExecution> getRecentExecutions() {
        return queryService.getRecentExecutions();
    }

    /**
     * Update a specific field in the payload with history tracking.
     * @deprecated Use PayloadManagementService#updatePayloadField instead.
     */
    @Deprecated
    public WorkflowExecution updatePayloadField(String executionId, String fieldPath,
            Object newValue, String changedBy, String reason) {
        return payloadService.updatePayloadField(
                executionId, fieldPath, newValue, changedBy, reason);
    }

    /**
     * Restore payload to its original state.
     * @deprecated Use {@link PayloadManagementService#restorePayload(String)} instead.
     */
    @Deprecated
    public WorkflowExecution restorePayload(String executionId) {
        return payloadService.restorePayload(executionId);
    }
}
