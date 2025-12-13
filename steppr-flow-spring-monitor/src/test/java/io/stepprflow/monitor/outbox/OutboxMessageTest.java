package io.stepprflow.monitor.outbox;

import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OutboxMessage.
 */
@DisplayName("OutboxMessage Tests")
class OutboxMessageTest {

    @Nested
    @DisplayName("incrementAttemptWithBackoff()")
    class IncrementAttemptWithBackoff {

        @Test
        @DisplayName("Should increment attempt count")
        void shouldIncrementAttemptCount() {
            OutboxMessage message = OutboxMessage.builder()
                    .attempts(0)
                    .maxAttempts(5)
                    .status(OutboxStatus.PENDING)
                    .build();

            message.incrementAttemptWithBackoff(1000, 60000);

            assertThat(message.getAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should set processedAt")
        void shouldSetProcessedAt() {
            OutboxMessage message = OutboxMessage.builder()
                    .attempts(0)
                    .maxAttempts(5)
                    .status(OutboxStatus.PENDING)
                    .build();

            Instant before = Instant.now();
            message.incrementAttemptWithBackoff(1000, 60000);

            assertThat(message.getProcessedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("Should calculate exponential backoff for next retry")
        void shouldCalculateExponentialBackoff() {
            OutboxMessage message = OutboxMessage.builder()
                    .attempts(0)
                    .maxAttempts(5)
                    .status(OutboxStatus.PENDING)
                    .build();

            // First attempt: 1000ms delay
            message.incrementAttemptWithBackoff(1000, 60000);
            Instant firstRetry = message.getNextRetryAt();

            // Second attempt: 2000ms delay
            message.incrementAttemptWithBackoff(1000, 60000);
            Instant secondRetry = message.getNextRetryAt();

            // Third attempt: 4000ms delay
            message.incrementAttemptWithBackoff(1000, 60000);
            Instant thirdRetry = message.getNextRetryAt();

            assertThat(secondRetry).isAfter(firstRetry);
            assertThat(thirdRetry).isAfter(secondRetry);
        }

        @Test
        @DisplayName("Should cap delay at maxDelay")
        void shouldCapDelayAtMaxDelay() {
            OutboxMessage message = OutboxMessage.builder()
                    .attempts(10)  // High attempt count
                    .maxAttempts(20)
                    .status(OutboxStatus.PENDING)
                    .build();

            Instant before = Instant.now();
            message.incrementAttemptWithBackoff(1000, 5000);  // Max 5 seconds

            // Should not exceed 5 seconds from now
            assertThat(message.getNextRetryAt())
                    .isBefore(before.plusMillis(6000));
        }

        @Test
        @DisplayName("Should mark as FAILED when max attempts reached")
        void shouldMarkAsFailedWhenMaxAttemptsReached() {
            OutboxMessage message = OutboxMessage.builder()
                    .attempts(4)
                    .maxAttempts(5)
                    .status(OutboxStatus.PENDING)
                    .build();

            message.incrementAttemptWithBackoff(1000, 60000);

            assertThat(message.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(message.getNextRetryAt()).isNull();
        }
    }

    @Nested
    @DisplayName("markAsSent()")
    class MarkAsSent {

        @Test
        @DisplayName("Should set status to SENT")
        void shouldSetStatusToSent() {
            OutboxMessage message = OutboxMessage.builder()
                    .status(OutboxStatus.PENDING)
                    .build();

            message.markAsSent();

            assertThat(message.getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        @Test
        @DisplayName("Should set processedAt")
        void shouldSetProcessedAt() {
            OutboxMessage message = OutboxMessage.builder()
                    .status(OutboxStatus.PENDING)
                    .build();

            Instant before = Instant.now();
            message.markAsSent();

            assertThat(message.getProcessedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("Should clear nextRetryAt")
        void shouldClearNextRetryAt() {
            OutboxMessage message = OutboxMessage.builder()
                    .status(OutboxStatus.PENDING)
                    .nextRetryAt(Instant.now().plusSeconds(60))
                    .build();

            message.markAsSent();

            assertThat(message.getNextRetryAt()).isNull();
        }
    }
}
