package io.stepprflow.monitor.service;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WorkflowQueryService.
 * This service handles all read operations for workflow executions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowQueryService Tests")
class WorkflowQueryServiceTest {

    @Mock
    private WorkflowExecutionRepository repository;

    @InjectMocks
    private WorkflowQueryService queryService;

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
    @DisplayName("getExecution() method")
    class GetExecutionTests {

        @Test
        @DisplayName("Should return execution when found")
        void shouldReturnExecutionWhenFound() {
            when(repository.findById("exec-123")).thenReturn(Optional.of(testExecution));

            Optional<WorkflowExecution> result = queryService.getExecution("exec-123");

            assertThat(result).isPresent();
            assertThat(result.get().getExecutionId()).isEqualTo("exec-123");
        }

        @Test
        @DisplayName("Should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            when(repository.findById("unknown")).thenReturn(Optional.empty());

            Optional<WorkflowExecution> result = queryService.getExecution("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findExecutions() method")
    class FindExecutionsTests {

        private Pageable pageable;

        @BeforeEach
        void setUp() {
            pageable = PageRequest.of(0, 20);
        }

        @Test
        @DisplayName("Should filter by topic and statuses")
        void shouldFilterByTopicAndStatuses() {
            Page<WorkflowExecution> expectedPage = new PageImpl<>(List.of(testExecution));
            List<WorkflowStatus> statuses = List.of(WorkflowStatus.FAILED);
            when(repository.findByTopicAndStatusIn("test-topic", statuses, pageable))
                    .thenReturn(expectedPage);

            Page<WorkflowExecution> result = queryService.findExecutions("test-topic", statuses, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByTopicAndStatusIn("test-topic", statuses, pageable);
        }

        @Test
        @DisplayName("Should filter by topic only")
        void shouldFilterByTopicOnly() {
            Page<WorkflowExecution> expectedPage = new PageImpl<>(List.of(testExecution));
            when(repository.findByTopic("test-topic", pageable)).thenReturn(expectedPage);

            Page<WorkflowExecution> result = queryService.findExecutions("test-topic", null, pageable);

            verify(repository).findByTopic("test-topic", pageable);
        }

        @Test
        @DisplayName("Should filter by statuses only")
        void shouldFilterByStatusesOnly() {
            Page<WorkflowExecution> expectedPage = new PageImpl<>(List.of(testExecution));
            List<WorkflowStatus> statuses = List.of(WorkflowStatus.FAILED, WorkflowStatus.RETRY_PENDING);
            when(repository.findByStatusIn(statuses, pageable)).thenReturn(expectedPage);

            Page<WorkflowExecution> result = queryService.findExecutions(null, statuses, pageable);

            verify(repository).findByStatusIn(statuses, pageable);
        }

        @Test
        @DisplayName("Should return all when no filter")
        void shouldReturnAllWhenNoFilter() {
            Page<WorkflowExecution> expectedPage = new PageImpl<>(List.of(testExecution));
            when(repository.findAll(pageable)).thenReturn(expectedPage);

            Page<WorkflowExecution> result = queryService.findExecutions(null, null, pageable);

            verify(repository).findAll(pageable);
        }

        @Test
        @DisplayName("Should return all when empty statuses list")
        void shouldReturnAllWhenEmptyStatusesList() {
            Page<WorkflowExecution> expectedPage = new PageImpl<>(List.of(testExecution));
            when(repository.findAll(pageable)).thenReturn(expectedPage);

            Page<WorkflowExecution> result = queryService.findExecutions(null, List.of(), pageable);

            verify(repository).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("getStatistics() method")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should return statistics for all statuses")
        void shouldReturnStatisticsForAllStatuses() {
            when(repository.countByStatus(WorkflowStatus.PENDING)).thenReturn(5L);
            when(repository.countByStatus(WorkflowStatus.IN_PROGRESS)).thenReturn(3L);
            when(repository.countByStatus(WorkflowStatus.COMPLETED)).thenReturn(100L);
            when(repository.countByStatus(WorkflowStatus.FAILED)).thenReturn(2L);
            when(repository.countByStatus(WorkflowStatus.RETRY_PENDING)).thenReturn(1L);
            when(repository.countByStatus(WorkflowStatus.CANCELLED)).thenReturn(4L);
            when(repository.count()).thenReturn(115L);

            Map<String, Object> stats = queryService.getStatistics();

            assertThat(stats).containsEntry("pending", 5L);
            assertThat(stats).containsEntry("inProgress", 3L);
            assertThat(stats).containsEntry("completed", 100L);
            assertThat(stats).containsEntry("failed", 2L);
            assertThat(stats).containsEntry("retryPending", 1L);
            assertThat(stats).containsEntry("cancelled", 4L);
            assertThat(stats).containsEntry("total", 115L);
        }

        @Test
        @DisplayName("Should return zero counts when no executions")
        void shouldReturnZeroCountsWhenNoExecutions() {
            when(repository.countByStatus(any())).thenReturn(0L);
            when(repository.count()).thenReturn(0L);

            Map<String, Object> stats = queryService.getStatistics();

            assertThat(stats).containsEntry("pending", 0L);
            assertThat(stats).containsEntry("total", 0L);
        }
    }

    @Nested
    @DisplayName("getRecentExecutions() method")
    class GetRecentExecutionsTests {

        @Test
        @DisplayName("Should return recent executions")
        void shouldReturnRecentExecutions() {
            List<WorkflowExecution> recentExecutions = List.of(
                    testExecution,
                    WorkflowExecution.builder().executionId("exec-456").build()
            );
            when(repository.findTop10ByOrderByCreatedAtDesc()).thenReturn(recentExecutions);

            List<WorkflowExecution> result = queryService.getRecentExecutions();

            assertThat(result).hasSize(2);
            verify(repository).findTop10ByOrderByCreatedAtDesc();
        }

        @Test
        @DisplayName("Should return empty list when no executions")
        void shouldReturnEmptyListWhenNoExecutions() {
            when(repository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());

            List<WorkflowExecution> result = queryService.getRecentExecutions();

            assertThat(result).isEmpty();
        }
    }
}
