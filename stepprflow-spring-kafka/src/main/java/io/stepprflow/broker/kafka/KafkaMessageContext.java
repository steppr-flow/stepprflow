package io.stepprflow.broker.kafka;

import io.stepprflow.core.broker.MessageContext;
import lombok.Builder;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka implementation of MessageContext.
 */
@Getter
@Builder
public class KafkaMessageContext implements MessageContext {

    private final String destination;
    private final String messageKey;
    private final long offset;
    private final int partition;
    private final Map<String, String> headers;
    private final Acknowledgment acknowledgment;

    /**
     * Create a context from a Kafka ConsumerRecord.
     */
    public static KafkaMessageContext from(ConsumerRecord<String, ?> record, Acknowledgment ack) {
        Map<String, String> headers = new HashMap<>();
        record.headers().forEach(header ->
                headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8)));

        return KafkaMessageContext.builder()
                .destination(record.topic())
                .messageKey(record.key())
                .offset(record.offset())
                .partition(record.partition())
                .headers(headers)
                .acknowledgment(ack)
                .build();
    }

    @Override
    public String getOffset() {
        return String.valueOf(offset);
    }

    @Override
    public void acknowledge() {
        if (acknowledgment != null) {
            acknowledgment.acknowledge();
        }
    }

    @Override
    public void reject(boolean requeue) {
        // In Kafka, rejection typically means not committing the offset
        // The message will be redelivered based on consumer group settings
        // For explicit rejection, we just don't acknowledge
        if (!requeue && acknowledgment != null) {
            acknowledgment.acknowledge(); // Commit to skip the message
        }
        // If requeue=true, don't acknowledge - Kafka will redeliver
    }
}
