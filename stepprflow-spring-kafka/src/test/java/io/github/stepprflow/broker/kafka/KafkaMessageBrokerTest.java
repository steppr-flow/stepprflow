package io.github.stepprflow.broker.kafka;

import io.github.stepprflow.core.model.WorkflowMessage;
import io.github.stepprflow.core.model.WorkflowStatus;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.stepprflow.core.exception.MessageSendException;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMessageBroker Tests")
class KafkaMessageBrokerTest {

    @Mock
    private KafkaTemplate<String, WorkflowMessage> kafkaTemplate;

    private KafkaMessageBroker messageBroker;
    private WorkflowMessage testMessage;

    @BeforeEach
    void setUp() {
        messageBroker = new KafkaMessageBroker(kafkaTemplate);
        testMessage = WorkflowMessage.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .currentStep(1)
                .totalSteps(3)
                .status(WorkflowStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("isAvailable() method")
    class IsAvailableTests {

        @Test
        @DisplayName("Should return true when kafkaTemplate is not null")
        void shouldReturnTrueWhenKafkaTemplateNotNull() {
            boolean result = messageBroker.isAvailable();

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when kafkaTemplate is null")
        void shouldReturnFalseWhenKafkaTemplateIsNull() {
            KafkaMessageBroker brokerWithNull = new KafkaMessageBroker(null);

            boolean result = brokerWithNull.isAvailable();

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getBrokerType() method")
    class GetBrokerTypeTests {

        @Test
        @DisplayName("Should return 'kafka'")
        void shouldReturnKafka() {
            String result = messageBroker.getBrokerType();

            assertThat(result).isEqualTo("kafka");
        }
    }

    @Nested
    @DisplayName("send() method")
    class SendTests {

        @Test
        @DisplayName("Should send message successfully")
        void shouldSendMessageSuccessfully() {
            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0);
            SendResult<String, WorkflowMessage> sendResult = new SendResult<>(
                    new ProducerRecord<>("test-topic", testMessage), metadata);
            CompletableFuture<SendResult<String, WorkflowMessage>> future =
                    CompletableFuture.completedFuture(sendResult);
            when(kafkaTemplate.send(eq("test-topic"), eq("exec-123"), any(WorkflowMessage.class)))
                    .thenReturn(future);

            // Should not throw
            messageBroker.send("test-topic", testMessage);

            // Give async callback time to complete
            future.join();
        }

        @Test
        @DisplayName("Should handle send failure in callback")
        void shouldHandleSendFailureInCallback() {
            CompletableFuture<SendResult<String, WorkflowMessage>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
            when(kafkaTemplate.send(eq("test-topic"), eq("exec-123"), any(WorkflowMessage.class)))
                    .thenReturn(future);

            // Should not throw - error is handled in callback
            messageBroker.send("test-topic", testMessage);

            // Wait for callback to complete
            try {
                future.join();
            } catch (Exception ignored) {
                // Expected - future completed exceptionally
            }
        }
    }

    @Nested
    @DisplayName("sendAsync() method")
    class SendAsyncTests {

        @Test
        @DisplayName("Should send message asynchronously and return completed future")
        void shouldSendMessageAsynchronously() throws Exception {
            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0);
            SendResult<String, WorkflowMessage> sendResult = new SendResult<>(
                    new ProducerRecord<>("test-topic", testMessage), metadata);
            CompletableFuture<SendResult<String, WorkflowMessage>> future =
                    CompletableFuture.completedFuture(sendResult);
            when(kafkaTemplate.send(eq("test-topic"), eq("exec-123"), any(WorkflowMessage.class)))
                    .thenReturn(future);

            CompletableFuture<Void> result = messageBroker.sendAsync("test-topic", testMessage);

            assertThat(result).isCompleted();
        }

        @Test
        @DisplayName("Should propagate failure in returned future")
        void shouldPropagateFailureInFuture() {
            CompletableFuture<SendResult<String, WorkflowMessage>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
            when(kafkaTemplate.send(eq("test-topic"), eq("exec-123"), any(WorkflowMessage.class)))
                    .thenReturn(future);

            CompletableFuture<Void> result = messageBroker.sendAsync("test-topic", testMessage);

            assertThat(result).isCompletedExceptionally();
        }
    }

    @Nested
    @DisplayName("sendSync() method")
    class SendSyncTests {

        @Test
        @DisplayName("Should send message synchronously")
        void shouldSendMessageSynchronously() throws Exception {
            RecordMetadata metadata = new RecordMetadata(
                    new TopicPartition("test-topic", 0), 0, 0, 0, 0, 0);
            SendResult<String, WorkflowMessage> sendResult = new SendResult<>(
                    new ProducerRecord<>("test-topic", testMessage), metadata);
            CompletableFuture<SendResult<String, WorkflowMessage>> future =
                    CompletableFuture.completedFuture(sendResult);
            when(kafkaTemplate.send(eq("test-topic"), eq("exec-123"), any(WorkflowMessage.class)))
                    .thenReturn(future);

            messageBroker.sendSync("test-topic", testMessage);
        }

        @Test
        @DisplayName("Should throw MessageSendException on failure")
        void shouldThrowMessageSendExceptionOnFailure() {
            CompletableFuture<SendResult<String, WorkflowMessage>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
            when(kafkaTemplate.send(eq("test-topic"), eq("exec-123"), any(WorkflowMessage.class)))
                    .thenReturn(future);

            assertThatThrownBy(() -> messageBroker.sendSync("test-topic", testMessage))
                    .isInstanceOf(MessageSendException.class);
        }
    }
}