package io.github.stepprflow.monitor.integration;

import io.github.stepprflow.core.broker.MessageBroker;
import io.github.stepprflow.core.metrics.WorkflowMetrics;
import io.github.stepprflow.monitor.MonitorProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.mockito.Mockito.mock;

/**
 * Test application for integration tests.
 * Required because stepprflow-monitor is a library without a main application class.
 */
@SpringBootApplication(scanBasePackages = {"io.github.stepprflow.monitor"})
@EnableConfigurationProperties(MonitorProperties.class)
@EnableScheduling
@EnableAsync
public class TestApplication {

    @Bean
    @Primary
    public MessageBroker messageBroker() {
        return mock(MessageBroker.class);
    }

    @Bean
    @Primary
    public WorkflowMetrics workflowMetrics() {
        return mock(WorkflowMetrics.class);
    }
}