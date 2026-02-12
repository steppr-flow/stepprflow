package io.github.stepprflow.broker.kafka;

import io.github.stepprflow.core.broker.MessageBroker;
import io.github.stepprflow.core.exception.MessageSendException;
import io.github.stepprflow.core.model.WorkflowMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Apache Kafka implementation of MessageBroker.
 */
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageBroker implements MessageBroker {

    private final KafkaTemplate<String, WorkflowMessage> kafkaTemplate;

    @Override
    public void send(String destination, WorkflowMessage message) {
        log.debug("Sending message to Kafka topic {}: executionId={}, step={}",
                destination, message.getExecutionId(), message.getCurrentStep());

        CompletableFuture<SendResult<String, WorkflowMessage>> future =
                kafkaTemplate.send(destination, message.getExecutionId(), message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send message to topic {}: {}", destination, ex.getMessage(), ex);
            } else {
                log.debug("Message sent successfully to topic {} partition {} offset {}",
                        destination,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendAsync(String destination, WorkflowMessage message) {
        log.debug("Sending async message to Kafka topic {}: executionId={}",
                destination, message.getExecutionId());

        return kafkaTemplate.send(destination, message.getExecutionId(), message)
                .thenAccept(result -> log.debug("Async message sent to topic {} partition {} offset {}",
                        destination,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()));
    }

    @Override
    public void sendSync(String destination, WorkflowMessage message) {
        log.debug("Sending sync message to Kafka topic {}: executionId={}",
                destination, message.getExecutionId());

        try {
            SendResult<String, WorkflowMessage> result =
                    kafkaTemplate.send(destination, message.getExecutionId(), message).get();
            log.debug("Sync message sent to topic {} partition {} offset {}",
                    destination,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("Failed to send sync message to topic {}: {}", destination, e.getMessage(), e);
            throw new MessageSendException("kafka", destination, message.getExecutionId(),
                    e.getMessage(), e);
        }
    }

    @Override
    public String getBrokerType() {
        return "kafka";
    }

    @Override
    public boolean isAvailable() {
        // Simple check - could be enhanced with actual broker health check
        return kafkaTemplate != null;
    }
}
