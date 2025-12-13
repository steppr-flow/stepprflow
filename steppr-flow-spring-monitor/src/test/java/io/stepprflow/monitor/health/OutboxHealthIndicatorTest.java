package io.stepprflow.monitor.health;

import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import io.stepprflow.monitor.outbox.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OutboxHealthIndicator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxHealthIndicator Tests")
class OutboxHealthIndicatorTest {

    @Mock
    private OutboxMessageRepository outboxRepository;

    private MonitorProperties properties;
    private OutboxHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new MonitorProperties();
        properties.getOutbox().setEnabled(true);
        properties.getOutbox().getHealth().setPendingThreshold(1000);
        properties.getOutbox().getHealth().setFailedThreshold(0);

        healthIndicator = new OutboxHealthIndicator(outboxRepository, properties);
    }

    @Nested
    @DisplayName("When outbox is healthy")
    class WhenOutboxHealthy {

        @Test
        @DisplayName("Should return UP status with no pending or failed messages")
        void shouldReturnUpWithNoMessages() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(0L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("pending", 0L);
            assertThat(health.getDetails()).containsEntry("sent", 100L);
            assertThat(health.getDetails()).containsEntry("failed", 0L);
            assertThat(health.getDetails()).containsEntry("enabled", true);
        }

        @Test
        @DisplayName("Should return UP status with pending below threshold")
        void shouldReturnUpWithPendingBelowThreshold() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(500L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(1000L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsEntry("pending", 500L);
        }
    }

    @Nested
    @DisplayName("When pending threshold exceeded")
    class WhenPendingThresholdExceeded {

        @Test
        @DisplayName("Should return DOWN status")
        void shouldReturnDownStatus() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(1500L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("pending", 1500L);
            assertThat(health.getDetails()).containsKey("reason");
            assertThat(health.getDetails()).containsEntry("pendingThreshold", 1000L);
        }

        @Test
        @DisplayName("Should include reason with counts")
        void shouldIncludeReasonWithCounts() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(2000L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(0L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            Health health = healthIndicator.health();

            assertThat(health.getDetails().get("reason").toString())
                    .contains("2000")
                    .contains("1000");
        }
    }

    @Nested
    @DisplayName("When failed threshold exceeded")
    class WhenFailedThresholdExceeded {

        @Test
        @DisplayName("Should return DOWN status with any failed messages (default threshold 0)")
        void shouldReturnDownWithAnyFailed() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(1L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("failed", 1L);
            assertThat(health.getDetails()).containsKey("reason");
            assertThat(health.getDetails()).containsEntry("failedThreshold", 0L);
        }

        @Test
        @DisplayName("Should allow failed messages up to threshold")
        void shouldAllowFailedUpToThreshold() {
            properties.getOutbox().getHealth().setFailedThreshold(5);
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(3L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }

        @Test
        @DisplayName("Should return DOWN when failed exceeds custom threshold")
        void shouldReturnDownWhenExceedsCustomThreshold() {
            properties.getOutbox().getHealth().setFailedThreshold(5);
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(10L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails().get("reason").toString())
                    .contains("10")
                    .contains("5");
        }
    }

    @Nested
    @DisplayName("When repository throws exception")
    class WhenRepositoryThrowsException {

        @Test
        @DisplayName("Should return DOWN status with error")
        void shouldReturnDownWithError() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING))
                    .thenThrow(new RuntimeException("Database error"));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("error", "Database error");
            assertThat(health.getDetails()).containsEntry("enabled", true);
        }
    }

    @Nested
    @DisplayName("With custom thresholds")
    class WithCustomThresholds {

        @Test
        @DisplayName("Should use custom pending threshold")
        void shouldUseCustomPendingThreshold() {
            properties.getOutbox().getHealth().setPendingThreshold(100);
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(150L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(0L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsEntry("pendingThreshold", 100L);
        }

        @Test
        @DisplayName("Should use custom failed threshold")
        void shouldUseCustomFailedThreshold() {
            properties.getOutbox().getHealth().setFailedThreshold(10);
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(50L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(15L);

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails().get("reason").toString())
                    .contains("15")
                    .contains("10");
        }
    }
}
