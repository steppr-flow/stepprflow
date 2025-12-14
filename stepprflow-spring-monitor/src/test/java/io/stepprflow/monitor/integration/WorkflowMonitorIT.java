package io.stepprflow.monitor.integration;

import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.RetryInfo;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("test")
@EnableMongoRepositories(basePackageClasses = WorkflowExecutionRepository.class)
@DisplayName("Workflow Monitor MongoDB Integration Tests")
class WorkflowMonitorIT {

    @Autowired
    private WorkflowExecutionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("Save and retrieve operations")
    class SaveAndRetrieveTests {

        @Test
        @DisplayName("Should save and retrieve workflow execution by ID")
        void shouldSaveAndRetrieveWorkflowExecutionById() {
            // Given
            var executionId = UUID.randomUUID().toString();
            var execution = createExecution(executionId, "order-workflow", WorkflowStatus.PENDING);

            // When
            repository.save(execution);
            var retrieved = repository.findById(executionId);

            // Then
            assertThat(retrieved)
                    .isPresent()
                    .get()
                    .extracting(
                            WorkflowExecution::getExecutionId,
                            WorkflowExecution::getTopic,
                            WorkflowExecution::getStatus
                    )
                    .containsExactly(executionId, "order-workflow", WorkflowStatus.PENDING);
        }

        @Test
        @DisplayName("Should save execution with all fields")
        void shouldSaveExecutionWithAllFields() {
            // Given
            var executionId = UUID.randomUUID().toString();
            var execution = WorkflowExecution.builder()
                    .executionId(executionId)
                    .correlationId(UUID.randomUUID().toString())
                    .topic("detailed-workflow")
                    .status(WorkflowStatus.FAILED)
                    .currentStep(2)
                    .totalSteps(5)
                    .payload(Map.of("key", "value", "nested", Map.of("inner", 123)))
                    .metadata(Map.of("source", "api", "version", "1.0"))
                    .retryInfo(RetryInfo.builder().attempt(2).maxAttempts(3).build())
                    .errorInfo(ErrorInfo.builder().code("ERR_001").message("Test error").build())
                    .createdAt(Instant.now())
                    .build();

            // When
            repository.save(execution);
            var retrieved = repository.findById(executionId).orElseThrow();

            // Then
            assertThat((Map<String, Object>) retrieved.getPayload()).containsEntry("key", "value");
            assertThat(retrieved.getMetadata()).containsEntry("source", "api");
            assertThat(retrieved.getRetryInfo().getAttempt()).isEqualTo(2);
            assertThat(retrieved.getErrorInfo().getCode()).isEqualTo("ERR_001");
        }
    }

    @Nested
    @DisplayName("Query by topic")
    class QueryByTopicTests {

        @Test
        @DisplayName("Should find executions by topic")
        void shouldFindExecutionsByTopic() {
            // Given
            repository.saveAll(List.of(
                    createExecution("e1", "order-workflow", WorkflowStatus.PENDING),
                    createExecution("e2", "order-workflow", WorkflowStatus.COMPLETED),
                    createExecution("e3", "payment-workflow", WorkflowStatus.PENDING)
            ));

            // When
            var orderExecutions = repository.findByTopic("order-workflow", PageRequest.of(0, 100));

            // Then
            assertThat(orderExecutions.getContent())
                    .hasSize(2)
                    .extracting(WorkflowExecution::getTopic)
                    .containsOnly("order-workflow");
        }
    }

    @Nested
    @DisplayName("Query by status")
    class QueryByStatusTests {

        @Test
        @DisplayName("Should find executions by status")
        void shouldFindExecutionsByStatus() {
            // Given
            repository.saveAll(List.of(
                    createExecution("e1", "topic-1", WorkflowStatus.PENDING),
                    createExecution("e2", "topic-2", WorkflowStatus.COMPLETED),
                    createExecution("e3", "topic-3", WorkflowStatus.COMPLETED),
                    createExecution("e4", "topic-4", WorkflowStatus.FAILED)
            ));

            // When
            var completedExecutions = repository.findByStatus(WorkflowStatus.COMPLETED, PageRequest.of(0, 100));

            // Then
            assertThat(completedExecutions.getContent())
                    .hasSize(2)
                    .extracting(WorkflowExecution::getStatus)
                    .containsOnly(WorkflowStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should find executions by topic and status")
        void shouldFindExecutionsByTopicAndStatus() {
            // Given
            repository.saveAll(List.of(
                    createExecution("e1", "order-workflow", WorkflowStatus.PENDING),
                    createExecution("e2", "order-workflow", WorkflowStatus.COMPLETED),
                    createExecution("e3", "payment-workflow", WorkflowStatus.PENDING)
            ));

            // When
            var result = repository.findByTopicAndStatus("order-workflow", WorkflowStatus.PENDING, PageRequest.of(0, 100));

            // Then
            assertThat(result.getContent())
                    .hasSize(1)
                    .first()
                    .extracting(WorkflowExecution::getExecutionId)
                    .isEqualTo("e1");
        }
    }

    @Nested
    @DisplayName("Pagination and sorting")
    class PaginationAndSortingTests {

        @Test
        @DisplayName("Should return paginated results")
        void shouldReturnPaginatedResults() {
            // Given
            var executions = IntStream.rangeClosed(1, 25)
                    .mapToObj(i -> createExecution("exec-" + i, "topic", WorkflowStatus.PENDING))
                    .toList();
            repository.saveAll(executions);

            // When
            var page = repository.findAll(PageRequest.of(0, 10));

            // Then
            assertThat(page.getContent()).hasSize(10);
            assertThat(page.getTotalElements()).isEqualTo(25);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should sort by createdAt descending")
        void shouldSortByCreatedAtDescending() {
            // Given
            var now = Instant.now();
            repository.saveAll(List.of(
                    createExecutionWithTimestamp("oldest", "topic", now.minus(3, ChronoUnit.HOURS)),
                    createExecutionWithTimestamp("newest", "topic", now),
                    createExecutionWithTimestamp("middle", "topic", now.minus(1, ChronoUnit.HOURS))
            ));

            // When
            var sorted = repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

            // Then
            assertThat(sorted)
                    .extracting(WorkflowExecution::getExecutionId)
                    .containsExactly("newest", "middle", "oldest");
        }
    }

    @Nested
    @DisplayName("Count operations")
    class CountOperationsTests {

        @Test
        @DisplayName("Should count executions by status")
        void shouldCountExecutionsByStatus() {
            // Given
            repository.saveAll(List.of(
                    createExecution("e1", "t1", WorkflowStatus.PENDING),
                    createExecution("e2", "t2", WorkflowStatus.PENDING),
                    createExecution("e3", "t3", WorkflowStatus.COMPLETED),
                    createExecution("e4", "t4", WorkflowStatus.COMPLETED),
                    createExecution("e5", "t5", WorkflowStatus.COMPLETED),
                    createExecution("e6", "t6", WorkflowStatus.FAILED)
            ));

            // When
            var pendingCount = repository.countByStatus(WorkflowStatus.PENDING);
            var completedCount = repository.countByStatus(WorkflowStatus.COMPLETED);
            var failedCount = repository.countByStatus(WorkflowStatus.FAILED);

            // Then
            assertThat(pendingCount).isEqualTo(2);
            assertThat(completedCount).isEqualTo(3);
            assertThat(failedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Update operations")
    class UpdateOperationsTests {

        @Test
        @DisplayName("Should update execution status")
        void shouldUpdateExecutionStatus() {
            // Given
            var executionId = UUID.randomUUID().toString();
            var execution = createExecution(executionId, "workflow", WorkflowStatus.PENDING);
            repository.save(execution);

            // When
            execution.setStatus(WorkflowStatus.IN_PROGRESS);
            execution.setCurrentStep(2);
            repository.save(execution);

            // Then
            var updated = repository.findById(executionId).orElseThrow();
            assertThat(updated)
                    .extracting(WorkflowExecution::getStatus, WorkflowExecution::getCurrentStep)
                    .containsExactly(WorkflowStatus.IN_PROGRESS, 2);
        }
    }

    // Helper methods
    private WorkflowExecution createExecution(String executionId, String topic, WorkflowStatus status) {
        return WorkflowExecution.builder()
                .executionId(executionId)
                .correlationId(UUID.randomUUID().toString())
                .topic(topic)
                .status(status)
                .currentStep(1)
                .totalSteps(3)
                .createdAt(Instant.now())
                .build();
    }

    private WorkflowExecution createExecutionWithTimestamp(String executionId, String topic, Instant createdAt) {
        return WorkflowExecution.builder()
                .executionId(executionId)
                .topic(topic)
                .status(WorkflowStatus.PENDING)
                .currentStep(1)
                .totalSteps(1)
                .createdAt(createdAt)
                .build();
    }
}
