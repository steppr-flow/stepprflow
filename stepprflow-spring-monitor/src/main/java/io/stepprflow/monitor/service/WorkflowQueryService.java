package io.stepprflow.monitor.service;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for workflow query operations.
 * Handles all read-only operations for workflow executions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowQueryService {

    private final WorkflowExecutionRepository repository;

    /**
     * Get execution by ID.
     */
    public Optional<WorkflowExecution> getExecution(String executionId) {
        return repository.findById(executionId);
    }

    /**
     * Find executions with filtering.
     */
    public Page<WorkflowExecution> findExecutions(String topic, List<WorkflowStatus> statuses, Pageable pageable) {
        if (topic != null && statuses != null && !statuses.isEmpty()) {
            return repository.findByTopicAndStatusIn(topic, statuses, pageable);
        } else if (topic != null) {
            return repository.findByTopic(topic, pageable);
        } else if (statuses != null && !statuses.isEmpty()) {
            return repository.findByStatusIn(statuses, pageable);
        }
        return repository.findAll(pageable);
    }

    /**
     * Get dashboard statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("pending", repository.countByStatus(WorkflowStatus.PENDING));
        stats.put("inProgress", repository.countByStatus(WorkflowStatus.IN_PROGRESS));
        stats.put("completed", repository.countByStatus(WorkflowStatus.COMPLETED));
        stats.put("failed", repository.countByStatus(WorkflowStatus.FAILED));
        stats.put("retryPending", repository.countByStatus(WorkflowStatus.RETRY_PENDING));
        stats.put("cancelled", repository.countByStatus(WorkflowStatus.CANCELLED));
        stats.put("total", repository.count());

        return stats;
    }

    /**
     * Get recent executions.
     */
    public List<WorkflowExecution> getRecentExecutions() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}
