package io.stepprflow.monitor.service;

import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import io.stepprflow.monitor.websocket.WorkflowWebSocketHandler;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionPersistenceService Tests")
class ExecutionPersistenceServiceTest {

    @Mock
    private WorkflowExecutionRepository repository;

    @Mock
    private WorkflowWebSocketHandler webSocketHandler;

    @InjectMocks
    private ExecutionPersistenceService persistenceService;

    @Captor
    private ArgumentCaptor<WorkflowExecution> executionCaptor;

    private WorkflowMessage testMessage;

    @BeforeEach
    void setUp() {
        testMessage = WorkflowMessage.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .currentStep(1)
                .totalSteps(3)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("key", "value"))
                .payloadType("java.util.Map")
                .securityContext("token-abc")
                .metadata(Map.of("user", "test-user"))
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("onWorkflowMessage() method")
    class OnWorkflowMessageTests {

        @Test
        @DisplayName("Should ignore null message")
        void shouldIgnoreNullMessage() {
            persistenceService.onWorkflowMessage(null);

            verify(repository, never()).save(any());
            verify(webSocketHandler, never()).broadcastUpdate(any());
        }

        @Test
        @DisplayName("Should create new execution for new message")
        void shouldCreateNewExecutionForNewMessage() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getExecutionId()).isEqualTo("exec-123");
            assertThat(saved.getCorrelationId()).isEqualTo("corr-456");
            assertThat(saved.getTopic()).isEqualTo("test-topic");
            assertThat(saved.getTotalSteps()).isEqualTo(3);
            assertThat(saved.getPayload()).isEqualTo(testMessage.getPayload());
            assertThat(saved.getPayloadType()).isEqualTo("java.util.Map");
            assertThat(saved.getSecurityContext()).isEqualTo("token-abc");
            assertThat(saved.getMetadata()).isEqualTo(testMessage.getMetadata());
        }

        @Test
        @DisplayName("Should update existing execution")
        void shouldUpdateExistingExecution() {
            WorkflowExecution existing = WorkflowExecution.builder()
                    .executionId("exec-123")
                    .topic("test-topic")
                    .status(WorkflowStatus.PENDING)
                    .currentStep(1)
                    .stepHistory(new ArrayList<>())
                    .build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(existing));

            testMessage = testMessage.toBuilder()
                    .currentStep(2)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(saved.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should broadcast update via WebSocket")
        void shouldBroadcastUpdateViaWebSocket() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            persistenceService.onWorkflowMessage(testMessage);

            verify(webSocketHandler).broadcastUpdate(any(WorkflowExecution.class));
        }

        @Test
        @DisplayName("Should set updatedAt timestamp")
        void shouldSetUpdatedAtTimestamp() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Status handling")
    class StatusHandlingTests {

        @Test
        @DisplayName("Should update retry info")
        void shouldUpdateRetryInfo() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(3)
                    .nextRetryAt(Instant.now().plusSeconds(60))
                    .lastError("Previous error")
                    .build();
            testMessage = testMessage.toBuilder()
                    .retryInfo(retryInfo)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getRetryInfo()).isEqualTo(retryInfo);
        }

        @Test
        @DisplayName("Should update error info")
        void shouldUpdateErrorInfo() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            ErrorInfo errorInfo = ErrorInfo.builder()
                    .code("ERR_001")
                    .message("Something went wrong")
                    .exceptionType("java.lang.RuntimeException")
                    .build();
            testMessage = testMessage.toBuilder()
                    .errorInfo(errorInfo)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getErrorInfo()).isEqualTo(errorInfo);
        }
    }

    @Nested
    @DisplayName("Completion handling")
    class CompletionHandlingTests {

        @Test
        @DisplayName("Should set completedAt for COMPLETED status")
        void shouldSetCompletedAtForCompletedStatus() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.COMPLETED)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set completedAt for FAILED status")
        void shouldSetCompletedAtForFailedStatus() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.FAILED)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should calculate duration on completion")
        void shouldCalculateDurationOnCompletion() {
            Instant createdAt = Instant.now().minusSeconds(10);
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.COMPLETED)
                    .createdAt(createdAt)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getDurationMs()).isNotNull();
            assertThat(saved.getDurationMs()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Step history tracking")
    class StepHistoryTrackingTests {

        @Test
        @DisplayName("Should add step to history for IN_PROGRESS status")
        void shouldAddStepToHistoryForInProgressStatus() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.IN_PROGRESS)
                    .currentStep(1)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStepHistory()).isNotNull();
            assertThat(saved.getStepHistory()).hasSize(1);
            assertThat(saved.getStepHistory().get(0).getStepId()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should add step to history for COMPLETED status")
        void shouldAddStepToHistoryForCompletedStatus() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.COMPLETED)
                    .currentStep(3)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStepHistory()).isNotNull();
            assertThat(saved.getStepHistory()).isNotEmpty();
        }

        @Test
        @DisplayName("Should add step to history for FAILED status")
        void shouldAddStepToHistoryForFailedStatus() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.FAILED)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStepHistory()).isNotNull();
        }

        @Test
        @DisplayName("Should update existing step in history")
        void shouldUpdateExistingStepInHistory() {
            WorkflowExecution.StepExecution existingStep = WorkflowExecution.StepExecution.builder()
                    .stepId(1)
                    .startedAt(Instant.now().minusSeconds(5))
                    .status(WorkflowStatus.IN_PROGRESS)
                    .build();

            ArrayList<WorkflowExecution.StepExecution> history = new ArrayList<>();
            history.add(existingStep);

            WorkflowExecution existing = WorkflowExecution.builder()
                    .executionId("exec-123")
                    .topic("test-topic")
                    .stepHistory(history)
                    .build();
            when(repository.findById("exec-123")).thenReturn(Optional.of(existing));

            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.COMPLETED)
                    .currentStep(1)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStepHistory()).hasSize(1);
            assertThat(saved.getStepHistory().get(0).getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
            assertThat(saved.getStepHistory().get(0).getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should record error message in step history")
        void shouldRecordErrorMessageInStepHistory() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            ErrorInfo errorInfo = ErrorInfo.builder()
                    .message("Step failed with error")
                    .build();
            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.FAILED)
                    .errorInfo(errorInfo)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStepHistory().get(0).getErrorMessage()).isEqualTo("Step failed with error");
        }

        @Test
        @DisplayName("Should record retry attempt in step history")
        void shouldRecordRetryAttemptInStepHistory() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(5)
                    .build();
            testMessage = testMessage.toBuilder()
                    .status(WorkflowStatus.IN_PROGRESS)
                    .retryInfo(retryInfo)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getStepHistory().get(0).getAttempt()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("CreatedAt handling")
    class CreatedAtHandlingTests {

        @Test
        @DisplayName("Should use message createdAt if present")
        void shouldUseMessageCreatedAtIfPresent() {
            Instant messageCreatedAt = Instant.parse("2024-01-15T10:00:00Z");
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .createdAt(messageCreatedAt)
                    .build();

            persistenceService.onWorkflowMessage(testMessage);

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getCreatedAt()).isEqualTo(messageCreatedAt);
        }

        @Test
        @DisplayName("Should use current time if message createdAt is null")
        void shouldUseCurrentTimeIfMessageCreatedAtIsNull() {
            when(repository.findById("exec-123")).thenReturn(Optional.empty());

            testMessage = testMessage.toBuilder()
                    .createdAt(null)
                    .build();

            Instant before = Instant.now();
            persistenceService.onWorkflowMessage(testMessage);
            Instant after = Instant.now();

            verify(repository).save(executionCaptor.capture());
            WorkflowExecution saved = executionCaptor.getValue();

            assertThat(saved.getCreatedAt()).isBetween(before, after.plusMillis(1));
        }
    }
}