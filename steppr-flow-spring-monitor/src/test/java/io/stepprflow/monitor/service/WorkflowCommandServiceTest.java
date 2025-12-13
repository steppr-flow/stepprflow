package io.stepprflow.monitor.service;

import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.exception.MessageSendException;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.exception.ConcurrentModificationException;
import io.stepprflow.monitor.exception.WorkflowResumeException;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.outbox.OutboxService;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import io.stepprflow.monitor.util.WorkflowMessageFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WorkflowCommandService.
 * This service handles state-changing operations (resume, cancel).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowCommandService Tests")
class WorkflowCommandServiceTest {

    @Mock
    private WorkflowExecutionRepository repository;

    @Mock
    private MessageBroker messageBroker;

    @Mock
    private WorkflowMessageFactory messageFactory;

    @InjectMocks
    private WorkflowCommandService commandService;

    @Captor
    private ArgumentCaptor<WorkflowExecution> executionCaptor;

    private WorkflowExecution testExecution;

    @BeforeEach
    void setUp() {
        testExecution = WorkflowExecution.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .status(WorkflowStatus.FAILED)
                .currentStep(2)
                .totalSteps(5)
                .payload(Map.of("data", "test"))
                .payloadType("java.util.Map")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
    }

    @Nested
    @DisplayName("resume() method")
    class ResumeTests {

        @Test
        @DisplayName("Should throw exception when execution not found")
        void shouldThrowExceptionWhenNotFound() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.resume("unknown", null, "UI User"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution not found");
        }

        @Test
        @DisplayName("Should throw exception when status is not resumable")
        void shouldThrowExceptionWhenStatusNotResumable() {
            testExecution.setStatus(WorkflowStatus.COMPLETED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> commandService.resume("exec-123", null, "UI User"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot resume execution with status");
        }

        @Test
        @DisplayName("Should resume FAILED execution")
        void shouldResumeFailedExecution() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder()
                    .executionId("exec-123")
                    .currentStep(2)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(testExecution, 2)).thenReturn(resumeMessage);

            commandService.resume("exec-123", null, "UI User");

            verify(messageFactory).createResumeMessage(testExecution, 2);
            verify(messageBroker).sendSync("test-topic", resumeMessage);
        }

        @Test
        @DisplayName("Should resume PAUSED execution")
        void shouldResumePausedExecution() {
            testExecution.setStatus(WorkflowStatus.PAUSED);
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(testExecution, 2)).thenReturn(resumeMessage);

            commandService.resume("exec-123", null, "UI User");

            verify(messageBroker).sendSync(eq("test-topic"), any(WorkflowMessage.class));
        }

        @Test
        @DisplayName("Should resume RETRY_PENDING execution")
        void shouldResumeRetryPendingExecution() {
            testExecution.setStatus(WorkflowStatus.RETRY_PENDING);
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(testExecution, 2)).thenReturn(resumeMessage);

            commandService.resume("exec-123", null, "UI User");

            verify(messageBroker).sendSync(eq("test-topic"), any(WorkflowMessage.class));
        }

        @Test
        @DisplayName("Should resume from specified step")
        void shouldResumeFromSpecifiedStep() {
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder().currentStep(1).build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(testExecution, 1)).thenReturn(resumeMessage);

            commandService.resume("exec-123", 1, "UI User");

            verify(messageFactory).createResumeMessage(testExecution, 1);
            verify(messageBroker).sendSync("test-topic", resumeMessage);
        }

        @Test
        @DisplayName("Should create execution attempt on resume")
        void shouldCreateExecutionAttemptOnResume() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(any(), anyInt())).thenReturn(resumeMessage);

            commandService.resume("exec-123", null, "UI User");

            assertThat(testExecution.getExecutionAttempts()).hasSize(1);
            WorkflowExecution.ExecutionAttempt attempt = testExecution.getExecutionAttempts().get(0);
            assertThat(attempt.getAttemptNumber()).isEqualTo(1);
            assertThat(attempt.getStartStep()).isEqualTo(2);
            assertThat(attempt.getStartedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should increment attempt number for subsequent resumes")
        void shouldIncrementAttemptNumber() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            List<WorkflowExecution.ExecutionAttempt> existingAttempts = new ArrayList<>();
            existingAttempts.add(WorkflowExecution.ExecutionAttempt.builder()
                    .attemptNumber(1)
                    .startedAt(Instant.now().minusSeconds(3600))
                    .build());
            testExecution.setExecutionAttempts(existingAttempts);
            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(any(), anyInt())).thenReturn(resumeMessage);

            commandService.resume("exec-123", null, "UI User");

            assertThat(testExecution.getExecutionAttempts()).hasSize(2);
            WorkflowExecution.ExecutionAttempt newAttempt = testExecution.getExecutionAttempts().get(1);
            assertThat(newAttempt.getAttemptNumber()).isEqualTo(2);
            assertThat(newAttempt.getResumedBy()).isEqualTo("UI User");
        }

        @Test
        @DisplayName("Should transfer pending payload changes to execution attempt")
        void shouldTransferPayloadChangesToAttempt() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setExecutionAttempts(new ArrayList<>());

            List<WorkflowExecution.PayloadChange> pendingChanges = new ArrayList<>();
            pendingChanges.add(WorkflowExecution.PayloadChange.builder()
                    .fieldPath("orderId")
                    .oldValue("OLD")
                    .newValue("NEW")
                    .changedAt(Instant.now())
                    .changedBy("user")
                    .reason("Fix")
                    .build());
            testExecution.setPayloadHistory(pendingChanges);

            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(any(), anyInt())).thenReturn(resumeMessage);

            commandService.resume("exec-123", null, "UI User");

            assertThat(testExecution.getPayloadHistory()).isEmpty();
            WorkflowExecution.ExecutionAttempt attempt = testExecution.getExecutionAttempts().get(0);
            assertThat(attempt.getPayloadChanges()).hasSize(1);
            assertThat(attempt.getPayloadChanges().get(0).getFieldPath()).isEqualTo("orderId");
        }

        @Test
        @DisplayName("Should use outbox when enabled")
        void shouldUseOutboxWhenEnabled() {
            // Create service with outbox
            OutboxService outboxService = org.mockito.Mockito.mock(OutboxService.class);
            when(outboxService.isEnabled()).thenReturn(true);
            when(outboxService.enqueueResume(eq("test-topic"), any(WorkflowMessage.class)))
                    .thenReturn("outbox-123");

            WorkflowCommandService serviceWithOutbox = new WorkflowCommandService(
                    repository, messageBroker, messageFactory, outboxService);

            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(any(), anyInt())).thenReturn(resumeMessage);

            serviceWithOutbox.resume("exec-123", null, "UI User");

            verify(outboxService).enqueueResume("test-topic", resumeMessage);
            verify(messageBroker, never()).sendSync(any(), any());
        }

        @Test
        @DisplayName("Should throw WorkflowResumeException when broker send fails")
        void shouldThrowWorkflowResumeExceptionWhenBrokerFails() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setExecutionAttempts(new ArrayList<>());
            WorkflowMessage resumeMessage = WorkflowMessage.builder().build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(messageFactory.createResumeMessage(any(), anyInt())).thenReturn(resumeMessage);
            doThrow(new MessageSendException("kafka", "test-topic", "Connection failed"))
                    .when(messageBroker).sendSync(any(), any());

            assertThatThrownBy(() -> commandService.resume("exec-123", null, "UI User"))
                    .isInstanceOf(WorkflowResumeException.class);
        }

        @Test
        @DisplayName("Should throw ConcurrentModificationException on optimistic lock failure")
        void shouldThrowConcurrentModificationExceptionOnLockFailure() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            testExecution.setExecutionAttempts(new ArrayList<>());
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("Concurrent update"));

            assertThatThrownBy(() -> commandService.resume("exec-123", null, "UI User"))
                    .isInstanceOf(ConcurrentModificationException.class);
        }
    }

    @Nested
    @DisplayName("cancel() method")
    class CancelTests {

        @Test
        @DisplayName("Should throw exception when execution not found")
        void shouldThrowExceptionWhenNotFound() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.cancel("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution not found");
        }

        @Test
        @DisplayName("Should throw exception when already COMPLETED")
        void shouldThrowExceptionWhenAlreadyCompleted() {
            testExecution.setStatus(WorkflowStatus.COMPLETED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> commandService.cancel("exec-123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel execution with status");
        }

        @Test
        @DisplayName("Should throw exception when already CANCELLED")
        void shouldThrowExceptionWhenAlreadyCancelled() {
            testExecution.setStatus(WorkflowStatus.CANCELLED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            assertThatThrownBy(() -> commandService.cancel("exec-123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel execution with status");
        }

        @Test
        @DisplayName("Should cancel IN_PROGRESS execution")
        void shouldCancelInProgressExecution() {
            testExecution.setStatus(WorkflowStatus.IN_PROGRESS);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            commandService.cancel("exec-123");

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStatus()).isEqualTo(WorkflowStatus.CANCELLED);
            assertThat(saved.getCompletedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should cancel PENDING execution")
        void shouldCancelPendingExecution() {
            testExecution.setStatus(WorkflowStatus.PENDING);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            commandService.cancel("exec-123");

            verify(repository).save(any(WorkflowExecution.class));
        }

        @Test
        @DisplayName("Should cancel FAILED execution")
        void shouldCancelFailedExecution() {
            testExecution.setStatus(WorkflowStatus.FAILED);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            commandService.cancel("exec-123");

            verify(repository).save(any(WorkflowExecution.class));
        }

        @Test
        @DisplayName("Should throw ConcurrentModificationException on optimistic lock failure")
        void shouldThrowConcurrentModificationExceptionOnLockFailure() {
            testExecution.setStatus(WorkflowStatus.IN_PROGRESS);
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));
            when(repository.save(any())).thenThrow(new OptimisticLockingFailureException("Concurrent update"));

            assertThatThrownBy(() -> commandService.cancel("exec-123"))
                    .isInstanceOf(ConcurrentModificationException.class);
        }
    }
}

