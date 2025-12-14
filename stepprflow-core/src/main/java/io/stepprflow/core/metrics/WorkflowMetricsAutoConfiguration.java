package io.stepprflow.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for StepprFlow workflow metrics.
 * Configures WorkflowMetrics bean only when MeterRegistry is available.
 * Runs after metrics auto-configuration to ensure MeterRegistry exists.
 */
@AutoConfiguration(afterName = "org.springframework.boot.actuate"
        + ".autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass(MeterRegistry.class)
public class WorkflowMetricsAutoConfiguration {

    /**
     * Creates the WorkflowMetrics bean.
     *
     * @param meterRegistry the meter registry
     * @return the workflow metrics instance
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public WorkflowMetrics workflowMetrics(final MeterRegistry meterRegistry) {
        return new WorkflowMetrics(meterRegistry);
    }

    /**
     * Creates the WorkflowMetricsListener bean.
     *
     * @param workflowMetrics the workflow metrics instance
     * @return the workflow metrics listener instance
     */
    @Bean
    @ConditionalOnBean(WorkflowMetrics.class)
    public WorkflowMetricsListener workflowMetricsListener(
            final WorkflowMetrics workflowMetrics) {
        return new WorkflowMetricsListener(workflowMetrics);
    }
}
