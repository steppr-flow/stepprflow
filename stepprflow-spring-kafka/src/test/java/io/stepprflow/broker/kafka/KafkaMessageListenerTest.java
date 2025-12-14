package io.stepprflow.broker.kafka;

import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.core.service.StepExecutor;
import io.stepprflow.core.service.WorkflowRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaMessageListener Tests")
class KafkaMessageListenerTest {

    @Mock
    private StepExecutor stepExecutor;

    @Mock
    private WorkflowRegistry registry;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private KafkaMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new KafkaMessageListener(stepExecutor, registry, eventPublisher);
    }

    @Nested
    @DisplayName("onMessage()")
    class OnMessageTests {

        @Test
        @DisplayName("Should process PENDING message and acknowledge")
        void shouldProcessPendingMessage() {
            // Given
            WorkflowMessage message = createMessage(WorkflowStatus.PENDING);
            ConsumerRecord<String, WorkflowMessage> record = createRecord(message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor).execute(message);
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should process IN_PROGRESS message and acknowledge")
        void shouldProcessInProgressMessage() {
            // Given
            WorkflowMessage message = createMessage(WorkflowStatus.IN_PROGRESS);
            ConsumerRecord<String, WorkflowMessage> record = createRecord(message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor).execute(message);
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should skip COMPLETED message and acknowledge")
        void shouldSkipCompletedMessage() {
            // Given
            WorkflowMessage message = createMessage(WorkflowStatus.COMPLETED);
            ConsumerRecord<String, WorkflowMessage> record = createRecord(message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should skip FAILED message and acknowledge")
        void shouldSkipFailedMessage() {
            // Given
            WorkflowMessage message = createMessage(WorkflowStatus.FAILED);
            ConsumerRecord<String, WorkflowMessage> record = createRecord(message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should skip CANCELLED message and acknowledge")
        void shouldSkipCancelledMessage() {
            // Given
            WorkflowMessage message = createMessage(WorkflowStatus.CANCELLED);
            ConsumerRecord<String, WorkflowMessage> record = createRecord(message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should acknowledge null message")
        void shouldAcknowledgeNullMessage() {
            // Given
            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "test-topic", 0, 0L, "key", null
            );

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should not acknowledge when executor throws exception")
        void shouldNotAcknowledgeOnException() {
            // Given
            WorkflowMessage message = createMessage(WorkflowStatus.PENDING);
            ConsumerRecord<String, WorkflowMessage> record = createRecord(message);
            doThrow(new RuntimeException("Processing failed")).when(stepExecutor).execute(message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(stepExecutor).execute(message);
            verify(acknowledgment, never()).acknowledge();
        }
    }

    private WorkflowMessage createMessage(WorkflowStatus status) {
        return WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic("test-topic")
                .currentStep(1)
                .totalSteps(3)
                .status(status)
                .payload(Map.of("key", "value"))
                .build();
    }

    private ConsumerRecord<String, WorkflowMessage> createRecord(WorkflowMessage message) {
        return new ConsumerRecord<>(
                message.getTopic(),
                0,
                0L,
                message.getExecutionId(),
                message
        );
    }
}
