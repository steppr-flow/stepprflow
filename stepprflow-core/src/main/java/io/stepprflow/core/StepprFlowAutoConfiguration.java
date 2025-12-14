package io.stepprflow.core;

import io.stepprflow.core.service.StepExecutor;
import io.stepprflow.core.service.WorkflowRegistry;
import io.stepprflow.core.service.WorkflowStarterImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for StepprFlow core components.
 * This configures the core workflow infrastructure.
 * Broker-specific configurations (Kafka, RabbitMQ) are in separate modules.
 */
@AutoConfiguration
@EnableConfigurationProperties(StepprFlowProperties.class)
@ConditionalOnProperty(prefix = "stepprflow", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import({
        WorkflowRegistry.class,
        StepExecutor.class,
        WorkflowStarterImpl.class
})
@ComponentScan(basePackages = "io.stepprflow.core")
public class StepprFlowAutoConfiguration {
}
