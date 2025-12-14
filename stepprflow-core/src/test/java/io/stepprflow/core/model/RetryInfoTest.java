package io.stepprflow.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RetryInfo Tests")
class RetryInfoTest {

    @Nested
    @DisplayName("Builder and defaults")
    class BuilderTests {

        @Test
        @DisplayName("Should have default attempt of 1")
        void shouldHaveDefaultAttemptOfOne() {
            RetryInfo retryInfo = RetryInfo.builder().build();

            assertThat(retryInfo.getAttempt()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should build with all fields")
        void shouldBuildWithAllFields() {
            Instant nextRetry = Instant.now().plusSeconds(60);

            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(5)
                    .nextRetryAt(nextRetry)
                    .lastError("Connection timeout")
                    .build();

            assertThat(retryInfo.getAttempt()).isEqualTo(2);
            assertThat(retryInfo.getMaxAttempts()).isEqualTo(5);
            assertThat(retryInfo.getNextRetryAt()).isEqualTo(nextRetry);
            assertThat(retryInfo.getLastError()).isEqualTo("Connection timeout");
        }
    }

    @Nested
    @DisplayName("nextAttempt() method")
    class NextAttemptTests {

        @Test
        @DisplayName("Should increment attempt number")
        void shouldIncrementAttemptNumber() {
            RetryInfo original = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .build();

            Instant nextRetry = Instant.now().plusSeconds(30);
            RetryInfo next = original.nextAttempt(nextRetry, "First failure");

            assertThat(next.getAttempt()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should preserve maxAttempts")
        void shouldPreserveMaxAttempts() {
            RetryInfo original = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(5)
                    .build();

            RetryInfo next = original.nextAttempt(Instant.now(), "Error");

            assertThat(next.getMaxAttempts()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should set new nextRetryAt")
        void shouldSetNewNextRetryAt() {
            RetryInfo original = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .nextRetryAt(Instant.parse("2024-01-01T10:00:00Z"))
                    .build();

            Instant newRetryAt = Instant.parse("2024-01-01T10:01:00Z");
            RetryInfo next = original.nextAttempt(newRetryAt, "Error");

            assertThat(next.getNextRetryAt()).isEqualTo(newRetryAt);
        }

        @Test
        @DisplayName("Should set new lastError")
        void shouldSetNewLastError() {
            RetryInfo original = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .lastError("Previous error")
                    .build();

            RetryInfo next = original.nextAttempt(Instant.now(), "New error message");

            assertThat(next.getLastError()).isEqualTo("New error message");
        }

        @Test
        @DisplayName("Should chain multiple retries")
        void shouldChainMultipleRetries() {
            RetryInfo retry1 = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(5)
                    .build();

            RetryInfo retry2 = retry1.nextAttempt(Instant.now(), "Error 1");
            RetryInfo retry3 = retry2.nextAttempt(Instant.now(), "Error 2");
            RetryInfo retry4 = retry3.nextAttempt(Instant.now(), "Error 3");

            assertThat(retry4.getAttempt()).isEqualTo(4);
            assertThat(retry4.getLastError()).isEqualTo("Error 3");
        }
    }

    @Nested
    @DisplayName("isExhausted() method")
    class IsExhaustedTests {

        @Test
        @DisplayName("Should return false when attempts remaining")
        void shouldReturnFalseWhenAttemptsRemaining() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .build();

            assertThat(retryInfo.isExhausted()).isFalse();
        }

        @Test
        @DisplayName("Should return true when attempt equals maxAttempts")
        void shouldReturnTrueWhenAttemptEqualsMax() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .build();

            assertThat(retryInfo.isExhausted()).isTrue();
        }

        @Test
        @DisplayName("Should return true when attempt exceeds maxAttempts")
        void shouldReturnTrueWhenAttemptExceedsMax() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(5)
                    .maxAttempts(3)
                    .build();

            assertThat(retryInfo.isExhausted()).isTrue();
        }

        @Test
        @DisplayName("Should return false just before exhaustion")
        void shouldReturnFalseJustBeforeExhaustion() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(3)
                    .build();

            assertThat(retryInfo.isExhausted()).isFalse();
        }

        @Test
        @DisplayName("Should handle single attempt max")
        void shouldHandleSingleAttemptMax() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(1)
                    .build();

            assertThat(retryInfo.isExhausted()).isTrue();
        }

        @Test
        @DisplayName("Should handle zero maxAttempts")
        void shouldHandleZeroMaxAttempts() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(0)
                    .build();

            assertThat(retryInfo.isExhausted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null lastError")
        void shouldHandleNullLastError() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .build();

            RetryInfo next = retryInfo.nextAttempt(Instant.now(), null);

            assertThat(next.getLastError()).isNull();
        }

        @Test
        @DisplayName("Should handle null nextRetryAt")
        void shouldHandleNullNextRetryAt() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .build();

            RetryInfo next = retryInfo.nextAttempt(null, "Error");

            assertThat(next.getNextRetryAt()).isNull();
        }

        @Test
        @DisplayName("Should be immutable - original unchanged after nextAttempt")
        void shouldBeImmutable() {
            RetryInfo original = RetryInfo.builder()
                    .attempt(1)
                    .maxAttempts(3)
                    .lastError("Original error")
                    .build();

            original.nextAttempt(Instant.now(), "New error");

            assertThat(original.getAttempt()).isEqualTo(1);
            assertThat(original.getLastError()).isEqualTo("Original error");
        }
    }
}