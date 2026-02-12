package io.github.stepprflow.broker.rabbitmq;

import com.rabbitmq.client.Channel;
import io.github.stepprflow.core.broker.MessageContext;
import io.github.stepprflow.core.exception.MessageAcknowledgeException;
import io.github.stepprflow.core.exception.MessageRejectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ implementation of MessageContext.
 */
@Slf4j
public class RabbitMQMessageContext implements MessageContext {

    private final Message message;
    private final Channel channel;
    private final long deliveryTag;
    private final String queueName;

    public RabbitMQMessageContext(Message message, Channel channel, String queueName) {
        this.message = message;
        this.channel = channel;
        this.deliveryTag = message.getMessageProperties().getDeliveryTag();
        this.queueName = queueName;
    }

    @Override
    public String getDestination() {
        return queueName;
    }

    @Override
    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        MessageProperties props = message.getMessageProperties();

        if (props.getHeaders() != null) {
            props.getHeaders().forEach((key, value) -> {
                if (value != null) {
                    headers.put(key, value.toString());
                }
            });
        }

        // Add standard properties as headers
        if (props.getMessageId() != null) {
            headers.put("messageId", props.getMessageId());
        }
        if (props.getCorrelationId() != null) {
            headers.put("correlationId", props.getCorrelationId());
        }
        if (props.getContentType() != null) {
            headers.put("contentType", props.getContentType());
        }

        return headers;
    }

    @Override
    public String getMessageKey() {
        // In RabbitMQ, the routing key serves a similar purpose to Kafka's message key
        return message.getMessageProperties().getReceivedRoutingKey();
    }

    @Override
    public void acknowledge() {
        try {
            channel.basicAck(deliveryTag, false);
            log.debug("Message acknowledged: deliveryTag={}", deliveryTag);
        } catch (IOException e) {
            log.error("Failed to acknowledge message: deliveryTag={}", deliveryTag, e);
            throw new MessageAcknowledgeException("rabbitmq", String.valueOf(deliveryTag),
                    e.getMessage(), e);
        }
    }

    @Override
    public void reject(boolean requeue) {
        try {
            channel.basicReject(deliveryTag, requeue);
            log.debug("Message rejected: deliveryTag={}, requeue={}", deliveryTag, requeue);
        } catch (IOException e) {
            log.error("Failed to reject message: deliveryTag={}", deliveryTag, e);
            throw new MessageRejectException("rabbitmq", String.valueOf(deliveryTag), requeue,
                    e.getMessage(), e);
        }
    }

    @Override
    public String getOffset() {
        // RabbitMQ uses delivery tags instead of offsets
        return String.valueOf(deliveryTag);
    }

    /**
     * Get the original AMQP message.
     */
    public Message getOriginalMessage() {
        return message;
    }

    /**
     * Get the RabbitMQ channel.
     */
    public Channel getChannel() {
        return channel;
    }
}
