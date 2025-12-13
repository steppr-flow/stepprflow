package io.stepprflow.monitor.outbox;

import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for outbox messages.
 */
@Repository
public interface OutboxMessageRepository extends MongoRepository<OutboxMessage, String> {

    /**
     * Find pending messages ready to be sent.
     * Returns messages that are PENDING and have nextRetryAt <= now (or null for new messages).
     *
     * @param status the status to filter by
     * @param now current time for retry check
     * @param pageable pagination info
     * @return list of messages ready to be sent
     */
    @Query("{ 'status': ?0, $or: [ { 'nextRetryAt': null }, { 'nextRetryAt': { $lte: ?1 } } ] }")
    List<OutboxMessage> findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
            OutboxStatus status, Instant now, Pageable pageable);

    /**
     * Find messages by execution ID.
     *
     * @param executionId the execution ID
     * @return list of outbox messages for the execution
     */
    List<OutboxMessage> findByExecutionId(String executionId);

    /**
     * Find messages by status.
     *
     * @param status the status
     * @return list of messages with the given status
     */
    List<OutboxMessage> findByStatus(OutboxStatus status);

    /**
     * Count messages by status.
     *
     * @param status the status
     * @return count of messages
     */
    long countByStatus(OutboxStatus status);

    /**
     * Delete sent messages older than the given timestamp.
     * Used for cleanup of processed messages.
     *
     * @param status the status (should be SENT)
     * @param before delete messages processed before this time
     * @return number of deleted messages
     */
    long deleteByStatusAndProcessedAtBefore(OutboxStatus status, Instant before);

    /**
     * Find failed messages for manual review.
     *
     * @param pageable pagination info
     * @return list of failed messages
     */
    List<OutboxMessage> findByStatusOrderByCreatedAtDesc(OutboxStatus status, Pageable pageable);
}
