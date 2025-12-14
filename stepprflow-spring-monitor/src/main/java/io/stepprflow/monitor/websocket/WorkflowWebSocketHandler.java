package io.stepprflow.monitor.websocket;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.model.WorkflowExecution;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Handles WebSocket communication for workflow updates.
 * Broadcasts execution updates to connected clients via STOMP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "stepprflow.monitor.web-socket",
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowWebSocketHandler implements WorkflowBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final MonitorProperties properties;

    /** Only broadcast meaningful status changes */
    private static final Set<WorkflowStatus> BROADCAST_STATUSES = Set.of(
            WorkflowStatus.PENDING,
            WorkflowStatus.IN_PROGRESS,
            WorkflowStatus.COMPLETED,
            WorkflowStatus.FAILED,
            WorkflowStatus.CANCELLED
    );

    /**
     * Broadcast workflow update to all subscribers.
     * Runs asynchronously to avoid blocking persistence.
     */
    @Override
    @Async
    public void broadcastUpdate(WorkflowExecution execution) {
        // Skip non-meaningful status changes
        if (!BROADCAST_STATUSES.contains(execution.getStatus())) {
            return;
        }

        String topicPrefix = properties.getWebSocket().getTopicPrefix();

        // Use lightweight DTO to reduce payload size
        WorkflowUpdateDTO update = WorkflowUpdateDTO.from(execution);

        try {
            // Broadcast to general topic
            messagingTemplate.convertAndSend(topicPrefix + "/updates", update);

            // Broadcast to topic-specific channel
            messagingTemplate.convertAndSend(topicPrefix + "/" + execution.getTopic(), update);

            // Only broadcast to execution-specific channel for terminal states
            if (isTerminalStatus(execution.getStatus())) {
                messagingTemplate.convertAndSend(
                        topicPrefix + "/execution/" + execution.getExecutionId(),
                        update);
            }

            log.debug("Broadcast workflow update: executionId={}, status={}",
                    execution.getExecutionId(), execution.getStatus());
        } catch (Exception e) {
            log.error("Error broadcasting update for {}: {}",
                    execution.getExecutionId(), e.getMessage());
        }
    }

    private boolean isTerminalStatus(WorkflowStatus status) {
        return status == WorkflowStatus.COMPLETED ||
               status == WorkflowStatus.FAILED ||
               status == WorkflowStatus.CANCELLED;
    }

    /**
     * Send notification to specific user.
     */
    @Override
    public void sendToUser(String userId, WorkflowExecution execution) {
        messagingTemplate.convertAndSendToUser(
                userId,
                properties.getWebSocket().getTopicPrefix() + "/updates",
                WorkflowUpdateDTO.from(execution));
    }

    /**
     * Lightweight DTO for WebSocket broadcasts - excludes large payload data.
     */
    @Data
    @Builder
    public static class WorkflowUpdateDTO {
        private String executionId;
        private String topic;
        private WorkflowStatus status;
        private int currentStep;
        private int totalSteps;
        private Instant updatedAt;
        private Long durationMs;

        public static WorkflowUpdateDTO from(WorkflowExecution execution) {
            return WorkflowUpdateDTO.builder()
                    .executionId(execution.getExecutionId())
                    .topic(execution.getTopic())
                    .status(execution.getStatus())
                    .currentStep(execution.getCurrentStep())
                    .totalSteps(execution.getTotalSteps())
                    .updatedAt(execution.getUpdatedAt())
                    .durationMs(execution.getDurationMs())
                    .build();
        }
    }
}
