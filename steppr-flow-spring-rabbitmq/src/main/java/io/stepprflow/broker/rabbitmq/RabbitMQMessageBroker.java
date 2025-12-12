package io.stepprflow.broker.rabbitmq;

import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.WorkflowMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.concurrent.CompletableFuture;

/**
 * RabbitMQ implementation of MessageBroker.
 */
@RequiredArgsConstructor
@Slf4j
public class RabbitMQMessageBroker implements MessageBroker {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final MessageConverter messageConverter;

    @Override
    public void send(String destination, WorkflowMessage message) {
        log.debug("Sending message to RabbitMQ exchange {} with routing key {}: executionId={}, step={}",
                exchange, destination, message.getExecutionId(), message.getCurrentStep());

        try {
            MessageProperties properties = createMessageProperties(message);
            Message amqpMessage = messageConverter.toMessage(message, properties);

            rabbitTemplate.send(exchange, destination, amqpMessage);

            log.debug("Message sent successfully to exchange {} with routing key {}",
                    exchange, destination);
        } catch (Exception e) {
            log.error("Failed to send message to exchange {} with routing key {}: {}",
                    exchange, destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send workflow message", e);
        }
    }

    @Override
    public CompletableFuture<Void> sendAsync(String destination, WorkflowMessage message) {
        log.debug("Sending async message to RabbitMQ exchange {} with routing key {}: executionId={}",
                exchange, destination, message.getExecutionId());

        return CompletableFuture.runAsync(() -> {
            try {
                MessageProperties properties = createMessageProperties(message);
                Message amqpMessage = messageConverter.toMessage(message, properties);

                rabbitTemplate.send(exchange, destination, amqpMessage);

                log.debug("Async message sent to exchange {} with routing key {}",
                        exchange, destination);
            } catch (Exception e) {
                log.error("Failed to send async message to exchange {} with routing key {}: {}",
                        exchange, destination, e.getMessage(), e);
                throw new RuntimeException("Failed to send workflow message", e);
            }
        });
    }

    @Override
    public void sendSync(String destination, WorkflowMessage message) {
        log.debug("Sending sync message to RabbitMQ exchange {} with routing key {}: executionId={}",
                exchange, destination, message.getExecutionId());

        try {
            MessageProperties properties = createMessageProperties(message);
            Message amqpMessage = messageConverter.toMessage(message, properties);

            rabbitTemplate.invoke(operations -> {
                operations.send(exchange, destination, amqpMessage);
                operations.waitForConfirmsOrDie(5000);
                return null;
            });

            log.debug("Sync message sent and confirmed to exchange {} with routing key {}",
                    exchange, destination);
        } catch (Exception e) {
            log.error("Failed to send sync message to exchange {} with routing key {}: {}",
                    exchange, destination, e.getMessage(), e);
            throw new RuntimeException("Failed to send workflow message", e);
        }
    }

    @Override
    public String getBrokerType() {
        return "rabbitmq";
    }

    @Override
    public boolean isAvailable() {
        try (var createdConnection = rabbitTemplate.getConnectionFactory().createConnection()) {
            return createdConnection.isOpen();
        } catch (Exception e) {
            log.warn("RabbitMQ connection check failed: {}", e.getMessage());
            return false;
        }
    }

    private MessageProperties createMessageProperties(WorkflowMessage message) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setMessageId(message.getExecutionId());
        properties.setCorrelationId(message.getCorrelationId());

        if (message.getMetadata() != null) {
            message.getMetadata().forEach((key, value) -> {
                if (value != null) {
                    properties.setHeader(key, value.toString());
                }
            });
        }

        // Add workflow-specific headers
        properties.setHeader("x-workflow-topic", message.getTopic());
        properties.setHeader("x-workflow-step", message.getCurrentStep());
        properties.setHeader("x-workflow-status", message.getStatus().name());

        return properties;
    }
}
