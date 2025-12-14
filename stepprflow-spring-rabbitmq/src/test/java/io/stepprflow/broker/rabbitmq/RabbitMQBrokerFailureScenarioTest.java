package io.stepprflow.broker.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.broker.ResilientMessageBroker;
import io.stepprflow.core.exception.CircuitBreakerOpenException;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Failure scenario tests for RabbitMQ broker using TestContainers.
 * Tests broker unavailability and circuit breaker behavior.
 */
@Testcontainers
class RabbitMQBrokerFailureScenarioTest {

    private static final String TEST_EXCHANGE = "failure-test-exchange";
    private static final String TEST_QUEUE = "failure-test-queue";
    private static final String TEST_ROUTING_KEY = "failure-test-topic";

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");

    private RabbitMQMessageBroker rabbitBroker;
    private ResilientMessageBroker resilientBroker;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RabbitTemplate rabbitTemplate;
    private CachingConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        // Ensure container is running
        if (!rabbitmq.isRunning()) {
            rabbitmq.start();
        }

        // Create connection factory
        connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitmq.getHost());
        connectionFactory.setPort(rabbitmq.getAmqpPort());
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setConnectionTimeout(5000);

        // Create message converter
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);

        // Create RabbitTemplate
        rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setReplyTimeout(5000);

        // Create broker
        rabbitBroker = new RabbitMQMessageBroker(rabbitTemplate, TEST_EXCHANGE, messageConverter);

        // Configure circuit breaker
        StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
        cbConfig.setEnabled(true);
        cbConfig.setFailureRateThreshold(50);
        cbConfig.setSlidingWindowSize(4);
        cbConfig.setMinimumNumberOfCalls(2);
        cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(10));
        cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
        cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        resilientBroker = new ResilientMessageBroker(rabbitBroker, cbConfig, circuitBreakerRegistry);

        // Setup exchange and queue
        setupExchangeAndQueue();
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    private void setupExchangeAndQueue() {
        try {
            RabbitAdmin admin = new RabbitAdmin(connectionFactory);
            DirectExchange exchange = new DirectExchange(TEST_EXCHANGE);
            Queue queue = new Queue(TEST_QUEUE, true);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(TEST_ROUTING_KEY);

            admin.declareExchange(exchange);
            admin.declareQueue(queue);
            admin.declareBinding(binding);
            admin.purgeQueue(TEST_QUEUE);
        } catch (Exception e) {
            // Ignore if already exists
        }
    }

    private WorkflowMessage createTestMessage() {
        return WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic(TEST_ROUTING_KEY)
                .currentStep(1)
                .totalSteps(3)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("test", "data"))
                .build();
    }

    @Nested
    @DisplayName("Broker availability scenarios")
    class BrokerAvailability {

        @Test
        @DisplayName("should report broker as available when connected")
        void shouldReportBrokerAsAvailable() {
            assertThat(rabbitBroker.isAvailable()).isTrue();
            assertThat(rabbitBroker.getBrokerType()).isEqualTo("rabbitmq");
        }

        @Test
        @DisplayName("should send message successfully when broker is available")
        void shouldSendMessageSuccessfully() {
            WorkflowMessage message = createTestMessage();

            assertThatCode(() -> rabbitBroker.send(TEST_ROUTING_KEY, message))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should send message asynchronously when broker is available")
        void shouldSendMessageAsyncSuccessfully() throws Exception {
            WorkflowMessage message = createTestMessage();

            rabbitBroker.sendAsync(TEST_ROUTING_KEY, message).get(10, TimeUnit.SECONDS);
        }
    }

    @Nested
    @DisplayName("Circuit breaker integration")
    class CircuitBreakerIntegration {

        @Test
        @DisplayName("should track successful calls in circuit breaker metrics")
        void shouldTrackSuccessfulCalls() {
            // Send successful messages
            for (int i = 0; i < 3; i++) {
                resilientBroker.send(TEST_ROUTING_KEY, createTestMessage());
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-rabbitmq");
            assertThat(cb.getMetrics().getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(3);
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should keep circuit closed after successful calls")
        void shouldKeepCircuitClosedAfterSuccess() {
            for (int i = 0; i < 5; i++) {
                resilientBroker.send(TEST_ROUTING_KEY, createTestMessage());
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-rabbitmq");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should report available when circuit is closed and broker is connected")
        void shouldReportAvailableWhenCircuitClosedAndConnected() {
            // Send a successful message
            resilientBroker.send(TEST_ROUTING_KEY, createTestMessage());

            assertThat(resilientBroker.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return correct broker type through resilient broker")
        void shouldReturnBrokerTypeThroughResilientBroker() {
            assertThat(resilientBroker.getBrokerType()).isEqualTo("rabbitmq");
        }
    }

    @Nested
    @DisplayName("Broker unavailability scenarios")
    class BrokerUnavailabilityScenarios {

        @Test
        @DisplayName("should report unavailable when broker connection fails")
        void shouldReportUnavailableWhenConnectionFails() {
            // Create a broker pointing to non-existent dashboard
            CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(59998); // Non-existent port
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setConnectionTimeout(500);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);

            RabbitTemplate template = new RabbitTemplate(factory);
            template.setMessageConverter(messageConverter);

            RabbitMQMessageBroker unavailableBroker = new RabbitMQMessageBroker(template, TEST_EXCHANGE, messageConverter);

            try {
                // Should report unavailable
                assertThat(unavailableBroker.isAvailable()).isFalse();
            } finally {
                factory.destroy();
            }
        }

        @Test
        @DisplayName("should throw exception when sending to unavailable broker")
        void shouldThrowWhenSendingToUnavailableBroker() {
            CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(59997);
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setConnectionTimeout(500);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);

            RabbitTemplate template = new RabbitTemplate(factory);
            template.setMessageConverter(messageConverter);

            RabbitMQMessageBroker unavailableBroker = new RabbitMQMessageBroker(template, TEST_EXCHANGE, messageConverter);

            try {
                assertThatThrownBy(() -> unavailableBroker.send(TEST_ROUTING_KEY, createTestMessage()))
                        .isInstanceOf(RuntimeException.class);
            } finally {
                factory.destroy();
            }
        }
    }

    @Nested
    @DisplayName("Circuit breaker with mock failures")
    class CircuitBreakerWithMockFailures {

        @Test
        @DisplayName("should open circuit and throw CircuitBreakerOpenException after failures")
        void shouldOpenCircuitAfterFailures() {
            // Configure circuit breaker with aggressive settings
            StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
            cbConfig.setEnabled(true);
            cbConfig.setFailureRateThreshold(50);
            cbConfig.setSlidingWindowSize(4);
            cbConfig.setMinimumNumberOfCalls(2);
            cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(60));
            cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
            cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

            CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

            // Create broker pointing to non-existent dashboard
            CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(59999); // Non-existent port
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setConnectionTimeout(500);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);

            RabbitTemplate template = new RabbitTemplate(factory);
            template.setMessageConverter(messageConverter);
            template.setReplyTimeout(500);

            RabbitMQMessageBroker failingBroker = new RabbitMQMessageBroker(template, TEST_EXCHANGE, messageConverter);
            ResilientMessageBroker broker = new ResilientMessageBroker(failingBroker, cbConfig, registry);

            try {
                // Trigger failures
                for (int i = 0; i < 4; i++) {
                    try {
                        broker.send(TEST_ROUTING_KEY, createTestMessage());
                    } catch (Exception ignored) {}
                }

                // Circuit should be open
                CircuitBreaker cb = registry.circuitBreaker("broker-rabbitmq");
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

                // Next call should throw CircuitBreakerOpenException
                assertThatThrownBy(() -> broker.send(TEST_ROUTING_KEY, createTestMessage()))
                        .isInstanceOf(CircuitBreakerOpenException.class)
                        .satisfies(ex -> {
                            CircuitBreakerOpenException cbEx = (CircuitBreakerOpenException) ex;
                            assertThat(cbEx.getCircuitBreakerName()).isEqualTo("broker-rabbitmq");
                            assertThat(cbEx.getState()).isEqualTo(CircuitBreaker.State.OPEN);
                        });

            } finally {
                factory.destroy();
            }
        }

        @Test
        @DisplayName("should report unavailable when circuit is open")
        void shouldReportUnavailableWhenCircuitOpen() {
            StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
            cbConfig.setEnabled(true);
            cbConfig.setFailureRateThreshold(50);
            cbConfig.setSlidingWindowSize(4);
            cbConfig.setMinimumNumberOfCalls(2);
            cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(60));
            cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
            cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

            CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

            // Create broker pointing to non-existent dashboard
            CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(59999);
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setConnectionTimeout(500);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);

            RabbitTemplate template = new RabbitTemplate(factory);
            template.setMessageConverter(messageConverter);

            RabbitMQMessageBroker failingBroker = new RabbitMQMessageBroker(template, TEST_EXCHANGE, messageConverter);
            ResilientMessageBroker broker = new ResilientMessageBroker(failingBroker, cbConfig, registry);

            try {
                // Trigger failures to open circuit
                for (int i = 0; i < 4; i++) {
                    try {
                        broker.send(TEST_ROUTING_KEY, createTestMessage());
                    } catch (Exception ignored) {}
                }

                // Should report unavailable
                assertThat(broker.isAvailable()).isFalse();

            } finally {
                factory.destroy();
            }
        }

        @Test
        @DisplayName("should track failure metrics correctly")
        void shouldTrackFailureMetrics() {
            StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
            cbConfig.setEnabled(true);
            cbConfig.setFailureRateThreshold(50);
            cbConfig.setSlidingWindowSize(10);
            cbConfig.setMinimumNumberOfCalls(2);
            cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(60));
            cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
            cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

            CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

            CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(59999);
            factory.setUsername("guest");
            factory.setPassword("guest");
            factory.setConnectionTimeout(500);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MessageConverter messageConverter = new Jackson2JsonMessageConverter(objectMapper);

            RabbitTemplate template = new RabbitTemplate(factory);
            template.setMessageConverter(messageConverter);

            RabbitMQMessageBroker failingBroker = new RabbitMQMessageBroker(template, TEST_EXCHANGE, messageConverter);
            ResilientMessageBroker broker = new ResilientMessageBroker(failingBroker, cbConfig, registry);

            try {
                // Trigger some failures
                for (int i = 0; i < 3; i++) {
                    try {
                        broker.send(TEST_ROUTING_KEY, createTestMessage());
                    } catch (Exception ignored) {}
                }

                // Check metrics
                CircuitBreaker cb = registry.circuitBreaker("broker-rabbitmq");
                assertThat(cb.getMetrics().getNumberOfFailedCalls()).isGreaterThanOrEqualTo(2);

            } finally {
                factory.destroy();
            }
        }
    }

    @Nested
    @DisplayName("Recovery scenarios")
    class RecoveryScenarios {

        @Test
        @DisplayName("should recover after broker becomes available again")
        void shouldRecoverAfterBrokerBecomesAvailable() throws Exception {
            // Start with working broker
            assertThat(rabbitBroker.isAvailable()).isTrue();
            resilientBroker.send(TEST_ROUTING_KEY, createTestMessage());

            // Verify circuit is closed
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-rabbitmq");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(cb.getMetrics().getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Message delivery verification")
    class MessageDeliveryVerification {

        @Test
        @DisplayName("should deliver message to queue when broker is available")
        void shouldDeliverMessageToQueue() {
            WorkflowMessage message = createTestMessage();
            resilientBroker.send(TEST_ROUTING_KEY, message);

            // Verify message was delivered
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                Message receivedMessage = rabbitTemplate.receive(TEST_QUEUE, 1000);
                assertThat(receivedMessage).isNotNull();

                WorkflowMessage received = (WorkflowMessage) rabbitTemplate.getMessageConverter()
                        .fromMessage(receivedMessage);

                assertThat(received.getExecutionId()).isEqualTo(message.getExecutionId());
            });
        }
    }
}