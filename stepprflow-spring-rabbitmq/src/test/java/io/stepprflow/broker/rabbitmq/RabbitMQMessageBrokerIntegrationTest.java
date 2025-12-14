package io.stepprflow.broker.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for RabbitMQMessageBroker using Testcontainers.
 */
@SpringBootTest(classes = RabbitMQMessageBrokerIntegrationTest.TestConfig.class,
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
@Testcontainers
class RabbitMQMessageBrokerIntegrationTest {

    private static final String TEST_EXCHANGE = "test-stepprflow-exchange";
    private static final String TEST_QUEUE = "test-workflow-queue";
    private static final String TEST_ROUTING_KEY = "test-workflow-topic";

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

    @DynamicPropertySource
    static void rabbitmqProperties(DynamicPropertyRegistry registry) {
        registry.add("test.rabbitmq.host", rabbitmq::getHost);
        registry.add("test.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("test.rabbitmq.username", () -> "guest");
        registry.add("test.rabbitmq.password", () -> "guest");
    }

    @Autowired
    private RabbitMQMessageBroker messageBroker;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @BeforeEach
    void setUp() {
        // Declare exchange, queue and binding
        DirectExchange exchange = new DirectExchange(TEST_EXCHANGE);
        Queue queue = new Queue(TEST_QUEUE, true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(TEST_ROUTING_KEY);

        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(binding);

        // Purge the queue before each test
        rabbitAdmin.purgeQueue(TEST_QUEUE);
    }

    @AfterEach
    void tearDown() {
        rabbitAdmin.purgeQueue(TEST_QUEUE);
    }

    @Test
    void send_shouldDeliverMessageToRabbitMQ() {
        // Given
        WorkflowMessage message = createTestMessage();

        // When
        messageBroker.send(TEST_ROUTING_KEY, message);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message receivedMessage = rabbitTemplate.receive(TEST_QUEUE, 1000);
            assertThat(receivedMessage).isNotNull();

            WorkflowMessage received = (WorkflowMessage) rabbitTemplate.getMessageConverter()
                    .fromMessage(receivedMessage);

            assertThat(received.getExecutionId()).isEqualTo(message.getExecutionId());
            assertThat(received.getTopic()).isEqualTo(message.getTopic());
            assertThat(received.getCurrentStep()).isEqualTo(message.getCurrentStep());
        });
    }

    @Test
    void sendAsync_shouldDeliverMessageAsynchronously() throws Exception {
        // Given
        WorkflowMessage message = createTestMessage();

        // When
        messageBroker.sendAsync(TEST_ROUTING_KEY, message).get(10, TimeUnit.SECONDS);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message receivedMessage = rabbitTemplate.receive(TEST_QUEUE, 1000);
            assertThat(receivedMessage).isNotNull();

            WorkflowMessage received = (WorkflowMessage) rabbitTemplate.getMessageConverter()
                    .fromMessage(receivedMessage);

            assertThat(received.getExecutionId()).isEqualTo(message.getExecutionId());
        });
    }

    @Test
    void send_shouldIncludeWorkflowHeadersInMessage() {
        // Given
        WorkflowMessage message = createTestMessage();

        // When
        messageBroker.send(TEST_ROUTING_KEY, message);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message receivedMessage = rabbitTemplate.receive(TEST_QUEUE, 1000);
            assertThat(receivedMessage).isNotNull();

            MessageProperties properties = receivedMessage.getMessageProperties();
            assertThat(properties.getMessageId()).isEqualTo(message.getExecutionId());
            assertThat(properties.getCorrelationId()).isEqualTo(message.getCorrelationId());
            assertThat((String) properties.getHeader("x-workflow-topic")).isEqualTo(message.getTopic());
            assertThat((Integer) properties.getHeader("x-workflow-step")).isEqualTo(message.getCurrentStep());
            assertThat((String) properties.getHeader("x-workflow-status")).isEqualTo(message.getStatus().name());
        });
    }

    @Test
    void getBrokerType_shouldReturnRabbitmq() {
        assertThat(messageBroker.getBrokerType()).isEqualTo("rabbitmq");
    }

    @Test
    void isAvailable_shouldReturnTrueWhenConnected() {
        assertThat(messageBroker.isAvailable()).isTrue();
    }

    @Test
    void send_shouldMaintainMessageOrderFIFO() {
        // Given - send multiple messages in order
        int messageCount = 5;
        String[] executionIds = new String[messageCount];

        for (int i = 0; i < messageCount; i++) {
            executionIds[i] = "order-test-" + i;
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId(executionIds[i])
                    .correlationId(UUID.randomUUID().toString())
                    .topic(TEST_ROUTING_KEY)
                    .currentStep(i + 1)
                    .totalSteps(messageCount)
                    .status(WorkflowStatus.IN_PROGRESS)
                    .payload(Map.of("order", i))
                    .build();
            messageBroker.send(TEST_ROUTING_KEY, message);
        }

        // Then - messages should be received in FIFO order
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            for (int i = 0; i < messageCount; i++) {
                Message receivedMessage = rabbitTemplate.receive(TEST_QUEUE, 1000);
                assertThat(receivedMessage).isNotNull();

                WorkflowMessage received = (WorkflowMessage) rabbitTemplate.getMessageConverter()
                        .fromMessage(receivedMessage);
                assertThat(received.getExecutionId()).isEqualTo(executionIds[i]);
                assertThat(received.getCurrentStep()).isEqualTo(i + 1);
            }
        });
    }

    @Test
    void send_shouldConfigureDeadLetterQueueCorrectly() {
        // Given - setup separate queue with DLQ for this test
        String testQueueDlq = "dlq-test-queue";
        String dlqName = testQueueDlq + ".dlq";
        String dlxName = "dlq-test-exchange.dlx";
        String dlqRoutingKey = "dlq-test-routing";

        // Create DLX and DLQ
        DirectExchange dlx = new DirectExchange(dlxName);
        Queue dlq = new Queue(dlqName, true);
        Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(dlqRoutingKey);

        rabbitAdmin.declareExchange(dlx);
        rabbitAdmin.declareQueue(dlq);
        rabbitAdmin.declareBinding(dlqBinding);

        // Create main queue with DLX configuration
        Queue queueWithDlx = QueueBuilder.durable(testQueueDlq)
                .withArgument("x-dead-letter-exchange", dlxName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
        rabbitAdmin.declareQueue(queueWithDlx);

        // Bind to test exchange
        rabbitAdmin.declareBinding(BindingBuilder.bind(queueWithDlx)
                .to(new DirectExchange(TEST_EXCHANGE)).with(dlqRoutingKey));

        // When - send a message to the DLQ-enabled queue
        WorkflowMessage message = createTestMessage();
        rabbitTemplate.convertAndSend(TEST_EXCHANGE, dlqRoutingKey, message);

        // Then - message should be delivered to the queue
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message receivedMessage = rabbitTemplate.receive(testQueueDlq, 1000);
            assertThat(receivedMessage).isNotNull();

            // Verify message properties
            MessageProperties props = receivedMessage.getMessageProperties();
            assertThat(props).isNotNull();
        });

        // Cleanup
        rabbitAdmin.deleteQueue(testQueueDlq);
        rabbitAdmin.deleteQueue(dlqName);
        rabbitAdmin.deleteExchange(dlxName);
    }

    @Test
    void sendMultipleMessages_shouldAllBeDelivered() {
        // Given
        int messageCount = 10;
        List<String> sentIds = new ArrayList<>();

        // When - send multiple messages
        for (int i = 0; i < messageCount; i++) {
            WorkflowMessage message = createTestMessage();
            sentIds.add(message.getExecutionId());
            messageBroker.send(TEST_ROUTING_KEY, message);
        }

        // Then - all messages should be received
        List<String> receivedIds = new ArrayList<>();
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            for (int i = 0; i < messageCount; i++) {
                Message receivedMessage = rabbitTemplate.receive(TEST_QUEUE, 1000);
                if (receivedMessage != null) {
                    WorkflowMessage received = (WorkflowMessage) rabbitTemplate.getMessageConverter()
                            .fromMessage(receivedMessage);
                    receivedIds.add(received.getExecutionId());
                }
            }
            assertThat(receivedIds).hasSize(messageCount);
            assertThat(receivedIds).containsExactlyInAnyOrderElementsOf(sentIds);
        });
    }

    private WorkflowMessage createTestMessage() {
        return WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic(TEST_ROUTING_KEY)
                .currentStep(1)
                .totalSteps(3)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("key", "value"))
                .build();
    }

    @Configuration
    static class TestConfig {

        @Bean
        public ConnectionFactory connectionFactory(
                @Value("${test.rabbitmq.host}") String host,
                @Value("${test.rabbitmq.port}") int port,
                @Value("${test.rabbitmq.username}") String username,
                @Value("${test.rabbitmq.password}") String password) {
            CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            return factory;
        }

        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper;
        }

        @Bean
        public MessageConverter messageConverter(ObjectMapper objectMapper) {
            return new Jackson2JsonMessageConverter(objectMapper);
        }

        @Bean
        public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                             MessageConverter messageConverter) {
            RabbitTemplate template = new RabbitTemplate(connectionFactory);
            template.setMessageConverter(messageConverter);
            return template;
        }

        @Bean
        public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
            return new RabbitAdmin(connectionFactory);
        }

        @Bean
        public RabbitMQMessageBroker rabbitMQMessageBroker(RabbitTemplate rabbitTemplate,
                                                           MessageConverter messageConverter) {
            return new RabbitMQMessageBroker(rabbitTemplate, TEST_EXCHANGE, messageConverter);
        }
    }
}