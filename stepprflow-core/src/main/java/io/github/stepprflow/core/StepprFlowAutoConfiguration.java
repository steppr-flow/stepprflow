package io.github.stepprflow.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.stepprflow.core.security.NoOpSecurityContextPropagator;
import io.github.stepprflow.core.security.SecurityContextPropagator;
import io.github.stepprflow.core.service.StepExecutor;
import io.github.stepprflow.core.service.WorkflowRegistry;
import io.github.stepprflow.core.service.WorkflowStarterImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
@ComponentScan(basePackages = "io.github.stepprflow.core")
public class StepprFlowAutoConfiguration {

    /**
     * Default security context propagator (no-op).
     * Users can provide their own implementation to propagate security context.
     *
     * @return the default no-op security context propagator
     */
    @Bean
    @ConditionalOnMissingBean(SecurityContextPropagator.class)
    public SecurityContextPropagator securityContextPropagator() {
        return new NoOpSecurityContextPropagator();
    }

    /**
     * ObjectMapper configured for workflow payload serialization/deserialization.
     * This mapper is lenient to handle domain objects with computed properties
     * (getters without setters) without requiring Jackson annotations.
     *
     * @return the stepprflow ObjectMapper
     */
    @Bean("stepprflowObjectMapper")
    public ObjectMapper stepprflowObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Ignore properties in JSON that don't have setters (computed getters)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
