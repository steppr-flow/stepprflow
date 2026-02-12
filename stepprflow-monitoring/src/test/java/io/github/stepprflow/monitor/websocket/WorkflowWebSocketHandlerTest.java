package io.github.stepprflow.monitor.websocket;

import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.monitor.MonitorProperties;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for WorkflowWebSocketHandler.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WorkflowWebSocketHandler Tests")
class WorkflowWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MonitorProperties properties;

    @Mock
    private MonitorProperties.WebSocket webSocketProperties;

    @Captor
    private ArgumentCaptor<WorkflowWebSocketHandler.WorkflowUpdateDTO> dtoCaptor;

    private WorkflowWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        when(properties.getWebSocket()).thenReturn(webSocketProperties);
        when(webSocketProperties.getTopicPrefix()).thenReturn("/topic/workflow");
        handler = new WorkflowWebSocketHandler(messagingTemplate, properties);
    }

    @Nested
    @DisplayName("broadcastUpdate()")
    class BroadcastUpdateTests {

        @Test
        @DisplayName("should broadcast to general and topic-specific channels for PENDING status")
        void shouldBroadcastToGeneralAndTopicSpecificChannelsForPendingStatus() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-123")
                    .topic("order-workflow")
                    .status(WorkflowStatus.PENDING)
                    .currentStep(0)
                    .totalSteps(5)
                    .updatedAt(Instant.now())
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/updates"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/order-workflow"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
            // PENDING is not terminal, so no execution-specific broadcast
            verify(messagingTemplate, never()).convertAndSend(eq("/topic/workflow/execution/exec-123"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }

        @Test
        @DisplayName("should broadcast to all channels including execution-specific for COMPLETED status")
        void shouldBroadcastToAllChannelsIncludingExecutionSpecificForCompletedStatus() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-456")
                    .topic("payment-workflow")
                    .status(WorkflowStatus.COMPLETED)
                    .currentStep(3)
                    .totalSteps(3)
                    .updatedAt(Instant.now())
                    .durationMs(5000L)
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/updates"), dtoCaptor.capture());
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/payment-workflow"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/execution/exec-456"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));

            // Verify DTO content
            WorkflowWebSocketHandler.WorkflowUpdateDTO dto = dtoCaptor.getValue();
            assertThat(dto.getExecutionId()).isEqualTo("exec-456");
            assertThat(dto.getTopic()).isEqualTo("payment-workflow");
            assertThat(dto.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
            assertThat(dto.getCurrentStep()).isEqualTo(3);
            assertThat(dto.getTotalSteps()).isEqualTo(3);
            assertThat(dto.getDurationMs()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("should broadcast to execution-specific channel for FAILED status")
        void shouldBroadcastToExecutionSpecificChannelForFailedStatus() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-789")
                    .topic("inventory-workflow")
                    .status(WorkflowStatus.FAILED)
                    .currentStep(2)
                    .totalSteps(4)
                    .updatedAt(Instant.now())
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then
            verify(messagingTemplate, times(3)).convertAndSend(anyString(), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/execution/exec-789"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }

        @Test
        @DisplayName("should broadcast to execution-specific channel for CANCELLED status")
        void shouldBroadcastToExecutionSpecificChannelForCancelledStatus() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-cancel")
                    .topic("shipping-workflow")
                    .status(WorkflowStatus.CANCELLED)
                    .currentStep(1)
                    .totalSteps(3)
                    .updatedAt(Instant.now())
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/execution/exec-cancel"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }

        @Test
        @DisplayName("should not broadcast for non-meaningful statuses like RETRY_PENDING")
        void shouldNotBroadcastForNonMeaningfulStatusesLikeRetryPending() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-retry")
                    .topic("notification-workflow")
                    .status(WorkflowStatus.RETRY_PENDING)
                    .currentStep(1)
                    .totalSteps(2)
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }

        @Test
        @DisplayName("should not broadcast for PAUSED status")
        void shouldNotBroadcastForPausedStatus() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-paused")
                    .topic("paused-workflow")
                    .status(WorkflowStatus.PAUSED)
                    .currentStep(2)
                    .totalSteps(5)
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }

        @Test
        @DisplayName("should handle messaging exception gracefully")
        void shouldHandleMessagingExceptionGracefully() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-error")
                    .topic("error-workflow")
                    .status(WorkflowStatus.IN_PROGRESS)
                    .currentStep(1)
                    .totalSteps(3)
                    .updatedAt(Instant.now())
                    .build();

            doThrow(new RuntimeException("WebSocket error"))
                    .when(messagingTemplate).convertAndSend(anyString(), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));

            // When - should not throw
            handler.broadcastUpdate(execution);

            // Then - exception is caught and logged
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/updates"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }

        @Test
        @DisplayName("should broadcast for IN_PROGRESS status")
        void shouldBroadcastForInProgressStatus() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-progress")
                    .topic("progress-workflow")
                    .status(WorkflowStatus.IN_PROGRESS)
                    .currentStep(2)
                    .totalSteps(5)
                    .updatedAt(Instant.now())
                    .build();

            // When
            handler.broadcastUpdate(execution);

            // Then - broadcasts to general and topic-specific, not execution-specific
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/updates"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/workflow/progress-workflow"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
            verify(messagingTemplate, never()).convertAndSend(eq("/topic/workflow/execution/exec-progress"), any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("sendToUser()")
    class SendToUserTests {

        @Test
        @DisplayName("should send update to specific user")
        void shouldSendUpdateToSpecificUser() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-user")
                    .topic("user-workflow")
                    .status(WorkflowStatus.COMPLETED)
                    .currentStep(3)
                    .totalSteps(3)
                    .updatedAt(Instant.now())
                    .build();

            // When
            handler.sendToUser("user-123", execution);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    eq("user-123"),
                    eq("/topic/workflow/updates"),
                    any(WorkflowWebSocketHandler.WorkflowUpdateDTO.class));
        }
    }

    @Nested
    @DisplayName("WorkflowUpdateDTO")
    class WorkflowUpdateDTOTests {

        @Test
        @DisplayName("should create DTO from WorkflowExecution")
        void shouldCreateDtoFromWorkflowExecution() {
            // Given
            Instant now = Instant.now();
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-dto")
                    .topic("dto-workflow")
                    .status(WorkflowStatus.IN_PROGRESS)
                    .currentStep(2)
                    .totalSteps(5)
                    .updatedAt(now)
                    .durationMs(1500L)
                    .build();

            // When
            WorkflowWebSocketHandler.WorkflowUpdateDTO dto = WorkflowWebSocketHandler.WorkflowUpdateDTO.from(execution);

            // Then
            assertThat(dto.getExecutionId()).isEqualTo("exec-dto");
            assertThat(dto.getTopic()).isEqualTo("dto-workflow");
            assertThat(dto.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(dto.getCurrentStep()).isEqualTo(2);
            assertThat(dto.getTotalSteps()).isEqualTo(5);
            assertThat(dto.getUpdatedAt()).isEqualTo(now);
            assertThat(dto.getDurationMs()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("should handle null duration")
        void shouldHandleNullDuration() {
            // Given
            WorkflowExecution execution = WorkflowExecution.builder()
                    .executionId("exec-null-duration")
                    .topic("test-workflow")
                    .status(WorkflowStatus.PENDING)
                    .currentStep(0)
                    .totalSteps(3)
                    .durationMs(null)
                    .build();

            // When
            WorkflowWebSocketHandler.WorkflowUpdateDTO dto = WorkflowWebSocketHandler.WorkflowUpdateDTO.from(execution);

            // Then
            assertThat(dto.getDurationMs()).isNull();
        }
    }
}