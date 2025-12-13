package io.stepprflow.monitor.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Outbox message for reliable message delivery.
 *
 * <p>The Transactional Outbox pattern ensures that database changes and
 * message broker sends are eventually consistent. Messages are first
 * written to this collection in the same transaction as business data,
 * then a background relay process sends them to the broker.
 *
 * <p>Message lifecycle:
 * <ol>
 *   <li>PENDING - Message created, waiting to be sent</li>
 *   <li>SENT - Message successfully sent to broker</li>
 *   <li>FAILED - Message failed after max retries (requires manual intervention)</li>
 * </ol>
 */
@Document(collection = "outbox_messages")
@CompoundIndex(name = "status_createdAt", def = "{'status': 1, 'createdAt': 1}")
@CompoundIndex(name = "status_nextRetryAt", def = "{'status': 1, 'nextRetryAt': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessage {

    @Id
    private String id;

    /**
     * The destination topic/queue for the message.
     */
    @Indexed
    private String destination;

    /**
     * The workflow execution ID (for correlation).
     */
    @Indexed
    private String executionId;

    /**
     * The message type (e.g., "RESUME", "RETRY").
     */
    private MessageType messageType;

    /**
     * The serialized message payload (JSON).
     */
    private String payload;

    /**
     * The fully qualified class name of the payload.
     */
    private String payloadClass;

    /**
     * Current status of the outbox message.
     */
    @Indexed
    private OutboxStatus status;

    /**
     * Number of send attempts.
     */
    private int attempts;

    /**
     * Maximum number of send attempts before marking as FAILED.
     */
    private int maxAttempts;

    /**
     * When the message was created.
     */
    @Indexed
    private Instant createdAt;

    /**
     * When the message was last processed.
     */
    private Instant processedAt;

    /**
     * When to retry next (for exponential backoff).
     */
    @Indexed
    private Instant nextRetryAt;

    /**
     * Last error message if send failed.
     */
    private String lastError;

    /**
     * Outbox message status.
     */
    public enum OutboxStatus {
        /** Waiting to be sent */
        PENDING,
        /** Successfully sent to broker */
        SENT,
        /** Failed after max retries */
        FAILED
    }

    /**
     * Type of message being sent.
     */
    public enum MessageType {
        /** Resume a failed/paused workflow */
        RESUME,
        /** Automatic retry of a failed step */
        RETRY,
        /** Generic workflow message */
        WORKFLOW
    }

    /**
     * Increment attempt counter and update next retry time with exponential backoff.
     *
     * @param baseDelayMs base delay in milliseconds
     * @param maxDelayMs maximum delay in milliseconds
     */
    public void incrementAttemptWithBackoff(long baseDelayMs, long maxDelayMs) {
        this.attempts++;
        this.processedAt = Instant.now();

        if (this.attempts >= this.maxAttempts) {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            // Exponential backoff: baseDelay * 2^(attempts-1), capped at maxDelay
            long delayMs = Math.min(baseDelayMs * (1L << (this.attempts - 1)), maxDelayMs);
            this.nextRetryAt = Instant.now().plusMillis(delayMs);
        }
    }

    /**
     * Mark the message as successfully sent.
     */
    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.processedAt = Instant.now();
        this.nextRetryAt = null;
    }
}
