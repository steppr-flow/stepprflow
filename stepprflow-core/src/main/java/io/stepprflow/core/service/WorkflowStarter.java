package io.stepprflow.core.service;

import io.stepprflow.core.model.WorkflowMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for starting workflows.
 */
public interface WorkflowStarter {

    /**
     * Start a workflow with the given payload.
     *
     * @param topic   The workflow topic
     * @param payload The payload object
     * @return The execution ID
     */
    String start(String topic, Object payload);

    /**
     * Start a workflow with the given payload and metadata.
     *
     * @param topic    The workflow topic
     * @param payload  The payload object
     * @param metadata Additional metadata
     * @return The execution ID
     */
    String start(String topic, Object payload, Map<String, Object> metadata);

    /**
     * Start a workflow asynchronously.
     *
     * @param topic   The workflow topic
     * @param payload The payload object
     * @return Future containing the execution ID
     */
    CompletableFuture<String> startAsync(String topic, Object payload);

    /**
     * Start a workflow and return the full message.
     *
     * @param topic   The workflow topic
     * @param payload The payload object
     * @return The workflow message
     */
    WorkflowMessage startAndGetMessage(String topic, Object payload);

    /**
     * Resume a paused or failed workflow.
     *
     * @param executionId The execution ID
     * @param stepId      The step to resume from (or null for next step)
     */
    void resume(String executionId, Integer stepId);

    /**
     * Cancel a running workflow.
     *
     * @param executionId The execution ID
     */
    void cancel(String executionId);
}
