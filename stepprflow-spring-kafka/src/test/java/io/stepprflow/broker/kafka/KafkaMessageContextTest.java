package io.stepprflow.broker.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("KafkaMessageContext Tests")
class KafkaMessageContextTest {

    @Nested
    @DisplayName("from() factory method")
    class FromFactoryTests {

        @Test
        @DisplayName("Should create context from ConsumerRecord with all fields")
        void shouldCreateContextFromConsumerRecord() {
            // Given
            RecordHeaders headers = new RecordHeaders();
            headers.add("header1", "value1".getBytes(StandardCharsets.UTF_8));
            headers.add("header2", "value2".getBytes(StandardCharsets.UTF_8));

            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "test-topic", 2, 100L, "test-key", "test-value"
            );
            record.headers().add("header1", "value1".getBytes(StandardCharsets.UTF_8));
            record.headers().add("header2", "value2".getBytes(StandardCharsets.UTF_8));

            Acknowledgment ack = mock(Acknowledgment.class);

            // When
            KafkaMessageContext context = KafkaMessageContext.from(record, ack);

            // Then
            assertThat(context.getDestination()).isEqualTo("test-topic");
            assertThat(context.getMessageKey()).isEqualTo("test-key");
            assertThat(context.getOffset()).isEqualTo("100");
            assertThat(context.getPartition()).isEqualTo(2);
            assertThat(context.getHeaders()).containsEntry("header1", "value1");
            assertThat(context.getHeaders()).containsEntry("header2", "value2");
        }

        @Test
        @DisplayName("Should handle empty headers")
        void shouldHandleEmptyHeaders() {
            // Given
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "test-topic", 0, 0L, "key", "value"
            );
            Acknowledgment ack = mock(Acknowledgment.class);

            // When
            KafkaMessageContext context = KafkaMessageContext.from(record, ack);

            // Then
            assertThat(context.getHeaders()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null key")
        void shouldHandleNullKey() {
            // Given
            ConsumerRecord<String, String> record = new ConsumerRecord<>(
                    "test-topic", 0, 0L, null, "value"
            );
            Acknowledgment ack = mock(Acknowledgment.class);

            // When
            KafkaMessageContext context = KafkaMessageContext.from(record, ack);

            // Then
            assertThat(context.getMessageKey()).isNull();
        }
    }

    @Nested
    @DisplayName("getOffset() method")
    class GetOffsetTests {

        @Test
        @DisplayName("Should return offset as string")
        void shouldReturnOffsetAsString() {
            // Given
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("topic")
                    .offset(12345L)
                    .partition(0)
                    .headers(Map.of())
                    .build();

            // When & Then
            assertThat(context.getOffset()).isEqualTo("12345");
        }
    }

    @Nested
    @DisplayName("acknowledge() method")
    class AcknowledgeTests {

        @Test
        @DisplayName("Should call acknowledgment when present")
        void shouldCallAcknowledgmentWhenPresent() {
            // Given
            Acknowledgment ack = mock(Acknowledgment.class);
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("topic")
                    .offset(0L)
                    .partition(0)
                    .headers(Map.of())
                    .acknowledgment(ack)
                    .build();

            // When
            context.acknowledge();

            // Then
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("Should handle null acknowledgment gracefully")
        void shouldHandleNullAcknowledgment() {
            // Given
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("topic")
                    .offset(0L)
                    .partition(0)
                    .headers(Map.of())
                    .acknowledgment(null)
                    .build();

            // When & Then - should not throw
            context.acknowledge();
        }
    }

    @Nested
    @DisplayName("reject() method")
    class RejectTests {

        @Test
        @DisplayName("Should acknowledge when requeue is false (skip message)")
        void shouldAcknowledgeWhenRequeueIsFalse() {
            // Given
            Acknowledgment ack = mock(Acknowledgment.class);
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("topic")
                    .offset(0L)
                    .partition(0)
                    .headers(Map.of())
                    .acknowledgment(ack)
                    .build();

            // When
            context.reject(false);

            // Then
            verify(ack).acknowledge();
        }

        @Test
        @DisplayName("Should not acknowledge when requeue is true (redeliver)")
        void shouldNotAcknowledgeWhenRequeueIsTrue() {
            // Given
            Acknowledgment ack = mock(Acknowledgment.class);
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("topic")
                    .offset(0L)
                    .partition(0)
                    .headers(Map.of())
                    .acknowledgment(ack)
                    .build();

            // When
            context.reject(true);

            // Then
            verify(ack, never()).acknowledge();
        }

        @Test
        @DisplayName("Should handle null acknowledgment when rejecting")
        void shouldHandleNullAcknowledgmentWhenRejecting() {
            // Given
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("topic")
                    .offset(0L)
                    .partition(0)
                    .headers(Map.of())
                    .acknowledgment(null)
                    .build();

            // When & Then - should not throw
            context.reject(false);
            context.reject(true);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Should build context with all fields")
        void shouldBuildContextWithAllFields() {
            // Given & When
            KafkaMessageContext context = KafkaMessageContext.builder()
                    .destination("my-topic")
                    .messageKey("my-key")
                    .offset(999L)
                    .partition(5)
                    .headers(Map.of("h1", "v1"))
                    .build();

            // Then
            assertThat(context.getDestination()).isEqualTo("my-topic");
            assertThat(context.getMessageKey()).isEqualTo("my-key");
            assertThat(context.getOffset()).isEqualTo("999");
            assertThat(context.getPartition()).isEqualTo(5);
            assertThat(context.getHeaders()).containsEntry("h1", "v1");
        }
    }
}
