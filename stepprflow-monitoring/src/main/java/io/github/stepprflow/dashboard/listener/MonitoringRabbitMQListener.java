package io.github.stepprflow.dashboard.listener;

import io.github.stepprflow.core.StepprFlowProperties;
import io.github.stepprflow.core.event.WorkflowMessageEvent;
import io.github.stepprflow.core.model.WorkflowMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ listener for the monitoring dashboard.
 *
 * <p>This listener binds to the stepprflow exchange with a wildcard routing key (#)
 * to receive copies of ALL workflow messages for monitoring purposes.
 * It does NOT execute workflow steps - that is the responsibility of the
 * microservices using stepprflow-core.</p>
 *
 * <p>The listener uses a dedicated queue ({@code stepprflow.monitoring}) separate
 * from the workflow processing queues, ensuring zero impact on workflow execution.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@EnableConfigurationProperties(StepprFlowProperties.class)
@Slf4j
public class MonitoringRabbitMQListener {

    /** Queue name for monitoring. */
    static final String MONITORING_QUEUE = "stepprflow.monitoring";

    private final ApplicationEventPublisher eventPublisher;
    private final MessageConverter messageConverter;

    /**
     * Creates the monitoring RabbitMQ listener.
     *
     * @param eventPublisher the event publisher
     * @param messageConverter the message converter
     */
    public MonitoringRabbitMQListener(ApplicationEventPublisher eventPublisher,
                                      MessageConverter messageConverter) {
        this.eventPublisher = eventPublisher;
        this.messageConverter = messageConverter;
    }

    /**
     * Declares the monitoring queue bound to the stepprflow exchange.
     *
     * @param rabbitAdmin the RabbitMQ admin
     * @param properties the stepprflow properties
     * @return the monitoring queue binding
     */
    @Bean
    public Binding monitoringQueueBinding(RabbitAdmin rabbitAdmin, StepprFlowProperties properties) {
        String exchangeName = properties.getRabbitmq().getExchange();

        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        rabbitAdmin.declareExchange(exchange);

        Queue monitoringQueue = QueueBuilder.durable(MONITORING_QUEUE).build();
        rabbitAdmin.declareQueue(monitoringQueue);

        Binding binding = BindingBuilder.bind(monitoringQueue).to(exchange).with("#");
        rabbitAdmin.declareBinding(binding);

        log.info("Declared monitoring queue '{}' bound to exchange '{}' with routing key '#'",
                MONITORING_QUEUE, exchangeName);

        return binding;
    }

    /**
     * Listen to all workflow messages on the monitoring queue.
     *
     * @param message the AMQP message
     */
    @RabbitListener(queues = MONITORING_QUEUE)
    public void onMessage(Message message) {
        try {
            WorkflowMessage workflowMessage = (WorkflowMessage) messageConverter.fromMessage(message);

            if (workflowMessage == null) {
                log.warn("Received null message on monitoring queue");
                return;
            }

            log.debug("Monitoring received: queue={}, executionId={}, step={}, status={}",
                    message.getMessageProperties().getConsumerQueue(),
                    workflowMessage.getExecutionId(),
                    workflowMessage.getCurrentStep(),
                    workflowMessage.getStatus());

            eventPublisher.publishEvent(new WorkflowMessageEvent(this, workflowMessage));

        } catch (Exception e) {
            log.error("Error processing monitoring message: {}", e.getMessage(), e);
        }
    }
}
