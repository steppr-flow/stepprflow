package io.stepprflow.monitor.health;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import io.stepprflow.monitor.outbox.OutboxMessageRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the transactional outbox.
 *
 * <p>This indicator monitors the outbox queue status, reporting on pending
 * and failed message counts. It helps identify potential delivery issues
 * or backlogs in message processing.
 *
 * <p>Health states:
 * <ul>
 *   <li>UP - No failed messages and pending count is within threshold</li>
 *   <li>DOWN - Failed messages exist or pending count exceeds threshold</li>
 * </ul>
 *
 * <p>Thresholds can be configured via properties:
 * <ul>
 *   <li>{@code stepprflow.monitor.outbox.health.pending-threshold} - Max pending before DOWN (default: 1000)</li>
 *   <li>{@code stepprflow.monitor.outbox.health.failed-threshold} - Max failed before DOWN (default: 0)</li>
 * </ul>
 */
@Component
@ConditionalOnBean(OutboxMessageRepository.class)
@ConditionalOnProperty(
        prefix = "stepprflow.monitor.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxHealthIndicator implements HealthIndicator {

    private final OutboxMessageRepository outboxRepository;
    private final MonitorProperties properties;

    /**
     * Creates a new outbox health indicator.
     *
     * @param outboxRepository the outbox repository
     * @param properties the monitor properties
     */
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed beans are safely shared via dependency injection")
    public OutboxHealthIndicator(
            OutboxMessageRepository outboxRepository,
            MonitorProperties properties) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
    }

    @Override
    public Health health() {
        try {
            long pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING);
            long sentCount = outboxRepository.countByStatus(OutboxStatus.SENT);
            long failedCount = outboxRepository.countByStatus(OutboxStatus.FAILED);

            Health.Builder builder = Health.up()
                    .withDetail("pending", pendingCount)
                    .withDetail("sent", sentCount)
                    .withDetail("failed", failedCount)
                    .withDetail("enabled", properties.getOutbox().isEnabled());

            // Check failed threshold
            long failedThreshold = getFailedThreshold();
            if (failedCount > failedThreshold) {
                return builder
                        .down()
                        .withDetail("reason", "Failed message count (" + failedCount
                                + ") exceeds threshold (" + failedThreshold + ")")
                        .withDetail("failedThreshold", failedThreshold)
                        .build();
            }

            // Check pending threshold
            long pendingThreshold = getPendingThreshold();
            if (pendingCount > pendingThreshold) {
                return builder
                        .down()
                        .withDetail("reason", "Pending message count (" + pendingCount
                                + ") exceeds threshold (" + pendingThreshold + ")")
                        .withDetail("pendingThreshold", pendingThreshold)
                        .build();
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("enabled", properties.getOutbox().isEnabled())
                    .build();
        }
    }

    private long getPendingThreshold() {
        return properties.getOutbox().getHealth().getPendingThreshold();
    }

    private long getFailedThreshold() {
        return properties.getOutbox().getHealth().getFailedThreshold();
    }
}
