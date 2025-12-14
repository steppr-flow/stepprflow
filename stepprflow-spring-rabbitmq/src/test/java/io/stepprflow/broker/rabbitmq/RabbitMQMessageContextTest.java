package io.stepprflow.broker.rabbitmq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQMessageContext Tests")
class RabbitMQMessageContextTest {

    @Mock
    private Channel channel;

    private MessageProperties messageProperties;
    private Message message;

    @BeforeEach
    void setUp() {
        messageProperties = new MessageProperties();
        messageProperties.setDeliveryTag(12345L);
        messageProperties.setReceivedRoutingKey("test.routing.key");
        message = new Message("test body".getBytes(), messageProperties);
    }

    @Nested
    @DisplayName("Constructor and getters")
    class ConstructorTests {

        @Test
        @DisplayName("Should create context with all fields")
        void shouldCreateContextWithAllFields() {
            // When
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "test-queue");

            // Then
            assertThat(context.getDestination()).isEqualTo("test-queue");
            assertThat(context.getMessageKey()).isEqualTo("test.routing.key");
            assertThat(context.getOffset()).isEqualTo("12345");
            assertThat(context.getOriginalMessage()).isEqualTo(message);
            assertThat(context.getChannel()).isEqualTo(channel);
        }
    }

    @Nested
    @DisplayName("getHeaders()")
    class GetHeadersTests {

        @Test
        @DisplayName("Should return custom headers")
        void shouldReturnCustomHeaders() {
            // Given
            messageProperties.setHeader("custom-header", "custom-value");
            messageProperties.setHeader("another-header", 42);

            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            Map<String, String> headers = context.getHeaders();

            // Then
            assertThat(headers).containsEntry("custom-header", "custom-value");
            assertThat(headers).containsEntry("another-header", "42");
        }

        @Test
        @DisplayName("Should include standard properties as headers")
        void shouldIncludeStandardProperties() {
            // Given
            messageProperties.setMessageId("msg-123");
            messageProperties.setCorrelationId("corr-456");
            messageProperties.setContentType("application/json");

            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            Map<String, String> headers = context.getHeaders();

            // Then
            assertThat(headers).containsEntry("messageId", "msg-123");
            assertThat(headers).containsEntry("correlationId", "corr-456");
            assertThat(headers).containsEntry("contentType", "application/json");
        }

        @Test
        @DisplayName("Should handle null header values")
        void shouldHandleNullHeaderValues() {
            // Given
            messageProperties.setHeader("null-header", null);
            messageProperties.setHeader("valid-header", "value");

            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            Map<String, String> headers = context.getHeaders();

            // Then
            assertThat(headers).doesNotContainKey("null-header");
            assertThat(headers).containsEntry("valid-header", "value");
        }

        @Test
        @DisplayName("Should return only default contentType when no custom headers")
        void shouldReturnOnlyDefaultContentTypeWhenNoHeaders() {
            // Given - no custom headers set (but MessageProperties has default contentType)
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            Map<String, String> headers = context.getHeaders();

            // Then - only contains default content type from MessageProperties
            assertThat(headers).containsOnlyKeys("contentType");
        }
    }

    @Nested
    @DisplayName("acknowledge()")
    class AcknowledgeTests {

        @Test
        @DisplayName("Should call basicAck on channel")
        void shouldCallBasicAck() throws IOException {
            // Given
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            context.acknowledge();

            // Then
            verify(channel).basicAck(12345L, false);
        }

        @Test
        @DisplayName("Should throw RuntimeException when basicAck fails")
        void shouldThrowWhenBasicAckFails() throws IOException {
            // Given
            doThrow(new IOException("Connection lost")).when(channel).basicAck(anyLong(), anyBoolean());
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When & Then
            assertThatThrownBy(() -> context.acknowledge())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to acknowledge message");
        }
    }

    @Nested
    @DisplayName("reject()")
    class RejectTests {

        @Test
        @DisplayName("Should call basicReject with requeue=true")
        void shouldCallBasicRejectWithRequeue() throws IOException {
            // Given
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            context.reject(true);

            // Then
            verify(channel).basicReject(12345L, true);
        }

        @Test
        @DisplayName("Should call basicReject with requeue=false")
        void shouldCallBasicRejectWithoutRequeue() throws IOException {
            // Given
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When
            context.reject(false);

            // Then
            verify(channel).basicReject(12345L, false);
        }

        @Test
        @DisplayName("Should throw RuntimeException when basicReject fails")
        void shouldThrowWhenBasicRejectFails() throws IOException {
            // Given
            doThrow(new IOException("Connection lost")).when(channel).basicReject(anyLong(), anyBoolean());
            RabbitMQMessageContext context = new RabbitMQMessageContext(message, channel, "queue");

            // When & Then
            assertThatThrownBy(() -> context.reject(true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to reject message");
        }
    }

    @Nested
    @DisplayName("getOffset()")
    class GetOffsetTests {

        @Test
        @DisplayName("Should return delivery tag as string")
        void shouldReturnDeliveryTagAsString() {
            // Given
            messageProperties.setDeliveryTag(99999L);
            Message msg = new Message("body".getBytes(), messageProperties);
            RabbitMQMessageContext context = new RabbitMQMessageContext(msg, channel, "queue");

            // When & Then
            assertThat(context.getOffset()).isEqualTo("99999");
        }
    }
}