package io.stepprflow.broker.rabbitmq;

import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.service.WorkflowRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 * Initializes RabbitMQ queues, exchanges and bindings for workflows.
 * Uses @PostConstruct to initialize after WorkflowRegistry has scanned workflows
 * (via @DependsOn in auto-configuration).
 */
@Slf4j
public class RabbitMQQueueInitializer {

    private final WorkflowRegistry workflowRegistry;
    private final RabbitAdmin rabbitAdmin;
    private final StepprFlowProperties properties;

    @Getter
    private final List<String> workflowQueueNames = new ArrayList<>();

    public RabbitMQQueueInitializer(WorkflowRegistry workflowRegistry,
                                     RabbitAdmin rabbitAdmin,
                                     StepprFlowProperties properties) {
        this.workflowRegistry = workflowRegistry;
        this.rabbitAdmin = rabbitAdmin;
        this.properties = properties;
    }

    /**
     * Initialize queues. Called after WorkflowRegistry is populated.
     * Must run before RabbitListener containers start.
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        initializeQueues();
    }

    private void initializeQueues() {
        String exchangeName = properties.getRabbitmq().getExchange();
        String dlqSuffix = properties.getRabbitmq().getDlqSuffix();

        log.info("Initializing RabbitMQ infrastructure for workflows...");

        // Create the main exchange (topic exchange for flexible routing)
        TopicExchange exchange = new TopicExchange(exchangeName, true, false);
        rabbitAdmin.declareExchange(exchange);
        log.info("Declared exchange: {}", exchangeName);

        // Create DLQ exchange
        String dlqExchangeName = exchangeName + ".dlq";
        DirectExchange dlqExchange = new DirectExchange(dlqExchangeName, true, false);
        rabbitAdmin.declareExchange(dlqExchange);
        log.info("Declared DLQ exchange: {}", dlqExchangeName);

        // Create queues for each registered workflow
        List<String> topics = workflowRegistry.getTopics();
        for (String topic : topics) {
            createWorkflowQueues(topic, exchange, dlqExchange, exchangeName, dlqSuffix);
        }

        // Add a default fallback queue if no workflows are registered
        if (workflowQueueNames.isEmpty()) {
            workflowQueueNames.add("stepprflow-no-workflows");
            Queue fallbackQueue = QueueBuilder.durable("stepprflow-no-workflows").build();
            rabbitAdmin.declareQueue(fallbackQueue);
        }

        log.info("Initialized {} workflow queue(s)", workflowQueueNames.size());
    }

    private void createWorkflowQueues(String topic, TopicExchange exchange,
                                       DirectExchange dlqExchange,
                                       String exchangeName, String dlqSuffix) {
        // Main workflow queue
        String queueName = topic;
        String dlqQueueName = topic + dlqSuffix;
        String retryQueueName = topic + ".retry";
        String completedQueueName = topic + ".completed";

        // Create main queue with DLQ configuration
        Queue mainQueue = QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchangeName + ".dlq")
                .withArgument("x-dead-letter-routing-key", dlqQueueName)
                .build();
        rabbitAdmin.declareQueue(mainQueue);
        workflowQueueNames.add(queueName);
        log.debug("Declared queue: {}", queueName);

        // Bind main queue to exchange
        Binding mainBinding = BindingBuilder.bind(mainQueue)
                .to(exchange)
                .with(topic);
        rabbitAdmin.declareBinding(mainBinding);

        // Create DLQ
        Queue dlqQueue = QueueBuilder.durable(dlqQueueName).build();
        rabbitAdmin.declareQueue(dlqQueue);
        log.debug("Declared DLQ: {}", dlqQueueName);

        // Bind DLQ
        Binding dlqBinding = BindingBuilder.bind(dlqQueue)
                .to(dlqExchange)
                .with(dlqQueueName);
        rabbitAdmin.declareBinding(dlqBinding);

        // Create retry queue with TTL for delayed reprocessing
        Queue retryQueue = QueueBuilder.durable(retryQueueName)
                .withArgument("x-dead-letter-exchange", exchangeName)
                .withArgument("x-dead-letter-routing-key", topic)
                .build();
        rabbitAdmin.declareQueue(retryQueue);
        workflowQueueNames.add(retryQueueName);
        log.debug("Declared retry queue: {}", retryQueueName);

        // Bind retry queue
        Binding retryBinding = BindingBuilder.bind(retryQueue)
                .to(exchange)
                .with(topic + ".retry");
        rabbitAdmin.declareBinding(retryBinding);

        // Create completed queue
        Queue completedQueue = QueueBuilder.durable(completedQueueName).build();
        rabbitAdmin.declareQueue(completedQueue);
        workflowQueueNames.add(completedQueueName);
        log.debug("Declared completed queue: {}", completedQueueName);

        // Bind completed queue
        Binding completedBinding = BindingBuilder.bind(completedQueue)
                .to(exchange)
                .with(topic + ".completed");
        rabbitAdmin.declareBinding(completedBinding);

        log.info("Created queue infrastructure for workflow: {}", topic);
    }
}
