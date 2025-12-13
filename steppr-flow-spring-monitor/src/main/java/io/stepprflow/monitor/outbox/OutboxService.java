package io.stepprflow.monitor.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.outbox.OutboxMessage.MessageType;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for writing messages to the outbox.
 *
 * <p>This service provides methods to enqueue messages for reliable delivery.
 * Messages are written to the outbox collection and will be sent by the
 * {@link OutboxRelayService}.
 */
@Service
@Slf4j
@ConditionalOnProperty(
        prefix = "stepprflow.monitor.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxService {

    private final OutboxMessageRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final MonitorProperties.Outbox config;

    public OutboxService(
            OutboxMessageRepository outboxRepository,
            ObjectMapper objectMapper,
            MonitorProperties properties) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.config = properties.getOutbox();
    }

    /**
     * Enqueue a workflow message for reliable delivery.
     *
     * @param destination the destination topic/queue
     * @param message the workflow message
     * @param messageType the type of message
     * @return the created outbox message ID
     */
    public String enqueue(String destination, WorkflowMessage message, MessageType messageType) {
        try {
            String payload = objectMapper.writeValueAsString(message);

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .destination(destination)
                    .executionId(message.getExecutionId())
                    .messageType(messageType)
                    .payload(payload)
                    .payloadClass(WorkflowMessage.class.getName())
                    .status(OutboxStatus.PENDING)
                    .attempts(0)
                    .maxAttempts(config.getMaxAttempts())
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(outboxMessage);

            log.debug("Enqueued {} message for execution {} to outbox (id={})",
                    messageType, message.getExecutionId(), outboxMessage.getId());

            return outboxMessage.getId();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize workflow message", e);
        }
    }

    /**
     * Enqueue a resume message.
     *
     * @param destination the destination topic
     * @param message the workflow message
     * @return the outbox message ID
     */
    public String enqueueResume(String destination, WorkflowMessage message) {
        return enqueue(destination, message, MessageType.RESUME);
    }

    /**
     * Enqueue a retry message.
     *
     * @param destination the destination topic
     * @param message the workflow message
     * @return the outbox message ID
     */
    public String enqueueRetry(String destination, WorkflowMessage message) {
        return enqueue(destination, message, MessageType.RETRY);
    }

    /**
     * Check if outbox is enabled.
     *
     * @return true if outbox is enabled
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }
}
