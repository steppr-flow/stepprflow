package io.stepprflow.broker.rabbitmq;

import com.rabbitmq.client.Channel;
import io.stepprflow.core.event.WorkflowMessageEvent;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.core.service.StepExecutor;
import io.stepprflow.core.service.WorkflowRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQMessageListener Tests")
class RabbitMQMessageListenerTest {

    @Mock
    private StepExecutor stepExecutor;

    @Mock
    private WorkflowRegistry registry;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Channel channel;

    private RabbitMQMessageListener listener;
    private MessageProperties messageProperties;

    @BeforeEach
    void setUp() {
        listener = new RabbitMQMessageListener(stepExecutor, registry, messageConverter, eventPublisher);
        messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(1L);
        messageProperties.setConsumerQueue("test-queue");
    }

    @Nested
    @DisplayName("onMessage()")
    class OnMessageTests {

        @Test
        @DisplayName("Should process PENDING message and acknowledge")
        void shouldProcessPendingMessage() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.PENDING);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor).execute(workflowMessage);
            verify(channel).basicAck(1L, false);
        }

        @Test
        @DisplayName("Should publish WorkflowMessageEvent for monitoring")
        void shouldPublishWorkflowMessageEvent() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.IN_PROGRESS);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            ArgumentCaptor<WorkflowMessageEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowMessageEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getMessage()).isEqualTo(workflowMessage);
        }

        @Test
        @DisplayName("Should process IN_PROGRESS message and acknowledge")
        void shouldProcessInProgressMessage() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.IN_PROGRESS);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor).execute(workflowMessage);
            verify(channel).basicAck(1L, false);
        }

        @Test
        @DisplayName("Should skip COMPLETED message and acknowledge")
        void shouldSkipCompletedMessage() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.COMPLETED);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(channel).basicAck(1L, false);
        }

        @Test
        @DisplayName("Should skip FAILED message and acknowledge")
        void shouldSkipFailedMessage() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.FAILED);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(channel).basicAck(1L, false);
        }

        @Test
        @DisplayName("Should skip CANCELLED message and acknowledge")
        void shouldSkipCancelledMessage() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.CANCELLED);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(channel).basicAck(1L, false);
        }

        @Test
        @DisplayName("Should reject and requeue when executor throws exception")
        void shouldRejectAndRequeueOnExecutorException() throws IOException {
            // Given
            WorkflowMessage workflowMessage = createMessage(WorkflowStatus.PENDING);
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenReturn(workflowMessage);
            doThrow(new RuntimeException("Processing failed")).when(stepExecutor).execute(workflowMessage);

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor).execute(workflowMessage);
            verify(channel).basicReject(1L, true);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("Should reject without requeue when deserialization fails")
        void shouldRejectWithoutRequeueOnDeserializationError() throws IOException {
            // Given
            Message message = createAmqpMessage();
            when(messageConverter.fromMessage(message)).thenThrow(new RuntimeException("Invalid JSON"));

            // When
            listener.onMessage(message, channel);

            // Then
            verify(stepExecutor, never()).execute(any());
            verify(channel).basicReject(1L, false);
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

    private Message createAmqpMessage() {
        return new Message("test body".getBytes(), messageProperties);
    }
}