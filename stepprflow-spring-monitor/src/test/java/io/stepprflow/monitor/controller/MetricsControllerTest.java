package io.stepprflow.monitor.controller;

import io.stepprflow.core.metrics.MetricsSummary;
import io.stepprflow.core.metrics.WorkflowMetrics;
import io.stepprflow.monitor.dto.MetricsDashboard;
import io.stepprflow.monitor.dto.WorkflowMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * TDD tests for MetricsController.
 */
@ExtendWith(MockitoExtension.class)
class MetricsControllerTest {

    @Mock
    private WorkflowMetrics workflowMetrics;

    private MetricsController controller;

    @BeforeEach
    void setUp() {
        controller = new MetricsController(workflowMetrics);
    }

    @Nested
    @DisplayName("GET /api/metrics")
    class GetMetricsDashboard {

        @Test
        @DisplayName("should return metrics dashboard with global summary")
        void shouldReturnMetricsDashboard() {
            // Given
            MetricsSummary globalSummary = MetricsSummary.builder()
                    .topic("_global")
                    .workflowsStarted(100)
                    .workflowsCompleted(80)
                    .workflowsFailed(15)
                    .workflowsCancelled(5)
                    .workflowsActive(10)
                    .retryCount(20)
                    .dlqCount(3)
                    .successRate(80.0)
                    .build();

            when(workflowMetrics.getGlobalSummary()).thenReturn(globalSummary);
            when(workflowMetrics.getActiveWorkflowKeys()).thenReturn(Set.of());

            // When
            ResponseEntity<MetricsDashboard> response = controller.getMetricsDashboard();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalStarted()).isEqualTo(100);
            assertThat(response.getBody().getTotalCompleted()).isEqualTo(80);
            assertThat(response.getBody().getTotalFailed()).isEqualTo(15);
            assertThat(response.getBody().getTotalCancelled()).isEqualTo(5);
            assertThat(response.getBody().getTotalActive()).isEqualTo(10);
            assertThat(response.getBody().getTotalRetries()).isEqualTo(20);
            assertThat(response.getBody().getTotalDlq()).isEqualTo(3);
            assertThat(response.getBody().getGlobalSuccessRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("should return metrics dashboard with per-topic:serviceName metrics")
        void shouldReturnMetricsDashboardWithPerTopicServiceMetrics() {
            // Given
            MetricsSummary globalSummary = MetricsSummary.builder()
                    .topic("_global")
                    .workflowsStarted(50)
                    .workflowsCompleted(40)
                    .build();

            MetricsSummary kafkaSummary = MetricsSummary.builder()
                    .topic("order-workflow")
                    .serviceName("kafka-sample")
                    .workflowsStarted(30)
                    .workflowsCompleted(25)
                    .workflowsFailed(3)
                    .workflowsCancelled(2)
                    .workflowsActive(5)
                    .retryCount(10)
                    .dlqCount(1)
                    .avgWorkflowDurationMs(1500.0)
                    .successRate(83.3)
                    .build();

            MetricsSummary rabbitmqSummary = MetricsSummary.builder()
                    .topic("order-workflow")
                    .serviceName("rabbitmq-sample")
                    .workflowsStarted(20)
                    .workflowsCompleted(15)
                    .workflowsFailed(4)
                    .workflowsCancelled(1)
                    .workflowsActive(3)
                    .retryCount(5)
                    .dlqCount(2)
                    .avgWorkflowDurationMs(2500.0)
                    .successRate(75.0)
                    .build();

            when(workflowMetrics.getGlobalSummary()).thenReturn(globalSummary);
            when(workflowMetrics.getActiveWorkflowKeys()).thenReturn(Set.of(
                    new WorkflowMetrics.WorkflowKey("order-workflow", "kafka-sample"),
                    new WorkflowMetrics.WorkflowKey("order-workflow", "rabbitmq-sample")
            ));
            when(workflowMetrics.getSummary("order-workflow", "kafka-sample")).thenReturn(kafkaSummary);
            when(workflowMetrics.getSummary("order-workflow", "rabbitmq-sample")).thenReturn(rabbitmqSummary);

            // When
            ResponseEntity<MetricsDashboard> response = controller.getMetricsDashboard();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getWorkflowMetrics()).hasSize(2);

            // Verify serviceName is included
            var metrics = response.getBody().getWorkflowMetrics();
            assertThat(metrics).extracting(WorkflowMetricsDto::getServiceName)
                    .containsExactlyInAnyOrder("kafka-sample", "rabbitmq-sample");
            assertThat(metrics).extracting(WorkflowMetricsDto::getTopic)
                    .containsOnly("order-workflow");
        }

        @Test
        @DisplayName("should return empty metrics when no workflows registered")
        void shouldReturnEmptyMetricsWhenNoWorkflows() {
            // Given
            MetricsSummary globalSummary = MetricsSummary.builder()
                    .topic("_global")
                    .workflowsStarted(0)
                    .workflowsCompleted(0)
                    .workflowsFailed(0)
                    .workflowsCancelled(0)
                    .workflowsActive(0)
                    .retryCount(0)
                    .dlqCount(0)
                    .successRate(0)
                    .build();

            when(workflowMetrics.getGlobalSummary()).thenReturn(globalSummary);
            when(workflowMetrics.getActiveWorkflowKeys()).thenReturn(Set.of());

            // When
            ResponseEntity<MetricsDashboard> response = controller.getMetricsDashboard();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTotalStarted()).isZero();
            assertThat(response.getBody().getWorkflowMetrics()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/metrics/{topic}")
    class GetWorkflowMetrics {

        @Test
        @DisplayName("should return metrics for specific topic")
        void shouldReturnMetricsForTopic() {
            // Given
            MetricsSummary summary = MetricsSummary.builder()
                    .topic("order-workflow")
                    .workflowsStarted(50)
                    .workflowsCompleted(45)
                    .workflowsFailed(3)
                    .workflowsCancelled(2)
                    .workflowsActive(5)
                    .retryCount(10)
                    .dlqCount(1)
                    .avgWorkflowDurationMs(1200.0)
                    .successRate(90.0)
                    .build();

            when(workflowMetrics.getSummary("order-workflow")).thenReturn(summary);

            // When
            ResponseEntity<WorkflowMetricsDto> response = controller.getWorkflowMetrics("order-workflow");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getTopic()).isEqualTo("order-workflow");
            assertThat(response.getBody().getStarted()).isEqualTo(50);
            assertThat(response.getBody().getCompleted()).isEqualTo(45);
            assertThat(response.getBody().getFailed()).isEqualTo(3);
            assertThat(response.getBody().getCancelled()).isEqualTo(2);
            assertThat(response.getBody().getActive()).isEqualTo(5);
            assertThat(response.getBody().getRetries()).isEqualTo(10);
            assertThat(response.getBody().getDlq()).isEqualTo(1);
            assertThat(response.getBody().getAvgDurationMs()).isEqualTo(1200.0);
            assertThat(response.getBody().getSuccessRate()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("should return zero metrics for unknown topic")
        void shouldReturnZeroMetricsForUnknownTopic() {
            // Given
            MetricsSummary emptySummary = MetricsSummary.builder()
                    .topic("unknown-workflow")
                    .workflowsStarted(0)
                    .workflowsCompleted(0)
                    .workflowsFailed(0)
                    .workflowsCancelled(0)
                    .workflowsActive(0)
                    .retryCount(0)
                    .dlqCount(0)
                    .avgWorkflowDurationMs(0)
                    .successRate(0)
                    .build();

            when(workflowMetrics.getSummary("unknown-workflow")).thenReturn(emptySummary);

            // When
            ResponseEntity<WorkflowMetricsDto> response = controller.getWorkflowMetrics("unknown-workflow");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStarted()).isZero();
        }
    }

    @Nested
    @DisplayName("GET /api/metrics/summary")
    class GetGlobalSummary {

        @Test
        @DisplayName("should return global summary")
        void shouldReturnGlobalSummary() {
            // Given
            MetricsSummary summary = MetricsSummary.builder()
                    .topic("_global")
                    .workflowsStarted(200)
                    .workflowsCompleted(180)
                    .workflowsFailed(15)
                    .workflowsCancelled(5)
                    .workflowsActive(20)
                    .retryCount(30)
                    .dlqCount(5)
                    .successRate(90.0)
                    .build();

            when(workflowMetrics.getGlobalSummary()).thenReturn(summary);

            // When
            ResponseEntity<MetricsSummary> response = controller.getGlobalSummary();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getWorkflowsStarted()).isEqualTo(200);
            assertThat(response.getBody().getWorkflowsCompleted()).isEqualTo(180);
            assertThat(response.getBody().getSuccessRate()).isEqualTo(90.0);
        }
    }
}