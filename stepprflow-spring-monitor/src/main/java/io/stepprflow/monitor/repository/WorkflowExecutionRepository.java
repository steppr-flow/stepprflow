package io.stepprflow.monitor.repository;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB repository for workflow executions.
 */
@Repository
public interface WorkflowExecutionRepository extends MongoRepository<WorkflowExecution, String> {

    /**
     * Find by topic.
     */
    Page<WorkflowExecution> findByTopic(String topic, Pageable pageable);

    /**
     * Find by status.
     */
    Page<WorkflowExecution> findByStatus(WorkflowStatus status, Pageable pageable);

    /**
     * Find by multiple statuses.
     */
    Page<WorkflowExecution> findByStatusIn(List<WorkflowStatus> statuses, Pageable pageable);

    /**
     * Find by topic and status.
     */
    Page<WorkflowExecution> findByTopicAndStatus(String topic, WorkflowStatus status, Pageable pageable);

    /**
     * Find by topic and multiple statuses.
     */
    Page<WorkflowExecution> findByTopicAndStatusIn(String topic, List<WorkflowStatus> statuses, Pageable pageable);

    /**
     * Find executions pending retry.
     */
    @Query("{'status': 'RETRY_PENDING', 'retryInfo.nextRetryAt': {'$lte': ?0}}")
    List<WorkflowExecution> findPendingRetries(Instant now);

    /**
     * Find completed executions older than given date.
     */
    @Query("{'status': 'COMPLETED', 'completedAt': {'$lt': ?0}}")
    List<WorkflowExecution> findCompletedBefore(Instant date);

    /**
     * Find failed executions older than given date.
     */
    @Query("{'status': 'FAILED', 'completedAt': {'$lt': ?0}}")
    List<WorkflowExecution> findFailedBefore(Instant date);

    /**
     * Count by status.
     */
    long countByStatus(WorkflowStatus status);

    /**
     * Count by topic and status.
     */
    long countByTopicAndStatus(String topic, WorkflowStatus status);

    /**
     * Find recent executions.
     */
    List<WorkflowExecution> findTop10ByOrderByCreatedAtDesc();

    /**
     * Find by correlation ID.
     */
    List<WorkflowExecution> findByCorrelationId(String correlationId);
}
