package io.github.stepprflow.monitor.outbox;

import io.github.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
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
        @DisplayName("Should use correct backoff formula: baseDelay * 2^(attempts-1)")
        void shouldUseCorrectBackoffFormula() {
            OutboxMessage message = OutboxMessage.builder()
                    .attempts(0)
                    .maxAttempts(10)
                    .status(OutboxStatus.PENDING)
                    .build();

            long baseDelayMs = 1000;
            Instant before = Instant.now();

            // First call: attempts becomes 1, delay = 1000 * 2^(1-1) = 1000 * 1 = 1000ms
            message.incrementAttemptWithBackoff(baseDelayMs, 60000);

            Instant nextRetry = message.getNextRetryAt();
            // The delay should be approximately 1000ms (baseDelay * 2^0 = baseDelay * 1)
            // If mutation changes -1 to +1, it would be 4000ms (baseDelay * 2^2)
            assertThat(nextRetry).isAfterOrEqualTo(before.plusMillis(baseDelayMs));
            assertThat(nextRetry).isBefore(before.plusMillis(baseDelayMs + 1500)); // Allow 1.5s tolerance

            // Second call: attempts becomes 2, delay = 1000 * 2^(2-1) = 1000 * 2 = 2000ms
            Instant before2 = Instant.now();
            message.incrementAttemptWithBackoff(baseDelayMs, 60000);

            Instant nextRetry2 = message.getNextRetryAt();
            assertThat(nextRetry2).isAfterOrEqualTo(before2.plusMillis(2000));
            assertThat(nextRetry2).isBefore(before2.plusMillis(2000 + 1500));
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
    @DisplayName("No-args constructor")
    class NoArgsConstructorTests {

        @Test
        @DisplayName("Should support no-args constructor")
        void shouldSupportNoArgsConstructor() {
            OutboxMessage message = new OutboxMessage();
            message.setId("test-id");
            message.setDestination("dest");

            assertThat(message.getId()).isEqualTo("test-id");
            assertThat(message.getDestination()).isEqualTo("dest");
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
