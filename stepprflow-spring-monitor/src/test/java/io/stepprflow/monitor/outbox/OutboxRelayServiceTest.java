package io.stepprflow.monitor.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.exception.MessageSendException;
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
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OutboxRelayService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelayService Tests")
class OutboxRelayServiceTest {

    @Mock
    private OutboxMessageRepository outboxRepository;

    @Mock
    private MessageBroker messageBroker;

    @Captor
    private ArgumentCaptor<OutboxMessage> outboxCaptor;

    private ObjectMapper objectMapper;
    private MonitorProperties properties;
    private OutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        properties = new MonitorProperties();
        properties.getOutbox().setBatchSize(100);
        properties.getOutbox().setMaxAttempts(3);
        properties.getOutbox().setBaseDelayMs(1000);
        properties.getOutbox().setMaxDelayMs(60000);

        relayService = new OutboxRelayService(
                outboxRepository, messageBroker, objectMapper, properties);
    }

    private OutboxMessage createTestOutboxMessage() throws Exception {
        WorkflowMessage workflowMessage = WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic("test-topic")
                .currentStep(2)
                .totalSteps(5)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("key", "value"))
                .build();

        return OutboxMessage.builder()
                .id(UUID.randomUUID().toString())
                .destination("test-topic")
                .executionId(workflowMessage.getExecutionId())
                .messageType(MessageType.RESUME)
                .payload(objectMapper.writeValueAsString(workflowMessage))
                .payloadClass(WorkflowMessage.class.getName())
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("processOutbox()")
    class ProcessOutbox {

        @Test
        @DisplayName("Should do nothing when no pending messages")
        void shouldDoNothingWhenNoPendingMessages() {
            when(outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                    eq(OutboxStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of());

            relayService.processOutbox();

            verify(messageBroker, never()).sendSync(any(), any());
        }

        @Test
        @DisplayName("Should send message to broker")
        void shouldSendMessageToBroker() throws Exception {
            OutboxMessage outboxMessage = createTestOutboxMessage();
            when(outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                    eq(OutboxStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of(outboxMessage));

            relayService.processOutbox();

            verify(messageBroker).sendSync(eq("test-topic"), any(WorkflowMessage.class));
        }

        @Test
        @DisplayName("Should mark message as SENT on success")
        void shouldMarkMessageAsSentOnSuccess() throws Exception {
            OutboxMessage outboxMessage = createTestOutboxMessage();
            when(outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                    eq(OutboxStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of(outboxMessage));

            relayService.processOutbox();

            verify(outboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        @Test
        @DisplayName("Should increment attempts on failure")
        void shouldIncrementAttemptsOnFailure() throws Exception {
            OutboxMessage outboxMessage = createTestOutboxMessage();
            when(outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                    eq(OutboxStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of(outboxMessage));
            doThrow(new MessageSendException("kafka", "test-topic", "exec-1", "Connection failed", null))
                    .when(messageBroker).sendSync(any(), any());

            relayService.processOutbox();

            verify(outboxRepository).save(outboxCaptor.capture());
            OutboxMessage saved = outboxCaptor.getValue();
            assertThat(saved.getAttempts()).isEqualTo(1);
            assertThat(saved.getLastError()).contains("Connection failed");
        }

        @Test
        @DisplayName("Should mark as FAILED after max attempts")
        void shouldMarkAsFailedAfterMaxAttempts() throws Exception {
            OutboxMessage outboxMessage = createTestOutboxMessage();
            outboxMessage.setAttempts(2);  // Already at max - 1
            when(outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                    eq(OutboxStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of(outboxMessage));
            doThrow(new MessageSendException("kafka", "test-topic", "exec-1", "Connection failed", null))
                    .when(messageBroker).sendSync(any(), any());

            relayService.processOutbox();

            verify(outboxRepository).save(outboxCaptor.capture());
            assertThat(outboxCaptor.getValue().getStatus()).isEqualTo(OutboxStatus.FAILED);
        }

        @Test
        @DisplayName("Should process multiple messages")
        void shouldProcessMultipleMessages() throws Exception {
            OutboxMessage msg1 = createTestOutboxMessage();
            OutboxMessage msg2 = createTestOutboxMessage();
            when(outboxRepository.findByStatusAndNextRetryAtLessThanEqualOrNextRetryAtIsNull(
                    eq(OutboxStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                    .thenReturn(List.of(msg1, msg2));

            relayService.processOutbox();

            verify(messageBroker, times(2)).sendSync(any(), any());
            verify(outboxRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("cleanupSentMessages()")
    class CleanupSentMessages {

        @Test
        @DisplayName("Should delete old sent messages")
        void shouldDeleteOldSentMessages() {
            when(outboxRepository.deleteByStatusAndProcessedAtBefore(
                    eq(OutboxStatus.SENT), any(Instant.class)))
                    .thenReturn(10L);

            relayService.cleanupSentMessages();

            verify(outboxRepository).deleteByStatusAndProcessedAtBefore(
                    eq(OutboxStatus.SENT), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("Should return correct statistics")
        void shouldReturnCorrectStatistics() {
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(5L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(2L);

            OutboxRelayService.OutboxStats stats = relayService.getStats();

            assertThat(stats.pending()).isEqualTo(5L);
            assertThat(stats.sent()).isEqualTo(100L);
            assertThat(stats.failed()).isEqualTo(2L);
        }
    }
}
