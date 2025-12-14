package io.stepprflow.core.tracing;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for workflow tracing.
 * Automatically configures WorkflowTracing when Micrometer Observation
 * is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
public class TracingAutoConfiguration {

    /**
     * Creates the WorkflowTracing bean.
     *
     * @param observationRegistry the observation registry
     * @return the workflow tracing instance
     */
    @Bean
    @ConditionalOnMissingBean
    public WorkflowTracing workflowTracing(
            final ObservationRegistry observationRegistry) {
        return new WorkflowTracing(observationRegistry);
    }
}
