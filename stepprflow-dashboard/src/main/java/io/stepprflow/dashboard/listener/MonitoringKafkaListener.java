package io.stepprflow.dashboard.listener;

import io.stepprflow.core.event.WorkflowMessageEvent;
import io.stepprflow.core.model.WorkflowMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka listener for the monitoring dashboard.
 *
 * This listener only receives workflow messages for monitoring purposes.
 * It does NOT execute workflow steps - that is the responsibility of the
 * microservices using stepprflow-agent.
 *
 * The listener publishes WorkflowMessageEvent which are consumed by:
 * - ExecutionPersistenceService (saves to MongoDB)
 * - WorkflowMetricsListener (updates in-memory metrics)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringKafkaListener {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Listen to all workflow topics for monitoring.
     * Uses a different consumer group than the workflow processors.
     */
    @KafkaListener(
            topicPattern = "${stepprflow.kafka.topic-pattern:.*-workflow.*}",
            containerFactory = "workflowKafkaListenerContainerFactory",
            groupId = "${stepprflow.dashboard.consumer.group-id:stepprflow-monitoring}"
    )
    public void onMessage(ConsumerRecord<String, WorkflowMessage> record, Acknowledgment ack) {
        WorkflowMessage message = record.value();

        if (message == null) {
            log.warn("Received null message on topic {}", record.topic());
            ack.acknowledge();
            return;
        }

        log.debug("Monitoring received: topic={}, executionId={}, step={}, status={}",
                record.topic(), message.getExecutionId(), message.getCurrentStep(), message.getStatus());

        // Publish event for monitoring/persistence only
        // The ExecutionPersistenceService and WorkflowMetricsListener will handle it
        eventPublisher.publishEvent(new WorkflowMessageEvent(this, message));

        ack.acknowledge();
    }
}
