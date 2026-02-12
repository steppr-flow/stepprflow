package io.github.stepprflow.monitor.service;

import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import io.github.stepprflow.monitor.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

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
    private final MongoTemplate mongoTemplate;

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

    /**
     * Get distinct topics from all executions.
     */
    @SuppressWarnings("unchecked")
    public List<String> getDistinctTopics() {
        return mongoTemplate.findDistinct(new Query(), "topic", WorkflowExecution.class, String.class);
    }

    /**
     * Get summary info for a given topic derived from executions.
     */
    public Map<String, Object> getTopicSummary(String topic) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("topic", topic);
        long total = repository.countByTopicAndStatus(topic, WorkflowStatus.COMPLETED)
                + repository.countByTopicAndStatus(topic, WorkflowStatus.FAILED)
                + repository.countByTopicAndStatus(topic, WorkflowStatus.IN_PROGRESS)
                + repository.countByTopicAndStatus(topic, WorkflowStatus.PENDING)
                + repository.countByTopicAndStatus(topic, WorkflowStatus.RETRY_PENDING)
                + repository.countByTopicAndStatus(topic, WorkflowStatus.CANCELLED);
        summary.put("total", total);
        summary.put("completed", repository.countByTopicAndStatus(topic, WorkflowStatus.COMPLETED));
        summary.put("failed", repository.countByTopicAndStatus(topic, WorkflowStatus.FAILED));
        summary.put("inProgress", repository.countByTopicAndStatus(topic, WorkflowStatus.IN_PROGRESS));

        // Get step info from the most recent completed execution, or fallback to most recent
        Page<WorkflowExecution> completed = repository.findByTopicAndStatus(topic,
                WorkflowStatus.COMPLETED,
                PageRequest.of(0, 1,
                        org.springframework.data.domain.Sort.by(Direction.DESC, "createdAt")));
        WorkflowExecution ref = completed.hasContent() ? completed.getContent().get(0) : null;
        if (ref == null) {
            Page<WorkflowExecution> recent = repository.findByTopic(topic,
                    PageRequest.of(0, 1,
                            org.springframework.data.domain.Sort.by(Direction.DESC, "createdAt")));
            ref = recent.hasContent() ? recent.getContent().get(0) : null;
        }
        if (ref != null) {
            summary.put("totalSteps", ref.getTotalSteps());
            List<Map<String, Object>> steps = ref.getStepHistory().stream()
                    .sorted((a, b) -> Integer.compare(a.getStepId(), b.getStepId()))
                    .map(s -> {
                        Map<String, Object> step = new HashMap<>();
                        step.put("id", s.getStepId());
                        step.put("label", s.getStepLabel() != null
                                ? s.getStepLabel() : "Step " + s.getStepId());
                        return step;
                    })
                    .toList();
            summary.put("steps", steps);
        }
        return summary;
    }
}
