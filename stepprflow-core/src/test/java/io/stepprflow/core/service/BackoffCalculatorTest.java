package io.stepprflow.core.service;

import io.stepprflow.core.StepprFlowProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BackoffCalculator Tests")
class BackoffCalculatorTest {

    @Mock
    private StepprFlowProperties properties;

    private BackoffCalculator calculator;
    private StepprFlowProperties.Retry retryConfig;

    @BeforeEach
    void setUp() {
        calculator = new BackoffCalculator(properties);

        retryConfig = new StepprFlowProperties.Retry();
        retryConfig.setInitialDelay(Duration.ofSeconds(1));
        retryConfig.setMultiplier(2.0);
        retryConfig.setMaxDelay(Duration.ofMinutes(5));

        when(properties.getRetry()).thenReturn(retryConfig);
    }

    @Nested
    @DisplayName("calculate() method")
    class CalculateTests {

        @Test
        @DisplayName("Should return initial delay for first attempt")
        void shouldReturnInitialDelayForFirstAttempt() {
            Duration backoff = calculator.calculate(1);

            assertThat(backoff).isEqualTo(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should apply exponential multiplier for second attempt")
        void shouldApplyMultiplierForSecondAttempt() {
            Duration backoff = calculator.calculate(2);

            // 1s * 2^1 = 2s
            assertThat(backoff).isEqualTo(Duration.ofSeconds(2));
        }

        @Test
        @DisplayName("Should apply exponential multiplier for third attempt")
        void shouldApplyMultiplierForThirdAttempt() {
            Duration backoff = calculator.calculate(3);

            // 1s * 2^2 = 4s
            assertThat(backoff).isEqualTo(Duration.ofSeconds(4));
        }

        @Test
        @DisplayName("Should apply exponential multiplier for fourth attempt")
        void shouldApplyMultiplierForFourthAttempt() {
            Duration backoff = calculator.calculate(4);

            // 1s * 2^3 = 8s
            assertThat(backoff).isEqualTo(Duration.ofSeconds(8));
        }

        @Test
        @DisplayName("Should not exceed max delay")
        void shouldNotExceedMaxDelay() {
            Duration backoff = calculator.calculate(100);

            assertThat(backoff).isEqualTo(Duration.ofMinutes(5));
        }

        @Test
        @DisplayName("Should cap at max delay when calculated value exceeds it")
        void shouldCapAtMaxDelay() {
            // 1s * 2^9 = 512s > 300s (5 min)
            Duration backoff = calculator.calculate(10);

            assertThat(backoff).isEqualTo(Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("With different configurations")
    class ConfigurationTests {

        @Test
        @DisplayName("Should work with custom initial delay")
        void shouldWorkWithCustomInitialDelay() {
            retryConfig.setInitialDelay(Duration.ofMillis(500));

            Duration backoff = calculator.calculate(1);

            assertThat(backoff).isEqualTo(Duration.ofMillis(500));
        }

        @Test
        @DisplayName("Should work with custom multiplier")
        void shouldWorkWithCustomMultiplier() {
            retryConfig.setMultiplier(3.0);

            Duration backoff = calculator.calculate(2);

            // 1s * 3^1 = 3s
            assertThat(backoff).isEqualTo(Duration.ofSeconds(3));
        }

        @Test
        @DisplayName("Should work with multiplier of 1 (no exponential growth)")
        void shouldWorkWithMultiplierOfOne() {
            retryConfig.setMultiplier(1.0);

            Duration backoff1 = calculator.calculate(1);
            Duration backoff2 = calculator.calculate(2);
            Duration backoff3 = calculator.calculate(3);

            assertThat(backoff1).isEqualTo(Duration.ofSeconds(1));
            assertThat(backoff2).isEqualTo(Duration.ofSeconds(1));
            assertThat(backoff3).isEqualTo(Duration.ofSeconds(1));
        }

        @Test
        @DisplayName("Should work with small max delay")
        void shouldWorkWithSmallMaxDelay() {
            retryConfig.setMaxDelay(Duration.ofSeconds(3));

            Duration backoff = calculator.calculate(5);

            // Would be 1s * 2^4 = 16s, but capped at 3s
            assertThat(backoff).isEqualTo(Duration.ofSeconds(3));
        }
    }
}
