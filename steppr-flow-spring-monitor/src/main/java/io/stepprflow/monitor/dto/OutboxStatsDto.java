package io.stepprflow.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for outbox statistics response.
 * Used by the UI to display outbox queue status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxStatsDto {

    /**
     * Number of messages pending to be sent.
     */
    private long pending;

    /**
     * Number of messages successfully sent.
     */
    private long sent;

    /**
     * Number of messages that failed permanently.
     */
    private long failed;

    /**
     * Total number of messages in the outbox.
     */
    private long total;

    /**
     * Percentage of messages successfully sent.
     */
    private double sendRate;

    /**
     * Health status: UP, WARNING, or DOWN.
     * UP = no failed, pending below threshold
     * WARNING = pending above threshold but no failed
     * DOWN = has failed messages
     */
    private String health;
}
