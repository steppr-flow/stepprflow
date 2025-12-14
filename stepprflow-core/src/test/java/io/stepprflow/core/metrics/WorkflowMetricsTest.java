package io.stepprflow.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowMetrics Tests")
class WorkflowMetricsTest {

    private MeterRegistry meterRegistry;
    private WorkflowMetrics workflowMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        workflowMetrics = new WorkflowMetrics(meterRegistry);
    }

    @Nested
    @DisplayName("recordWorkflowStarted with serviceName")
    class RecordWorkflowStartedWithServiceNameTests {

        @Test
        @DisplayName("Should record workflow started with topic and serviceName")
        void shouldRecordWorkflowStartedWithServiceName() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");

            Counter counter = meterRegistry.find("stepprflow.workflow.started")
                    .tag("topic", "order-workflow")
                    .tag("service", "kafka-sample")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should use 'unknown' when serviceName is null")
        void shouldUseUnknownWhenServiceNameIsNull() {
            workflowMetrics.recordWorkflowStarted("order-workflow", null);

            Counter counter = meterRegistry.find("stepprflow.workflow.started")
                    .tag("topic", "order-workflow")
                    .tag("service", "unknown")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should track different services separately")
        void shouldTrackDifferentServicesSeparately() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "rabbitmq-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");

            Counter kafkaCounter = meterRegistry.find("stepprflow.workflow.started")
                    .tag("topic", "order-workflow")
                    .tag("service", "kafka-sample")
                    .counter();
            Counter rabbitmqCounter = meterRegistry.find("stepprflow.workflow.started")
                    .tag("topic", "order-workflow")
                    .tag("service", "rabbitmq-sample")
                    .counter();

            assertThat(kafkaCounter.count()).isEqualTo(2.0);
            assertThat(rabbitmqCounter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordWorkflowCompleted with serviceName")
    class RecordWorkflowCompletedWithServiceNameTests {

        @Test
        @DisplayName("Should record workflow completed with serviceName")
        void shouldRecordWorkflowCompletedWithServiceName() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowCompleted("order-workflow", "kafka-sample", Duration.ofMillis(1000));

            Counter counter = meterRegistry.find("stepprflow.workflow.completed")
                    .tag("topic", "order-workflow")
                    .tag("service", "kafka-sample")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordWorkflowFailed with serviceName")
    class RecordWorkflowFailedWithServiceNameTests {

        @Test
        @DisplayName("Should record workflow failed with serviceName")
        void shouldRecordWorkflowFailedWithServiceName() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowFailed("order-workflow", "kafka-sample", Duration.ofMillis(500));

            Counter counter = meterRegistry.find("stepprflow.workflow.failed")
                    .tag("topic", "order-workflow")
                    .tag("service", "kafka-sample")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("recordWorkflowCancelled with serviceName")
    class RecordWorkflowCancelledWithServiceNameTests {

        @Test
        @DisplayName("Should record workflow cancelled with serviceName")
        void shouldRecordWorkflowCancelledWithServiceName() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowCancelled("order-workflow", "kafka-sample");

            Counter counter = meterRegistry.find("stepprflow.workflow.cancelled")
                    .tag("topic", "order-workflow")
                    .tag("service", "kafka-sample")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("getActiveWorkflowKeys")
    class GetActiveWorkflowKeysTests {

        @Test
        @DisplayName("Should return set of topic:serviceName keys")
        void shouldReturnSetOfTopicServiceNameKeys() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "rabbitmq-sample");
            workflowMetrics.recordWorkflowStarted("payment-workflow", "kafka-sample");

            Set<WorkflowMetrics.WorkflowKey> keys = workflowMetrics.getActiveWorkflowKeys();

            assertThat(keys).hasSize(3);
            assertThat(keys).contains(
                    new WorkflowMetrics.WorkflowKey("order-workflow", "kafka-sample"),
                    new WorkflowMetrics.WorkflowKey("order-workflow", "rabbitmq-sample"),
                    new WorkflowMetrics.WorkflowKey("payment-workflow", "kafka-sample")
            );
        }

        @Test
        @DisplayName("Should return empty set when no workflows recorded")
        void shouldReturnEmptySetWhenNoWorkflowsRecorded() {
            Set<WorkflowMetrics.WorkflowKey> keys = workflowMetrics.getActiveWorkflowKeys();

            assertThat(keys).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSummary with serviceName")
    class GetSummaryWithServiceNameTests {

        @Test
        @DisplayName("Should return summary for specific topic and serviceName")
        void shouldReturnSummaryForTopicAndServiceName() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowCompleted("order-workflow", "kafka-sample", Duration.ofMillis(1000));
            workflowMetrics.recordWorkflowFailed("order-workflow", "kafka-sample", Duration.ofMillis(500));

            MetricsSummary summary = workflowMetrics.getSummary("order-workflow", "kafka-sample");

            assertThat(summary.getTopic()).isEqualTo("order-workflow");
            assertThat(summary.getServiceName()).isEqualTo("kafka-sample");
            assertThat(summary.getWorkflowsStarted()).isEqualTo(2);
            assertThat(summary.getWorkflowsCompleted()).isEqualTo(1);
            assertThat(summary.getWorkflowsFailed()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return separate summaries for different services with same topic")
        void shouldReturnSeparateSummariesForDifferentServices() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowCompleted("order-workflow", "kafka-sample", Duration.ofMillis(1000));

            workflowMetrics.recordWorkflowStarted("order-workflow", "rabbitmq-sample");
            workflowMetrics.recordWorkflowFailed("order-workflow", "rabbitmq-sample", Duration.ofMillis(500));

            MetricsSummary kafkaSummary = workflowMetrics.getSummary("order-workflow", "kafka-sample");
            MetricsSummary rabbitmqSummary = workflowMetrics.getSummary("order-workflow", "rabbitmq-sample");

            assertThat(kafkaSummary.getWorkflowsStarted()).isEqualTo(2);
            assertThat(kafkaSummary.getWorkflowsCompleted()).isEqualTo(1);
            assertThat(kafkaSummary.getWorkflowsFailed()).isEqualTo(0);

            assertThat(rabbitmqSummary.getWorkflowsStarted()).isEqualTo(1);
            assertThat(rabbitmqSummary.getWorkflowsCompleted()).isEqualTo(0);
            assertThat(rabbitmqSummary.getWorkflowsFailed()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Step metrics")
    class StepMetricsTests {

        @Test
        @DisplayName("Should record step executed with duration")
        void shouldRecordStepExecutedWithDuration() {
            workflowMetrics.recordStepExecuted("order-workflow", "validateOrder", Duration.ofMillis(100));

            Counter counter = meterRegistry.find("stepprflow.step.executed")
                    .tag("topic", "order-workflow")
                    .tag("step", "validateOrder")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record step failed")
        void shouldRecordStepFailed() {
            workflowMetrics.recordStepFailed("order-workflow", "processPayment");

            Counter counter = meterRegistry.find("stepprflow.step.failed")
                    .tag("topic", "order-workflow")
                    .tag("step", "processPayment")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record step timeout")
        void shouldRecordStepTimeout() {
            workflowMetrics.recordStepTimeout("order-workflow", "checkInventory");

            Counter counter = meterRegistry.find("stepprflow.step.timeout")
                    .tag("topic", "order-workflow")
                    .tag("step", "checkInventory")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Retry and DLQ metrics")
    class RetryAndDlqTests {

        @Test
        @DisplayName("Should record retry count")
        void shouldRecordRetryCount() {
            workflowMetrics.recordRetry("order-workflow", 3);

            Counter counter = meterRegistry.find("stepprflow.retry.count")
                    .tag("topic", "order-workflow")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should record DLQ count")
        void shouldRecordDlqCount() {
            workflowMetrics.recordDlq("failed-workflow");

            Counter counter = meterRegistry.find("stepprflow.dlq.count")
                    .tag("topic", "failed-workflow")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Global summary")
    class GlobalSummaryTests {

        @Test
        @DisplayName("Should return global summary across all topics")
        void shouldReturnGlobalSummaryAcrossAllTopics() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowCompleted("order-workflow", "kafka-sample", Duration.ofMillis(1000));
            workflowMetrics.recordWorkflowStarted("payment-workflow", "rabbitmq-sample");
            workflowMetrics.recordWorkflowFailed("payment-workflow", "rabbitmq-sample", Duration.ofMillis(500));
            workflowMetrics.recordRetry("order-workflow", 1);
            workflowMetrics.recordDlq("payment-workflow");

            MetricsSummary summary = workflowMetrics.getGlobalSummary();

            assertThat(summary.getTopic()).isEqualTo("_global");
            assertThat(summary.getWorkflowsStarted()).isEqualTo(3);
            assertThat(summary.getWorkflowsCompleted()).isEqualTo(1);
            assertThat(summary.getWorkflowsFailed()).isEqualTo(1);
            assertThat(summary.getRetryCount()).isEqualTo(1);
            assertThat(summary.getDlqCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate success rate correctly")
        void shouldCalculateSuccessRateCorrectly() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowCompleted("order-workflow", "kafka-sample", Duration.ofMillis(1000));

            MetricsSummary summary = workflowMetrics.getGlobalSummary();

            assertThat(summary.getSuccessRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return zero success rate when no workflows started")
        void shouldReturnZeroSuccessRateWhenNoWorkflowsStarted() {
            MetricsSummary summary = workflowMetrics.getGlobalSummary();

            assertThat(summary.getSuccessRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getActiveTopics")
    class GetActiveTopicsTests {

        @Test
        @DisplayName("Should return set of active topics")
        void shouldReturnSetOfActiveTopics() {
            workflowMetrics.recordWorkflowStarted("order-workflow", "kafka-sample");
            workflowMetrics.recordWorkflowStarted("payment-workflow", "rabbitmq-sample");

            Set<String> topics = workflowMetrics.getActiveTopics();

            assertThat(topics).isNotEmpty();
        }

        @Test
        @DisplayName("Should return empty set when no workflows recorded")
        void shouldReturnEmptySetWhenNoWorkflowsRecorded() {
            Set<String> topics = workflowMetrics.getActiveTopics();

            assertThat(topics).isEmpty();
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should support topic-only recordWorkflowStarted")
        void shouldSupportTopicOnlyRecordWorkflowStarted() {
            workflowMetrics.recordWorkflowStarted("order-workflow");

            Counter counter = meterRegistry.find("stepprflow.workflow.started")
                    .tag("topic", "order-workflow")
                    .tag("service", "unknown")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should support topic-only recordWorkflowCompleted")
        void shouldSupportTopicOnlyRecordWorkflowCompleted() {
            workflowMetrics.recordWorkflowStarted("order-workflow");
            workflowMetrics.recordWorkflowCompleted("order-workflow", Duration.ofMillis(1000));

            Counter counter = meterRegistry.find("stepprflow.workflow.completed")
                    .tag("topic", "order-workflow")
                    .tag("service", "unknown")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }
    }
}