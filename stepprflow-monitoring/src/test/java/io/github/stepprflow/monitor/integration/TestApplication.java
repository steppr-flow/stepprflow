package io.github.stepprflow.monitor.integration;

import io.github.stepprflow.core.broker.MessageBroker;
import io.github.stepprflow.core.metrics.WorkflowMetrics;
import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.monitor.MonitorProperties;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import io.github.stepprflow.monitor.websocket.WorkflowBroadcaster;
import io.github.stepprflow.monitor.websocket.WorkflowWebSocketHandler.WorkflowUpdateDTO;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Set;

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

    /**
     * Synchronous broadcaster for integration tests.
     * Bypasses @Async to eliminate timing issues with the async executor
     * while keeping the WebSocket infrastructure intact.
     * Only created when WebSocket is enabled (SimpMessagingTemplate is available).
     */
    @Bean
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(SimpMessagingTemplate.class)
    public WorkflowBroadcaster testBroadcaster(SimpMessagingTemplate messagingTemplate,
                                                MonitorProperties properties) {
        var terminalStatuses = Set.of(
                WorkflowStatus.COMPLETED, WorkflowStatus.FAILED, WorkflowStatus.CANCELLED);

        return new WorkflowBroadcaster() {
            @Override
            public void broadcastUpdate(WorkflowExecution execution) {
                String topicPrefix = properties.getWebSocket().getTopicPrefix();
                WorkflowUpdateDTO update = WorkflowUpdateDTO.from(execution);

                messagingTemplate.convertAndSend(topicPrefix + "/updates", update);
                messagingTemplate.convertAndSend(topicPrefix + "/" + execution.getTopic(), update);

                if (terminalStatuses.contains(execution.getStatus())) {
                    messagingTemplate.convertAndSend(
                            topicPrefix + "/execution/" + execution.getExecutionId(), update);
                }
            }

            @Override
            public void sendToUser(String userId, WorkflowExecution execution) {
                messagingTemplate.convertAndSendToUser(
                        userId,
                        properties.getWebSocket().getTopicPrefix() + "/updates",
                        WorkflowUpdateDTO.from(execution));
            }
        };
    }
}