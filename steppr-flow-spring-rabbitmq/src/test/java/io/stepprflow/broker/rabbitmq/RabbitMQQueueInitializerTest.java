package io.stepprflow.broker.rabbitmq;

import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.service.WorkflowRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitMQQueueInitializer Tests")
class RabbitMQQueueInitializerTest {

    @Mock
    private WorkflowRegistry workflowRegistry;

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Captor
    private ArgumentCaptor<Exchange> exchangeCaptor;

    @Captor
    private ArgumentCaptor<Queue> queueCaptor;

    @Captor
    private ArgumentCaptor<Binding> bindingCaptor;

    private StepprFlowProperties properties;
    private RabbitMQQueueInitializer initializer;

    @BeforeEach
    void setUp() {
        properties = new StepprFlowProperties();
        properties.getRabbitmq().setExchange("stepprflow-exchange");
        properties.getRabbitmq().setDlqSuffix(".dlq");

        initializer = new RabbitMQQueueInitializer(workflowRegistry, rabbitAdmin, properties);
    }

    @Nested
    @DisplayName("init()")
    class InitializationTests {

        @Test
        @DisplayName("Should create exchanges when workflows exist")
        void shouldCreateExchanges() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("order-workflow"));

            // When
            initializer.init();

            // Then
            verify(rabbitAdmin, times(2)).declareExchange(exchangeCaptor.capture());
            List<Exchange> exchanges = exchangeCaptor.getAllValues();

            assertThat(exchanges).hasSize(2);
            assertThat(exchanges.get(0).getName()).isEqualTo("stepprflow-exchange");
            assertThat(exchanges.get(0)).isInstanceOf(TopicExchange.class);
            assertThat(exchanges.get(1).getName()).isEqualTo("stepprflow-exchange.dlq");
            assertThat(exchanges.get(1)).isInstanceOf(DirectExchange.class);
        }

        @Test
        @DisplayName("Should create queues for each workflow")
        void shouldCreateQueuesForWorkflow() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("order-workflow"));

            // When
            initializer.init();

            // Then
            verify(rabbitAdmin, atLeast(4)).declareQueue(queueCaptor.capture());
            List<Queue> queues = queueCaptor.getAllValues();

            List<String> queueNames = queues.stream().map(Queue::getName).toList();
            assertThat(queueNames).contains(
                    "order-workflow",
                    "order-workflow.dlq",
                    "order-workflow.retry",
                    "order-workflow.completed"
            );
        }

        @Test
        @DisplayName("Should create bindings for queues")
        void shouldCreateBindings() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("order-workflow"));

            // When
            initializer.init();

            // Then
            verify(rabbitAdmin, atLeast(4)).declareBinding(bindingCaptor.capture());
            List<Binding> bindings = bindingCaptor.getAllValues();

            assertThat(bindings).hasSizeGreaterThanOrEqualTo(4);
        }

        @Test
        @DisplayName("Should configure DLQ on main queue")
        void shouldConfigureDlqOnMainQueue() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("payment-workflow"));

            // When
            initializer.init();

            // Then
            verify(rabbitAdmin, atLeast(1)).declareQueue(queueCaptor.capture());

            Queue mainQueue = queueCaptor.getAllValues().stream()
                    .filter(q -> q.getName().equals("payment-workflow"))
                    .findFirst()
                    .orElseThrow();

            assertThat(mainQueue.getArguments())
                    .containsEntry("x-dead-letter-exchange", "stepprflow-exchange.dlq")
                    .containsEntry("x-dead-letter-routing-key", "payment-workflow.dlq");
        }

        @Test
        @DisplayName("Should create fallback queue when no workflows registered")
        void shouldCreateFallbackQueueWhenNoWorkflows() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(Collections.emptyList());

            // When
            initializer.init();

            // Then
            verify(rabbitAdmin).declareQueue(queueCaptor.capture());
            Queue fallbackQueue = queueCaptor.getValue();
            assertThat(fallbackQueue.getName()).isEqualTo("stepprflow-no-workflows");
        }

        @Test
        @DisplayName("Should populate workflowQueueNames list")
        void shouldPopulateQueueNamesList() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("wf1", "wf2"));

            // When
            initializer.init();

            // Then
            assertThat(initializer.getWorkflowQueueNames())
                    .contains("wf1", "wf1.retry", "wf2", "wf2.retry");
        }

        @Test
        @DisplayName("Should handle multiple workflows")
        void shouldHandleMultipleWorkflows() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("wf-a", "wf-b", "wf-c"));

            // When
            initializer.init();

            // Then
            // 2 exchanges + 4 queues per workflow (main, dlq, retry, completed) * 3 workflows = 12 queues
            verify(rabbitAdmin, times(2)).declareExchange(any());
            verify(rabbitAdmin, times(12)).declareQueue(any());
        }
    }

    @Nested
    @DisplayName("getWorkflowQueueNames()")
    class GetQueueNamesTests {

        @Test
        @DisplayName("Should return empty list before initialization")
        void shouldReturnEmptyListBeforeInit() {
            // When & Then
            assertThat(initializer.getWorkflowQueueNames()).isEmpty();
        }

        @Test
        @DisplayName("Should return queue names after initialization")
        void shouldReturnQueueNamesAfterInit() {
            // Given
            when(workflowRegistry.getTopics()).thenReturn(List.of("my-workflow"));

            // When
            initializer.init();

            // Then
            assertThat(initializer.getWorkflowQueueNames())
                    .contains("my-workflow", "my-workflow.retry");
        }
    }
}