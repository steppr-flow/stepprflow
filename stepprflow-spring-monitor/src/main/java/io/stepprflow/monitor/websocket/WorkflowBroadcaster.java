package io.stepprflow.monitor.websocket;

import io.stepprflow.monitor.model.WorkflowExecution;

/**
 * Interface for broadcasting workflow updates.
 * Allows for different implementations (async vs sync) for production and testing.
 */
public interface WorkflowBroadcaster {

    /**
     * Broadcast workflow update to all subscribers.
     * @param execution the workflow execution to broadcast
     */
    void broadcastUpdate(WorkflowExecution execution);

    /**
     * Send notification to specific user.
     * @param userId the user ID
     * @param execution the workflow execution
     */
    void sendToUser(String userId, WorkflowExecution execution);
}
