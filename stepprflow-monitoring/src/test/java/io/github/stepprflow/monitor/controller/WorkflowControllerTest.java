package io.github.stepprflow.monitor.controller;

import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import io.github.stepprflow.monitor.service.PayloadManagementService;
import io.github.stepprflow.monitor.service.WorkflowCommandService;
import io.github.stepprflow.monitor.service.WorkflowQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowController Tests")
class WorkflowControllerTest {

    @Mock
    private WorkflowQueryService queryService;

    @Mock
    private WorkflowCommandService commandService;

    @Mock
    private PayloadManagementService payloadService;

    @InjectMocks
    private WorkflowController controller;

    private WorkflowExecution testExecution;

    @BeforeEach
    void setUp() {
        testExecution = WorkflowExecution.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .status(WorkflowStatus.IN_PROGRESS)
                .currentStep(2)
                .totalSteps(5)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /{executionId}")
    class GetExecutionTests {

        @Test
        @DisplayName("Should return 200 with execution when found")
        void shouldReturn200WhenFound() {
            when(queryService.getExecution("exec-123")).thenReturn(Optional.of(testExecution));

            ResponseEntity<WorkflowExecution> response = controller.getExecution("exec-123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getExecutionId()).isEqualTo("exec-123");
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404WhenNotFound() {
            when(queryService.getExecution("unknown")).thenReturn(Optional.empty());

            ResponseEntity<WorkflowExecution> response = controller.getExecution("unknown");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNull();
        }
    }

    @Nested
    @DisplayName("GET /")
    class ListExecutionsTests {

        @Test
        @DisplayName("Should return paginated executions with default params")
        void shouldReturnPaginatedExecutionsWithDefaultParams() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of(testExecution));
            when(queryService.findExecutions(eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(page);

            ResponseEntity<Page<WorkflowExecution>> response = controller.listExecutions(
                    null, null, 0, 20, "createdAt", "desc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter by topic")
        void shouldFilterByTopic() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of(testExecution));
            when(queryService.findExecutions(eq("test-topic"), eq(null), any(PageRequest.class)))
                    .thenReturn(page);

            ResponseEntity<Page<WorkflowExecution>> response = controller.listExecutions(
                    "test-topic", null, 0, 20, "createdAt", "desc");

            verify(queryService).findExecutions(eq("test-topic"), eq(null), any(PageRequest.class));
        }

        @Test
        @DisplayName("Should filter by status")
        void shouldFilterByStatus() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of(testExecution));
            when(queryService.findExecutions(eq(null), eq(List.of(WorkflowStatus.FAILED)), any(PageRequest.class)))
                    .thenReturn(page);

            ResponseEntity<Page<WorkflowExecution>> response = controller.listExecutions(
                    null, "FAILED", 0, 20, "createdAt", "DESC");

            verify(queryService).findExecutions(eq(null), eq(List.of(WorkflowStatus.FAILED)), any(PageRequest.class));
        }

        @Test
        @DisplayName("Should filter by topic and status")
        void shouldFilterByTopicAndStatus() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of(testExecution));
            when(queryService.findExecutions(eq("test-topic"), eq(List.of(WorkflowStatus.COMPLETED)), any(PageRequest.class)))
                    .thenReturn(page);

            ResponseEntity<Page<WorkflowExecution>> response = controller.listExecutions(
                    "test-topic", "COMPLETED", 0, 20, "createdAt", "desc");

            verify(queryService).findExecutions(eq("test-topic"), eq(List.of(WorkflowStatus.COMPLETED)), any(PageRequest.class));
        }

        @Test
        @DisplayName("Should apply pagination params")
        void shouldApplyPaginationParams() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of(testExecution));
            when(queryService.findExecutions(any(), any(), any(PageRequest.class)))
                    .thenReturn(page);

            controller.listExecutions(null, null, 2, 50, "updatedAt", "asc");

            verify(queryService).findExecutions(eq(null), eq(null),
                    eq(PageRequest.of(2, 50, Sort.by(Sort.Direction.ASC, "updatedAt"))));
        }
    }

    @Nested
    @DisplayName("GET /recent")
    class GetRecentExecutionsTests {

        @Test
        @DisplayName("Should return recent executions")
        void shouldReturnRecentExecutions() {
            List<WorkflowExecution> recentExecutions = List.of(
                    testExecution,
                    WorkflowExecution.builder().executionId("exec-456").build()
            );
            when(queryService.getRecentExecutions()).thenReturn(recentExecutions);

            ResponseEntity<List<WorkflowExecution>> response = controller.getRecentExecutions();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no recent executions")
        void shouldReturnEmptyListWhenNoRecentExecutions() {
            when(queryService.getRecentExecutions()).thenReturn(List.of());

            ResponseEntity<List<WorkflowExecution>> response = controller.getRecentExecutions();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /stats")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should return statistics")
        void shouldReturnStatistics() {
            Map<String, Object> stats = Map.of(
                    "pending", 5L,
                    "inProgress", 3L,
                    "completed", 100L,
                    "failed", 2L,
                    "total", 110L
            );
            when(queryService.getStatistics()).thenReturn(stats);

            ResponseEntity<Map<String, Object>> response = controller.getStatistics();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("total", 110L);
            assertThat(response.getBody()).containsEntry("completed", 100L);
        }
    }

    @Nested
    @DisplayName("POST /{executionId}/resume")
    class ResumeTests {

        @Test
        @DisplayName("Should return 202 Accepted on successful resume")
        void shouldReturn202OnSuccessfulResume() {
            doNothing().when(commandService).resume("exec-123", null, "UI User");

            ResponseEntity<Void> response = controller.resume("exec-123", null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(commandService).resume("exec-123", null, "UI User");
        }

        @Test
        @DisplayName("Should resume from specified step")
        void shouldResumeFromSpecifiedStep() {
            doNothing().when(commandService).resume("exec-123", 1, "UI User");

            controller.resume("exec-123", 1);

            verify(commandService).resume("exec-123", 1, "UI User");
        }

        @Test
        @DisplayName("Should propagate IllegalArgumentException")
        void shouldPropagateIllegalArgumentException() {
            doThrow(new IllegalArgumentException("Execution not found"))
                    .when(commandService).resume("unknown", null, "UI User");

            assertThatThrownBy(() -> controller.resume("unknown", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution not found");
        }

        @Test
        @DisplayName("Should propagate IllegalStateException")
        void shouldPropagateIllegalStateException() {
            doThrow(new IllegalStateException("Cannot resume execution"))
                    .when(commandService).resume("exec-123", null, "UI User");

            assertThatThrownBy(() -> controller.resume("exec-123", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot resume execution");
        }
    }

    @Nested
    @DisplayName("DELETE /{executionId}")
    class CancelTests {

        @Test
        @DisplayName("Should return 204 No Content on successful cancel")
        void shouldReturn204OnSuccessfulCancel() {
            doNothing().when(commandService).cancel("exec-123");

            ResponseEntity<Void> response = controller.cancel("exec-123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(commandService).cancel("exec-123");
        }

        @Test
        @DisplayName("Should propagate IllegalArgumentException for not found")
        void shouldPropagateIllegalArgumentExceptionForNotFound() {
            doThrow(new IllegalArgumentException("Execution not found"))
                    .when(commandService).cancel("unknown");

            assertThatThrownBy(() -> controller.cancel("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Execution not found");
        }

        @Test
        @DisplayName("Should propagate IllegalStateException for invalid status")
        void shouldPropagateIllegalStateExceptionForInvalidStatus() {
            doThrow(new IllegalStateException("Cannot cancel execution"))
                    .when(commandService).cancel("exec-123");

            assertThatThrownBy(() -> controller.cancel("exec-123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel execution");
        }
    }

    @Nested
    @DisplayName("POST /{executionId}/payload/restore")
    class RestorePayloadTests {

        @Test
        @DisplayName("Should return 200 with restored execution")
        void shouldReturn200WithRestoredExecution() {
            when(payloadService.restorePayload("exec-123")).thenReturn(testExecution);

            ResponseEntity<WorkflowExecution> response = controller.restorePayload("exec-123");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getExecutionId()).isEqualTo("exec-123");
            verify(payloadService).restorePayload("exec-123");
        }
    }
}
