package io.stepprflow.broker.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.service.WorkflowRegistry;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import io.stepprflow.core.service.StepExecutor;

/**
 * Auto-configuration for RabbitMQ message broker.
 * Activated when stepprflow.broker=rabbitmq.
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(name = "stepprflow.broker", havingValue = "rabbitmq")
@EnableConfigurationProperties(StepprFlowProperties.class)
public class RabbitMQBrokerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory rabbitConnectionFactory(StepprFlowProperties properties) {
        StepprFlowProperties.RabbitMQ rabbitProps = properties.getRabbitmq();

        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(rabbitProps.getHost());
        factory.setPort(rabbitProps.getPort());
        factory.setUsername(rabbitProps.getUsername());
        factory.setPassword(rabbitProps.getPassword());
        factory.setVirtualHost(rabbitProps.getVirtualHost());

        // Enable publisher confirms for synchronous sends
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);

        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(name = "workflowRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory workflowRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            StepprFlowProperties properties) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(properties.getRabbitmq().getPrefetchCount());
        factory.setDefaultRequeueRejected(false);

        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    @org.springframework.context.annotation.DependsOn("workflowRegistry")
    public RabbitMQQueueInitializer rabbitMQQueueInitializer(
            WorkflowRegistry workflowRegistry,
            RabbitAdmin rabbitAdmin,
            StepprFlowProperties properties) {
        return new RabbitMQQueueInitializer(workflowRegistry, rabbitAdmin, properties);
    }

    @Bean
    @ConditionalOnMissingBean(MessageBroker.class)
    public MessageBroker messageBroker(RabbitTemplate rabbitTemplate,
                                        StepprFlowProperties properties,
                                        MessageConverter messageConverter) {
        return new RabbitMQMessageBroker(
                rabbitTemplate,
                properties.getRabbitmq().getExchange(),
                messageConverter
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMQMessageListener rabbitMQMessageListener(
            StepExecutor stepExecutor,
            WorkflowRegistry workflowRegistry,
            MessageConverter messageConverter,
            ApplicationEventPublisher eventPublisher) {
        return new RabbitMQMessageListener(stepExecutor, workflowRegistry, messageConverter, eventPublisher);
    }
}
