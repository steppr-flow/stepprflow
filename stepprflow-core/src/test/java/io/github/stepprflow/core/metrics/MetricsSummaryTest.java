package io.github.stepprflow.core.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MetricsSummary Tests")
class MetricsSummaryTest {

    @Nested
    @DisplayName("getSuccessRate()")
    class GetSuccessRateTests {

        @Test
        @DisplayName("Should return stored successRate when greater than 0")
        void shouldReturnStoredSuccessRateWhenGreaterThanZero() {
            MetricsSummary summary = MetricsSummary.builder()
                    .successRate(85.5)
                    .workflowsStarted(100)
                    .workflowsCompleted(50)
                    .build();

            // Should return the stored value, not calculate from started/completed
            assertThat(summary.getSuccessRate()).isEqualTo(85.5);
        }

        @Test
        @DisplayName("Should calculate successRate when stored value is 0")
        void shouldCalculateSuccessRateWhenStoredValueIsZero() {
            MetricsSummary summary = MetricsSummary.builder()
                    .successRate(0)
                    .workflowsStarted(100)
                    .workflowsCompleted(75)
                    .build();

            // Should calculate: 75 / 100 * 100 = 75%
            assertThat(summary.getSuccessRate()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("Should return 0 when workflowsStarted is 0")
        void shouldReturnZeroWhenWorkflowsStartedIsZero() {
            MetricsSummary summary = MetricsSummary.builder()
                    .successRate(0)
                    .workflowsStarted(0)
                    .workflowsCompleted(0)
                    .build();

            assertThat(summary.getSuccessRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 100% when all workflows completed")
        void shouldReturn100WhenAllCompleted() {
            MetricsSummary summary = MetricsSummary.builder()
                    .workflowsStarted(50)
                    .workflowsCompleted(50)
                    .build();

            assertThat(summary.getSuccessRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should calculate correctly with negative stored successRate")
        void shouldCalculateWhenStoredValueIsNegative() {
            MetricsSummary summary = MetricsSummary.builder()
                    .successRate(-1)  // Invalid stored value
                    .workflowsStarted(100)
                    .workflowsCompleted(80)
                    .build();

            // Stored value <= 0, so should calculate
            assertThat(summary.getSuccessRate()).isEqualTo(80.0);
        }
    }

    @Nested
    @DisplayName("getFailureRate()")
    class GetFailureRateTests {

        @Test
        @DisplayName("Should calculate failure rate correctly")
        void shouldCalculateFailureRateCorrectly() {
            MetricsSummary summary = MetricsSummary.builder()
                    .workflowsStarted(100)
                    .workflowsFailed(25)
                    .build();

            assertThat(summary.getFailureRate()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should return 0 when workflowsStarted is 0")
        void shouldReturnZeroWhenWorkflowsStartedIsZero() {
            MetricsSummary summary = MetricsSummary.builder()
                    .workflowsStarted(0)
                    .workflowsFailed(0)
                    .build();

            assertThat(summary.getFailureRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getThroughputRate()")
    class GetThroughputRateTests {

        @Test
        @DisplayName("Should calculate throughput rate correctly")
        void shouldCalculateThroughputRateCorrectly() {
            MetricsSummary summary = MetricsSummary.builder()
                    .workflowsStarted(100)
                    .workflowsCompleted(60)
                    .workflowsFailed(30)
                    .build();

            // (60 + 30) / 100 * 100 = 90%
            assertThat(summary.getThroughputRate()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should return 0 when workflowsStarted is 0")
        void shouldReturnZeroWhenWorkflowsStartedIsZero() {
            MetricsSummary summary = MetricsSummary.builder()
                    .workflowsStarted(0)
                    .workflowsCompleted(0)
                    .workflowsFailed(0)
                    .build();

            assertThat(summary.getThroughputRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Builder and fields")
    class BuilderTests {

        @Test
        @DisplayName("Should build with all fields")
        void shouldBuildWithAllFields() {
            MetricsSummary summary = MetricsSummary.builder()
                    .topic("order-workflow")
                    .serviceName("order-service")
                    .workflowsStarted(1000)
                    .workflowsCompleted(900)
                    .workflowsFailed(50)
                    .workflowsCancelled(10)
                    .workflowsActive(40)
                    .retryCount(100)
                    .dlqCount(20)
                    .avgWorkflowDurationMs(500.5)
                    .successRate(90.0)
                    .build();

            assertThat(summary.getTopic()).isEqualTo("order-workflow");
            assertThat(summary.getServiceName()).isEqualTo("order-service");
            assertThat(summary.getWorkflowsStarted()).isEqualTo(1000);
            assertThat(summary.getWorkflowsCompleted()).isEqualTo(900);
            assertThat(summary.getWorkflowsFailed()).isEqualTo(50);
            assertThat(summary.getWorkflowsCancelled()).isEqualTo(10);
            assertThat(summary.getWorkflowsActive()).isEqualTo(40);
            assertThat(summary.getRetryCount()).isEqualTo(100);
            assertThat(summary.getDlqCount()).isEqualTo(20);
            assertThat(summary.getAvgWorkflowDurationMs()).isEqualTo(500.5);
        }
    }
}