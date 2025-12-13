package io.stepprflow.monitor.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service that relays outbox messages to the message broker.
 *
 * <p>This service implements the relay/poller part of the Transactional Outbox pattern.
 * It periodically polls for pending messages and sends them to the broker.
 *
 * <p>Features:
 * <ul>
 *   <li>Configurable polling interval and batch size</li>
 *   <li>Exponential backoff for failed sends</li>
 *   <li>Automatic cleanup of old sent messages</li>
 *   <li>Metrics and logging for observability</li>
 * </ul>
 */
@Service
@Slf4j
@ConditionalOnProperty(
        prefix = "stepprflow.monitor.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxRelayService {

    private final OutboxMessageRepository outboxRepository;
    private final MessageBroker messageBroker;
    private final ObjectMapper objectMapper;
    private final MonitorProperties.Outbox config;

    public OutboxRelayService(
            OutboxMessageRepository outboxRepository,
            MessageBroker messageBroker,
            ObjectMapper objectMapper,
            MonitorProperties properties) {
        this.outboxRepository = outboxRepository;
        this.messageBroker = messageBroker;
        this.objectMapper = objectMapper;
        this.config = properties.getOutbox();
    }

    /**
     * Poll and process pending outbox messages.
     * Runs on a fixed delay configured by stepprflow.monitor.outbox.poll-interval.
     */
    @Scheduled(fixedDelayString = "${stepprflow.monitor.outbox.poll-interval:1000}")
    public void processOutbox() {
        List<OutboxMessage> messages = outboxRepository
                .findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                        OutboxStatus.PENDING,
                        Instant.now(),
                        PageRequest.of(0, config.getBatchSize()));

        if (messages.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox messages", messages.size());

        int sent = 0;
        int failed = 0;

        for (OutboxMessage message : messages) {
            try {
                sendMessage(message);
                message.markAsSent();
                outboxRepository.save(message);
                sent++;
                log.debug("Outbox message {} sent successfully to {}",
                        message.getId(), message.getDestination());
            } catch (Exception e) {
                handleSendFailure(message, e);
                failed++;
            }
        }

        if (sent > 0 || failed > 0) {
            log.info("Outbox relay completed: {} sent, {} failed", sent, failed);
        }
    }

    /**
     * Send a message to the broker.
     */
    private void sendMessage(OutboxMessage outboxMessage) throws Exception {
        WorkflowMessage workflowMessage = objectMapper.readValue(
                outboxMessage.getPayload(), WorkflowMessage.class);

        messageBroker.sendSync(outboxMessage.getDestination(), workflowMessage);
    }

    /**
     * Handle send failure with exponential backoff.
     */
    private void handleSendFailure(OutboxMessage message, Exception e) {
        message.setLastError(e.getMessage());
        message.incrementAttemptWithBackoff(config.getBaseDelayMs(), config.getMaxDelayMs());
        outboxRepository.save(message);

        if (message.getStatus() == OutboxStatus.FAILED) {
            log.error("Outbox message {} failed permanently after {} attempts: {}",
                    message.getId(), message.getAttempts(), e.getMessage());
        } else {
            log.warn("Outbox message {} failed (attempt {}), will retry at {}: {}",
                    message.getId(), message.getAttempts(), message.getNextRetryAt(), e.getMessage());
        }
    }

    /**
     * Clean up old sent messages.
     * Runs on a fixed delay configured by stepprflow.monitor.outbox.cleanup-interval.
     */
    @Scheduled(fixedDelayString = "${stepprflow.monitor.outbox.cleanup-interval:3600000}")
    public void cleanupSentMessages() {
        Instant cutoff = Instant.now().minus(config.getSentRetention());
        long deleted = outboxRepository.deleteByStatusAndProcessedAtBefore(OutboxStatus.SENT, cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} old sent outbox messages", deleted);
        }
    }

    /**
     * Get outbox statistics.
     *
     * @return statistics map
     */
    public OutboxStats getStats() {
        return new OutboxStats(
                outboxRepository.countByStatus(OutboxStatus.PENDING),
                outboxRepository.countByStatus(OutboxStatus.SENT),
                outboxRepository.countByStatus(OutboxStatus.FAILED)
        );
    }

    /**
     * Outbox statistics record.
     *
     * @param pending number of pending messages
     * @param sent number of sent messages
     * @param failed number of failed messages
     */
    public record OutboxStats(long pending, long sent, long failed) {}
}
