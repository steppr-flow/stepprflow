package io.stepprflow.core.service;

import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;

import java.util.List;
import java.util.Optional;

/**
 * Interface for storing and retrieving workflow execution state.
 *
 * <p>This interface abstracts the persistence layer, allowing different
 * implementations (in-memory, MongoDB, Redis, etc.).
 *
 * <p>The monitor module provides a MongoDB implementation of this interface.
 */
public interface WorkflowExecutionStore {

    /**
     * Find a workflow execution by its execution ID.
     *
     * @param executionId the unique execution ID
     * @return the workflow message if found
     */
    Optional<WorkflowMessage> findByExecutionId(String executionId);

    /**
     * Save or update a workflow execution.
     *
     * @param message the workflow message to save
     */
    void save(WorkflowMessage message);

    /**
     * Find workflow executions by status.
     *
     * @param status the status to filter by
     * @return list of matching workflow messages
     */
    List<WorkflowMessage> findByStatus(WorkflowStatus status);

    /**
     * Find workflow executions by topic.
     *
     * @param topic the topic to filter by
     * @return list of matching workflow messages
     */
    List<WorkflowMessage> findByTopic(String topic);

    /**
     * Find workflow executions by topic and status.
     *
     * @param topic  the topic to filter by
     * @param status the status to filter by
     * @return list of matching workflow messages
     */
    List<WorkflowMessage> findByTopicAndStatus(String topic, WorkflowStatus status);

    /**
     * Delete a workflow execution.
     *
     * @param executionId the execution ID to delete
     * @return true if the execution was deleted, false if not found
     */
    boolean delete(String executionId);

    /**
     * Check if an execution exists.
     *
     * @param executionId the execution ID to check
     * @return true if the execution exists
     */
    default boolean exists(final String executionId) {
        return findByExecutionId(executionId).isPresent();
    }
}
