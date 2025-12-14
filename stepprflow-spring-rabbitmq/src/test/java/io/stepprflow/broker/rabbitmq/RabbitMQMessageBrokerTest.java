package io.stepprflow.broker.rabbitmq;

import io.stepprflow.core.exception.MessageSendException;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for RabbitMQMessageBroker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQMessageBroker Tests")
class RabbitMQMessageBrokerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private ConnectionFactory connectionFactory;

    @Mock
    private Connection connection;

    @Mock
    private Message amqpMessage;

    @Captor
    private ArgumentCaptor<MessageProperties> propertiesCaptor;

    private RabbitMQMessageBroker broker;
    private static final String EXCHANGE = "steppr-flow-exchange";

    @BeforeEach
    void setUp() {
        broker = new RabbitMQMessageBroker(rabbitTemplate, EXCHANGE, messageConverter);
    }

    @Nested
    @DisplayName("send()")
    class SendTests {

        @Test
        @DisplayName("should send message to exchange with routing key")
        void shouldSendMessageToExchangeWithRoutingKey() {
            // Given
            WorkflowMessage message = createWorkflowMessage("exec-123", "order-workflow");
            when(messageConverter.toMessage(eq(message), any(MessageProperties.class))).thenReturn(amqpMessage);

            // When
            broker.send("order-workflow", message);

            // Then
            verify(rabbitTemplate).send(eq(EXCHANGE), eq("order-workflow"), eq(amqpMessage));
        }

        @Test
        @DisplayName("should set message properties correctly")
        void shouldSetMessagePropertiesCorrectly() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-456")
                    .correlationId("corr-789")
                    .topic("payment-workflow")
                    .currentStep(2)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();
            when(messageConverter.toMessage(eq(message), propertiesCaptor.capture())).thenReturn(amqpMessage);

            // When
            broker.send("payment-workflow", message);

            // Then
            MessageProperties props = propertiesCaptor.getValue();
            assertThat(props.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
            assertThat(props.getMessageId()).isEqualTo("exec-456");
            assertThat(props.getCorrelationId()).isEqualTo("corr-789");
            assertThat((String) props.getHeader("x-workflow-topic")).isEqualTo("payment-workflow");
            assertThat((Integer) props.getHeader("x-workflow-step")).isEqualTo(2);
            assertThat((String) props.getHeader("x-workflow-status")).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("should include metadata as headers")
        void shouldIncludeMetadataAsHeaders() {
            // Given
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("customKey", "customValue");
            metadata.put("numericKey", 42);

            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-meta")
                    .topic("meta-workflow")
                    .currentStep(1)
                    .status(WorkflowStatus.PENDING)
                    .metadata(metadata)
                    .build();
            when(messageConverter.toMessage(eq(message), propertiesCaptor.capture())).thenReturn(amqpMessage);

            // When
            broker.send("meta-workflow", message);

            // Then
            MessageProperties props = propertiesCaptor.getValue();
            assertThat((String) props.getHeader("customKey")).isEqualTo("customValue");
            assertThat((String) props.getHeader("numericKey")).isEqualTo("42");
        }

        @Test
        @DisplayName("should skip null metadata values")
        void shouldSkipNullMetadataValues() {
            // Given
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("validKey", "validValue");
            metadata.put("nullKey", null);

            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("exec-null-meta")
                    .topic("null-meta-workflow")
                    .currentStep(1)
                    .status(WorkflowStatus.PENDING)
                    .metadata(metadata)
                    .build();
            when(messageConverter.toMessage(eq(message), propertiesCaptor.capture())).thenReturn(amqpMessage);

            // When
            broker.send("null-meta-workflow", message);

            // Then
            MessageProperties props = propertiesCaptor.getValue();
            assertThat((String) props.getHeader("validKey")).isEqualTo("validValue");
            assertThat(props.getHeaders()).doesNotContainKey("nullKey");
        }

        @Test
        @DisplayName("should throw MessageSendException on failure")
        void shouldThrowMessageSendExceptionOnFailure() {
            // Given
            WorkflowMessage message = createWorkflowMessage("exec-error", "error-workflow");
            when(messageConverter.toMessage(eq(message), any(MessageProperties.class))).thenReturn(amqpMessage);
            doThrow(new RuntimeException("Connection failed")).when(rabbitTemplate)
                    .send(anyString(), anyString(), any(Message.class));

            // When/Then
            assertThatThrownBy(() -> broker.send("error-workflow", message))
                    .isInstanceOf(MessageSendException.class)
                    .hasMessageContaining("error-workflow")
                    .extracting("brokerType", "topic")
                    .containsExactly("rabbitmq", "error-workflow");
        }
    }

    @Nested
    @DisplayName("sendAsync()")
    class SendAsyncTests {

        @Test
        @DisplayName("should send message asynchronously")
        void shouldSendMessageAsynchronously() throws ExecutionException, InterruptedException {
            // Given
            WorkflowMessage message = createWorkflowMessage("exec-async", "async-workflow");
            when(messageConverter.toMessage(eq(message), any(MessageProperties.class))).thenReturn(amqpMessage);

            // When
            CompletableFuture<Void> future = broker.sendAsync("async-workflow", message);
            future.get(); // Wait for completion

            // Then
            verify(rabbitTemplate).send(eq(EXCHANGE), eq("async-workflow"), eq(amqpMessage));
        }

        @Test
        @DisplayName("should complete exceptionally on failure")
        void shouldCompleteExceptionallyOnFailure() {
            // Given
            WorkflowMessage message = createWorkflowMessage("exec-async-error", "async-error-workflow");
            when(messageConverter.toMessage(eq(message), any(MessageProperties.class))).thenReturn(amqpMessage);
            doThrow(new RuntimeException("Async send failed")).when(rabbitTemplate)
                    .send(anyString(), anyString(), any(Message.class));

            // When
            CompletableFuture<Void> future = broker.sendAsync("async-error-workflow", message);

            // Then
            assertThatThrownBy(future::get)
                    .hasCauseInstanceOf(MessageSendException.class);
        }
    }

    @Nested
    @DisplayName("sendSync()")
    class SendSyncTests {

        @Test
        @DisplayName("should send message with confirmation")
        void shouldSendMessageWithConfirmation() {
            // Given
            WorkflowMessage message = createWorkflowMessage("exec-sync", "sync-workflow");
            when(messageConverter.toMessage(eq(message), any(MessageProperties.class))).thenReturn(amqpMessage);
            doAnswer(invocation -> {
                // Simulate callback execution - the actual operations are mocked
                return null;
            }).when(rabbitTemplate).invoke(any());

            // When
            broker.sendSync("sync-workflow", message);

            // Then
            verify(rabbitTemplate).invoke(any());
        }

        @Test
        @DisplayName("should throw MessageSendException on sync failure")
        void shouldThrowMessageSendExceptionOnSyncFailure() {
            // Given
            WorkflowMessage message = createWorkflowMessage("exec-sync-error", "sync-error-workflow");
            when(messageConverter.toMessage(eq(message), any(MessageProperties.class))).thenReturn(amqpMessage);
            doThrow(new RuntimeException("Sync send failed")).when(rabbitTemplate).invoke(any());

            // When/Then
            assertThatThrownBy(() -> broker.sendSync("sync-error-workflow", message))
                    .isInstanceOf(MessageSendException.class)
                    .hasMessageContaining("sync-error-workflow");
        }
    }

    @Nested
    @DisplayName("getBrokerType()")
    class GetBrokerTypeTests {

        @Test
        @DisplayName("should return rabbitmq")
        void shouldReturnRabbitmq() {
            assertThat(broker.getBrokerType()).isEqualTo("rabbitmq");
        }
    }

    @Nested
    @DisplayName("isAvailable()")
    class IsAvailableTests {

        @Test
        @DisplayName("should return true when connection is open")
        void shouldReturnTrueWhenConnectionIsOpen() {
            // Given
            when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.createConnection()).thenReturn(connection);
            when(connection.isOpen()).thenReturn(true);

            // When
            boolean available = broker.isAvailable();

            // Then
            assertThat(available).isTrue();
            verify(connection).close();
        }

        @Test
        @DisplayName("should return false when connection is closed")
        void shouldReturnFalseWhenConnectionIsClosed() {
            // Given
            when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.createConnection()).thenReturn(connection);
            when(connection.isOpen()).thenReturn(false);

            // When
            boolean available = broker.isAvailable();

            // Then
            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("should return false when connection fails")
        void shouldReturnFalseWhenConnectionFails() {
            // Given
            when(rabbitTemplate.getConnectionFactory()).thenReturn(connectionFactory);
            when(connectionFactory.createConnection()).thenThrow(new RuntimeException("Connection refused"));

            // When
            boolean available = broker.isAvailable();

            // Then
            assertThat(available).isFalse();
        }
    }

    private WorkflowMessage createWorkflowMessage(String executionId, String topic) {
        return WorkflowMessage.builder()
                .executionId(executionId)
                .topic(topic)
                .currentStep(1)
                .status(WorkflowStatus.IN_PROGRESS)
                .build();
    }
}