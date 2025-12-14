package io.stepprflow.core.service;

import io.stepprflow.core.StepprFlowProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Calculates exponential backoff delays for retry attempts.
 */
@Component
@RequiredArgsConstructor
public class BackoffCalculator {

    /** The steppr-flow properties. */
    private final StepprFlowProperties properties;

    /**
     * Calculate the backoff duration for a retry attempt.
     * Uses exponential backoff: initialDelay * multiplier^(attempt-1)
     * The result is capped at maxDelay.
     *
     * @param attempt the current attempt number (1-based)
     * @return the backoff duration
     */
    public Duration calculate(final int attempt) {
        StepprFlowProperties.Retry retryConfig = properties.getRetry();
        long initialMs = retryConfig.getInitialDelay().toMillis();
        double multiplier = retryConfig.getMultiplier();
        long maxMs = retryConfig.getMaxDelay().toMillis();

        long delayMs = (long) (initialMs * Math.pow(multiplier, attempt - 1));
        delayMs = Math.min(delayMs, maxMs);

        return Duration.ofMillis(delayMs);
    }
}
