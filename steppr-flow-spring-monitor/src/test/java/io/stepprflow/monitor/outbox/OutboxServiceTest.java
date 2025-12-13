package io.stepprflow.monitor.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.outbox.OutboxMessage.MessageType;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OutboxService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService Tests")
class OutboxServiceTest {

    @Mock
    private OutboxMessageRepository outboxRepository;

    @Captor
    private ArgumentCaptor<OutboxMessage> outboxCaptor;

    private ObjectMapper objectMapper;
    private MonitorProperties properties;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        properties = new MonitorProperties();
        properties.getOutbox().setMaxAttempts(5);
        properties.getOutbox().setEnabled(true);

        outboxService = new OutboxService(outboxRepository, objectMapper, properties);
    }

    private WorkflowMessage createTestMessage() {
        return WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic("test-topic")
                .currentStep(2)
                .totalSteps(5)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("key", "value"))
                .build();
    }

    @Nested
    @DisplayName("enqueue()")
    class Enqueue {

        @Test
        @DisplayName("Should create outbox message with correct fields")
        void shouldCreateOutboxMessageWithCorrectFields() {
            WorkflowMessage message = createTestMessage();
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxService.enqueue("test-destination", message, MessageType.RESUME);

            verify(outboxRepository).save(outboxCaptor.capture());
            OutboxMessage saved = outboxCaptor.getValue();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getDestination()).isEqualTo("test-destination");
            assertThat(saved.getExecutionId()).isEqualTo(message.getExecutionId());
            assertThat(saved.getMessageType()).isEqualTo(MessageType.RESUME);
            assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(saved.getAttempts()).isZero();
            assertThat(saved.getMaxAttempts()).isEqualTo(5);
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should serialize message payload as JSON")
        void shouldSerializeMessagePayloadAsJson() {
            WorkflowMessage message = createTestMessage();
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxService.enqueue("test-destination", message, MessageType.RESUME);

            verify(outboxRepository).save(outboxCaptor.capture());
            OutboxMessage saved = outboxCaptor.getValue();

            assertThat(saved.getPayload()).contains(message.getExecutionId());
            assertThat(saved.getPayloadClass()).isEqualTo(WorkflowMessage.class.getName());
        }

        @Test
        @DisplayName("Should return outbox message ID")
        void shouldReturnOutboxMessageId() {
            WorkflowMessage message = createTestMessage();
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            String id = outboxService.enqueue("test-destination", message, MessageType.RESUME);

            assertThat(id).isNotNull();
        }
    }

    @Nested
    @DisplayName("enqueueResume()")
    class EnqueueResume {

        @Test
        @DisplayName("Should create message with RESUME type")
        void shouldCreateMessageWithResumeType() {
            WorkflowMessage message = createTestMessage();
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxService.enqueueResume("test-destination", message);

            verify(outboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getMessageType()).isEqualTo(MessageType.RESUME);
        }
    }

    @Nested
    @DisplayName("enqueueRetry()")
    class EnqueueRetry {

        @Test
        @DisplayName("Should create message with RETRY type")
        void shouldCreateMessageWithRetryType() {
            WorkflowMessage message = createTestMessage();
            when(outboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxService.enqueueRetry("test-destination", message);

            verify(outboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getMessageType()).isEqualTo(MessageType.RETRY);
        }
    }

    @Nested
    @DisplayName("isEnabled()")
    class IsEnabled {

        @Test
        @DisplayName("Should return true when enabled")
        void shouldReturnTrueWhenEnabled() {
            properties.getOutbox().setEnabled(true);

            assertThat(outboxService.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should return false when disabled")
        void shouldReturnFalseWhenDisabled() {
            properties.getOutbox().setEnabled(false);
            outboxService = new OutboxService(outboxRepository, objectMapper, properties);

            assertThat(outboxService.isEnabled()).isFalse();
        }
    }
}
