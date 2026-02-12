package io.github.stepprflow.core.registration;

import io.github.stepprflow.core.broker.MessageBroker;
import io.github.stepprflow.core.service.WorkflowRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for workflow registration via the message broker.
 *
 * <p>Activated only when a {@link MessageBroker} bean is available.
 * Registers workflow definitions at startup and maintains a heartbeat.
 */
@AutoConfiguration
@ConditionalOnBean(MessageBroker.class)
@EnableConfigurationProperties(RegistrationProperties.class)
@EnableScheduling
public class RegistrationAutoConfiguration {

    /**
     * Creates the workflow registration client bean.
     *
     * @param properties       the registration properties
     * @param workflowRegistry the workflow registry
     * @param messageBroker    the message broker
     * @param appName          the application name
     * @param serverPort       the server port
     * @return the registration client
     */
    @Bean
    public WorkflowRegistrationClient workflowRegistrationClient(
            final RegistrationProperties properties,
            final WorkflowRegistry workflowRegistry,
            final MessageBroker messageBroker,
            @Value("${spring.application.name:unknown}") final String appName,
            @Value("${server.port:8080}") final int serverPort) {
        return new WorkflowRegistrationClient(
                properties, workflowRegistry, messageBroker, appName, serverPort);
    }
}
