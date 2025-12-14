package io.stepprflow.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Retry information for workflow steps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryInfo {

    /**
     * Current attempt number (starts at 1).
     */
    @Builder.Default
    private int attempt = 1;

    /**
     * Maximum number of attempts.
     */
    private int maxAttempts;

    /**
     * Next retry scheduled time.
     */
    private Instant nextRetryAt;

    /**
     * Last error message.
     */
    private String lastError;

    /**
     * Create next retry info.
     *
     * @param nextRetry the next retry time
     * @param error the last error message
     * @return the next retry info
     */
    public RetryInfo nextAttempt(final Instant nextRetry, final String error) {
        return RetryInfo.builder()
                .attempt(this.attempt + 1)
                .maxAttempts(this.maxAttempts)
                .nextRetryAt(nextRetry)
                .lastError(error)
                .build();
    }

    /**
     * Check if retry is exhausted.
     *
     * @return true if all retry attempts have been used
     */
    public boolean isExhausted() {
        return attempt >= maxAttempts;
    }
}
