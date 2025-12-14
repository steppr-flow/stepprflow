package io.stepprflow.monitor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.micrometer.core.instrument.MeterRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.metrics.WorkflowMetrics;
import io.stepprflow.core.metrics.WorkflowMetricsListener;
import io.stepprflow.monitor.config.OpenApiConfig;
import io.stepprflow.monitor.config.WebSocketConfig;
import io.stepprflow.monitor.controller.CircuitBreakerController;
import io.stepprflow.monitor.controller.GlobalExceptionHandler;
import io.stepprflow.monitor.controller.MetricsController;
import io.stepprflow.monitor.controller.WorkflowController;
import io.stepprflow.monitor.outbox.OutboxMessageRepository;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import io.stepprflow.monitor.outbox.OutboxRelayService;
import io.stepprflow.monitor.outbox.OutboxService;
import io.stepprflow.monitor.service.ExecutionPersistenceService;
import io.stepprflow.monitor.service.PayloadManagementService;
import io.stepprflow.monitor.service.RetrySchedulerService;
import io.stepprflow.monitor.service.WorkflowCommandService;
import io.stepprflow.monitor.service.WorkflowQueryService;
import io.stepprflow.monitor.service.WorkflowRegistryService;
import io.stepprflow.monitor.util.WorkflowMessageFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Auto-configuration for StepprFlow Monitor module.
 * Runs before Spring Boot's MongoDB auto-configuration to use StepprFlow properties.
 */
@AutoConfiguration(before = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableConfigurationProperties({MonitorProperties.class, StepprFlowProperties.class})
@ConditionalOnProperty(prefix = "stepprflow.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableMongoRepositories(basePackageClasses = {WorkflowExecutionRepository.class, OutboxMessageRepository.class})
@EnableScheduling
@EnableAsync
@Import({
        OpenApiConfig.class,
        WebSocketConfig.class,
        GlobalExceptionHandler.class,
        WorkflowController.class,
        CircuitBreakerController.class,
        ExecutionPersistenceService.class,
        RetrySchedulerService.class,
        WorkflowQueryService.class,
        WorkflowCommandService.class,
        PayloadManagementService.class,
        WorkflowRegistryService.class,
        WorkflowMessageFactory.class,
        OutboxService.class,
        OutboxRelayService.class
})
public class MonitorAutoConfiguration {

    /**
     * Creates MongoClient using StepprFlow properties.
     */
    @Bean
    @Primary
    public MongoClient mongoClient(StepprFlowProperties properties) {
        return MongoClients.create(properties.getMongodb().getUri());
    }

    /**
     * Creates MongoDatabaseFactory using StepprFlow properties.
     */
    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient, StepprFlowProperties properties) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, properties.getMongodb().getDatabase());
    }

    /**
     * Creates WorkflowMetrics bean.
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowMetrics.class)
    public WorkflowMetrics workflowMetrics(MeterRegistry meterRegistry) {
        return new WorkflowMetrics(meterRegistry);
    }

    /**
     * Creates WorkflowMetricsListener.
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowMetricsListener.class)
    public WorkflowMetricsListener workflowMetricsListener(WorkflowMetrics workflowMetrics) {
        return new WorkflowMetricsListener(workflowMetrics);
    }

    /**
     * Creates MetricsController.
     */
    @Bean
    public MetricsController metricsController(WorkflowMetrics workflowMetrics) {
        return new MetricsController(workflowMetrics);
    }
}
