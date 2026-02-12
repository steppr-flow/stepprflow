package io.github.stepprflow.dashboard.listener;

import io.github.stepprflow.core.event.WorkflowMessageEvent;
import io.github.stepprflow.core.model.WorkflowMessage;
import io.github.stepprflow.core.model.WorkflowRegistrationRequest;
import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.monitor.service.RegistrationMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for MonitoringKafkaListener.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringKafkaListener Tests")
class MonitoringKafkaListenerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RegistrationMessageHandler registrationHandler;

    @Mock
    private Acknowledgment acknowledgment;

    @Captor
    private ArgumentCaptor<WorkflowMessageEvent> eventCaptor;

    private MonitoringKafkaListener listener;

    @BeforeEach
    void setUp() {
        listener = new MonitoringKafkaListener(eventPublisher, registrationHandler);
    }

    @Nested
    @DisplayName("onMessage()")
    class OnMessageTests {

        @Test
        @DisplayName("should publish event and acknowledge when message is valid")
        void shouldPublishEventAndAcknowledgeWhenMessageIsValid() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-123")
                    .topic("test-workflow")
                    .currentStep(1)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "test-workflow", 0, 100L, "key", message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verify(acknowledgment).acknowledge();

            WorkflowMessageEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getMessage()).isEqualTo(message);
            assertThat(capturedEvent.getMessage().getExecutionId()).isEqualTo("exec-123");
        }

        @Test
        @DisplayName("should acknowledge and skip when message is null")
        void shouldAcknowledgeAndSkipWhenMessageIsNull() {
            // Given
            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "test-workflow", 0, 100L, "key", null);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(acknowledgment).acknowledge();
            verifyNoMoreInteractions(eventPublisher);
        }

        @Test
        @DisplayName("should handle message with all fields populated")
        void shouldHandleMessageWithAllFieldsPopulated() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-456")
                    .correlationId("corr-789")
                    .topic("order-workflow")
                    .currentStep(3)
                    .totalSteps(5)
                    .status(WorkflowStatus.COMPLETED)
                    .payload("{\"orderId\": 123}")
                    .payloadType("Order")
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "order-workflow", 1, 200L, "order-key", message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verify(acknowledgment).acknowledge();

            WorkflowMessageEvent capturedEvent = eventCaptor.getValue();
            WorkflowMessage capturedMessage = capturedEvent.getMessage();
            assertThat(capturedMessage.getExecutionId()).isEqualTo("exec-456");
            assertThat(capturedMessage.getCorrelationId()).isEqualTo("corr-789");
            assertThat(capturedMessage.getTopic()).isEqualTo("order-workflow");
            assertThat(capturedMessage.getCurrentStep()).isEqualTo(3);
            assertThat(capturedMessage.getTotalSteps()).isEqualTo(5);
            assertThat(capturedMessage.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        }

        @Test
        @DisplayName("should handle message with PENDING status")
        void shouldHandleMessageWithPendingStatus() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-pending")
                    .topic("pending-workflow")
                    .currentStep(0)
                    .status(WorkflowStatus.PENDING)
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "pending-workflow", 0, 0L, null, message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verify(acknowledgment).acknowledge();

            assertThat(eventCaptor.getValue().getMessage().getStatus())
                    .isEqualTo(WorkflowStatus.PENDING);
        }

        @Test
        @DisplayName("should handle message with FAILED status")
        void shouldHandleMessageWithFailedStatus() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-failed")
                    .topic("failed-workflow")
                    .currentStep(2)
                    .status(WorkflowStatus.FAILED)
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "failed-workflow", 0, 50L, "failed-key", message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verify(acknowledgment).acknowledge();

            assertThat(eventCaptor.getValue().getMessage().getStatus())
                    .isEqualTo(WorkflowStatus.FAILED);
        }

        @Test
        @DisplayName("should handle message with RETRY_PENDING status")
        void shouldHandleMessageWithRetryPendingStatus() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-retry")
                    .topic("retry-workflow")
                    .currentStep(1)
                    .status(WorkflowStatus.RETRY_PENDING)
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "retry-workflow", 2, 75L, "retry-key", message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            verify(acknowledgment).acknowledge();

            assertThat(eventCaptor.getValue().getMessage().getStatus())
                    .isEqualTo(WorkflowStatus.RETRY_PENDING);
        }

        @Test
        @DisplayName("should use listener as event source")
        void shouldUseListenerAsEventSource() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-source")
                    .topic("source-workflow")
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    "source-workflow", 0, 0L, null, message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            assertThat(eventCaptor.getValue().getSource()).isEqualTo(listener);
        }

        @Test
        @DisplayName("should delegate registration messages to handler")
        void shouldDelegateRegistrationMessages() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-reg")
                    .topic(WorkflowRegistrationRequest.REGISTRATION_TOPIC)
                    .serviceName("test-service")
                    .status(WorkflowStatus.COMPLETED)
                    .metadata(Map.of(
                            WorkflowRegistrationRequest.METADATA_ACTION,
                            WorkflowRegistrationRequest.ACTION_HEARTBEAT))
                    .build();

            ConsumerRecord<String, WorkflowMessage> record = new ConsumerRecord<>(
                    WorkflowRegistrationRequest.REGISTRATION_TOPIC, 0, 0L, null, message);

            // When
            listener.onMessage(record, acknowledgment);

            // Then
            verify(registrationHandler).handle(message);
            verify(acknowledgment).acknowledge();
            verifyNoInteractions(eventPublisher);
        }
    }
}