package io.stepprflow.broker.kafka;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.broker.ResilientMessageBroker;
import io.stepprflow.core.exception.CircuitBreakerOpenException;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Failure scenario tests for Kafka broker using TestContainers.
 * Tests broker unavailability and circuit breaker behavior.
 */
@Testcontainers
class KafkaBrokerFailureScenarioTest {

    private static final String TEST_TOPIC = "failure-test-topic";

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    private KafkaMessageBroker kafkaBroker;
    private ResilientMessageBroker resilientBroker;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure container is running
        if (!kafka.isRunning()) {
            kafka.start();
        }

        // Create Kafka template
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        producerProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 0);

        ProducerFactory<String, WorkflowMessage> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<String, WorkflowMessage> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        kafkaBroker = new KafkaMessageBroker(kafkaTemplate);

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
        resilientBroker = new ResilientMessageBroker(kafkaBroker, cbConfig, circuitBreakerRegistry);

        // Create test topic
        createTopicIfNotExists();
    }

    private void createTopicIfNotExists() {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()))) {
            Set<String> existingTopics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
            if (!existingTopics.contains(TEST_TOPIC)) {
                NewTopic topic = new NewTopic(TEST_TOPIC, 1, (short) 1);
                adminClient.createTopics(Collections.singletonList(topic)).all().get(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            // Topic may already exist
        }
    }

    private WorkflowMessage createTestMessage() {
        return WorkflowMessage.builder()
                .executionId(UUID.randomUUID().toString())
                .correlationId(UUID.randomUUID().toString())
                .topic(TEST_TOPIC)
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
            assertThat(kafkaBroker.isAvailable()).isTrue();
            assertThat(kafkaBroker.getBrokerType()).isEqualTo("kafka");
        }

        @Test
        @DisplayName("should send message successfully when broker is available")
        void shouldSendMessageSuccessfully() {
            WorkflowMessage message = createTestMessage();

            assertThatCode(() -> kafkaBroker.send(TEST_TOPIC, message))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should send message synchronously when broker is available")
        void shouldSendMessageSyncSuccessfully() {
            WorkflowMessage message = createTestMessage();

            assertThatCode(() -> kafkaBroker.sendSync(TEST_TOPIC, message))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should send message asynchronously when broker is available")
        void shouldSendMessageAsyncSuccessfully() throws Exception {
            WorkflowMessage message = createTestMessage();

            kafkaBroker.sendAsync(TEST_TOPIC, message).get(10, TimeUnit.SECONDS);
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
                resilientBroker.send(TEST_TOPIC, createTestMessage());
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            assertThat(cb.getMetrics().getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(3);
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should keep circuit closed after successful calls")
        void shouldKeepCircuitClosedAfterSuccess() {
            for (int i = 0; i < 5; i++) {
                resilientBroker.send(TEST_TOPIC, createTestMessage());
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should report available when circuit is closed and broker is connected")
        void shouldReportAvailableWhenCircuitClosedAndConnected() {
            // Send a successful message to ensure circuit is closed
            resilientBroker.send(TEST_TOPIC, createTestMessage());

            assertThat(resilientBroker.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return correct broker type through resilient broker")
        void shouldReturnBrokerTypeThroughResilientBroker() {
            assertThat(resilientBroker.getBrokerType()).isEqualTo("kafka");
        }
    }

    @Nested
    @DisplayName("Broker unavailability with pause")
    class BrokerUnavailabilityWithPause {

        @Test
        @DisplayName("should fail send when broker is paused")
        void shouldFailSendWhenBrokerPaused() {
            // Pause the container
            kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();

            try {
                // Use a short timeout template for this test
                Map<String, Object> shortTimeoutProps = new HashMap<>();
                shortTimeoutProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
                shortTimeoutProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                shortTimeoutProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
                shortTimeoutProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
                shortTimeoutProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 3000);
                shortTimeoutProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);
                shortTimeoutProps.put(ProducerConfig.RETRIES_CONFIG, 0);

                ProducerFactory<String, WorkflowMessage> factory = new DefaultKafkaProducerFactory<>(shortTimeoutProps);
                KafkaTemplate<String, WorkflowMessage> template = new KafkaTemplate<>(factory);
                KafkaMessageBroker shortTimeoutBroker = new KafkaMessageBroker(template);

                assertThatThrownBy(() -> shortTimeoutBroker.sendSync(TEST_TOPIC, createTestMessage()))
                        .isInstanceOf(RuntimeException.class);
            } finally {
                kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
            }
        }

        @Test
        @DisplayName("should recover after broker is unpaused")
        void shouldRecoverAfterBrokerUnpaused() throws Exception {
            // Pause briefly
            kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();
            Thread.sleep(1000);

            // Unpause
            kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
            Thread.sleep(2000);

            // Should work again
            await().atMost(30, TimeUnit.SECONDS)
                    .pollInterval(Duration.ofSeconds(1))
                    .untilAsserted(() ->
                        assertThatCode(() -> kafkaBroker.send(TEST_TOPIC, createTestMessage()))
                                .doesNotThrowAnyException()
                    );
        }
    }

    @Nested
    @DisplayName("Circuit breaker with failures")
    class CircuitBreakerWithFailures {

        @Test
        @DisplayName("should open circuit after repeated failures")
        void shouldOpenCircuitAfterRepeatedFailures() {
            // Pause to cause failures
            kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();

            try {
                // Create broker with very short timeouts
                Map<String, Object> shortTimeoutProps = new HashMap<>();
                shortTimeoutProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
                shortTimeoutProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                shortTimeoutProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
                shortTimeoutProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
                shortTimeoutProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
                shortTimeoutProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
                shortTimeoutProps.put(ProducerConfig.RETRIES_CONFIG, 0);

                ProducerFactory<String, WorkflowMessage> factory = new DefaultKafkaProducerFactory<>(shortTimeoutProps);
                KafkaTemplate<String, WorkflowMessage> template = new KafkaTemplate<>(factory);
                KafkaMessageBroker shortTimeoutBroker = new KafkaMessageBroker(template);

                StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
                cbConfig.setEnabled(true);
                cbConfig.setFailureRateThreshold(50);
                cbConfig.setSlidingWindowSize(4);
                cbConfig.setMinimumNumberOfCalls(2);
                cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(60));
                cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
                cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

                CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
                ResilientMessageBroker broker = new ResilientMessageBroker(shortTimeoutBroker, cbConfig, registry);

                // Trigger failures
                for (int i = 0; i < 4; i++) {
                    try {
                        broker.sendSync(TEST_TOPIC, createTestMessage());
                    } catch (Exception ignored) {}
                }

                // Circuit should be open
                CircuitBreaker cb = registry.circuitBreaker("broker-kafka");
                assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

                // Next call should throw CircuitBreakerOpenException
                assertThatThrownBy(() -> broker.send(TEST_TOPIC, createTestMessage()))
                        .isInstanceOf(CircuitBreakerOpenException.class)
                        .satisfies(ex -> {
                            CircuitBreakerOpenException cbEx = (CircuitBreakerOpenException) ex;
                            assertThat(cbEx.getCircuitBreakerName()).isEqualTo("broker-kafka");
                            assertThat(cbEx.getState()).isEqualTo(CircuitBreaker.State.OPEN);
                        });

            } finally {
                kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
            }
        }

        @Test
        @DisplayName("should report unavailable when circuit is open")
        void shouldReportUnavailableWhenCircuitOpen() {
            // Pause to cause failures
            kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();

            try {
                Map<String, Object> shortTimeoutProps = new HashMap<>();
                shortTimeoutProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
                shortTimeoutProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                shortTimeoutProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
                shortTimeoutProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
                shortTimeoutProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 2000);
                shortTimeoutProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
                shortTimeoutProps.put(ProducerConfig.RETRIES_CONFIG, 0);

                ProducerFactory<String, WorkflowMessage> factory = new DefaultKafkaProducerFactory<>(shortTimeoutProps);
                KafkaTemplate<String, WorkflowMessage> template = new KafkaTemplate<>(factory);
                KafkaMessageBroker shortTimeoutBroker = new KafkaMessageBroker(template);

                StepprFlowProperties.CircuitBreaker cbConfig = new StepprFlowProperties.CircuitBreaker();
                cbConfig.setEnabled(true);
                cbConfig.setFailureRateThreshold(50);
                cbConfig.setSlidingWindowSize(4);
                cbConfig.setMinimumNumberOfCalls(2);
                cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(60));
                cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
                cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

                CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
                ResilientMessageBroker broker = new ResilientMessageBroker(shortTimeoutBroker, cbConfig, registry);

                // Trigger failures to open circuit
                for (int i = 0; i < 4; i++) {
                    try {
                        broker.sendSync(TEST_TOPIC, createTestMessage());
                    } catch (Exception ignored) {}
                }

                // Should report unavailable
                assertThat(broker.isAvailable()).isFalse();

            } finally {
                kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
            }
        }
    }
}