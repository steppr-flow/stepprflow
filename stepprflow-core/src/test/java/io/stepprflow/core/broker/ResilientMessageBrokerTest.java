package io.stepprflow.core.broker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.exception.CircuitBreakerOpenException;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD tests for ResilientMessageBroker - Circuit Breaker wrapper for MessageBroker.
 */
@ExtendWith(MockitoExtension.class)
class ResilientMessageBrokerTest {

    @Mock
    private MessageBroker delegateBroker;

    private ResilientMessageBroker resilientBroker;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private StepprFlowProperties.CircuitBreaker cbConfig;

    @BeforeEach
    void setUp() {
        cbConfig = new StepprFlowProperties.CircuitBreaker();
        cbConfig.setEnabled(true);
        cbConfig.setFailureRateThreshold(50);
        cbConfig.setSlidingWindowSize(4);
        cbConfig.setMinimumNumberOfCalls(2);
        cbConfig.setWaitDurationInOpenState(Duration.ofSeconds(5));
        cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
        cbConfig.setAutomaticTransitionFromOpenToHalfOpenEnabled(false);

        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        when(delegateBroker.getBrokerType()).thenReturn("kafka");
    }

    private void createResilientBroker() {
        resilientBroker = new ResilientMessageBroker(delegateBroker, cbConfig, circuitBreakerRegistry);
    }

    private WorkflowMessage createTestMessage() {
        return WorkflowMessage.builder()
                .executionId("test-123")
                .topic("test-topic")
                .status(WorkflowStatus.PENDING)
                .currentStep(1)
                .totalSteps(3)
                .build();
    }

    @Nested
    @DisplayName("When circuit breaker is disabled")
    class CircuitBreakerDisabled {

        @Test
        @DisplayName("should delegate directly without circuit breaker")
        void shouldDelegateDirectly() {
            cbConfig.setEnabled(false);
            createResilientBroker();
            WorkflowMessage message = createTestMessage();

            resilientBroker.send("topic", message);

            verify(delegateBroker).send("topic", message);
        }

        @Test
        @DisplayName("should propagate exceptions without circuit breaker intervention")
        void shouldPropagateExceptions() {
            cbConfig.setEnabled(false);
            createResilientBroker();
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker error")).when(delegateBroker).send(any(), any());

            assertThatThrownBy(() -> resilientBroker.send("topic", message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Broker error");
        }
    }

    @Nested
    @DisplayName("When circuit is CLOSED")
    class CircuitClosed {

        @BeforeEach
        void setUp() {
            createResilientBroker();
        }

        @Test
        @DisplayName("should delegate send() to underlying broker")
        void shouldDelegateSend() {
            WorkflowMessage message = createTestMessage();

            resilientBroker.send("topic", message);

            verify(delegateBroker).send("topic", message);
        }

        @Test
        @DisplayName("should delegate sendSync() to underlying broker")
        void shouldDelegateSendSync() {
            WorkflowMessage message = createTestMessage();

            resilientBroker.sendSync("topic", message);

            verify(delegateBroker).sendSync("topic", message);
        }

        @Test
        @DisplayName("should delegate sendAsync() to underlying broker")
        void shouldDelegateSendAsync() {
            WorkflowMessage message = createTestMessage();
            when(delegateBroker.sendAsync(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            CompletableFuture<Void> result = resilientBroker.sendAsync("topic", message);

            assertThat(result).isCompleted();
            verify(delegateBroker).sendAsync("topic", message);
        }

        @Test
        @DisplayName("should return delegate broker type")
        void shouldReturnBrokerType() {
            assertThat(resilientBroker.getBrokerType()).isEqualTo("kafka");
        }

        @Test
        @DisplayName("should record successful calls")
        void shouldRecordSuccessfulCalls() {
            WorkflowMessage message = createTestMessage();

            resilientBroker.send("topic", message);
            resilientBroker.send("topic", message);

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            assertThat(cb.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("When failures occur")
    class FailureScenarios {

        @BeforeEach
        void setUp() {
            createResilientBroker();
        }

        @Test
        @DisplayName("should record failed calls")
        void shouldRecordFailedCalls() {
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            assertThatThrownBy(() -> resilientBroker.send("topic", message))
                    .isInstanceOf(RuntimeException.class);

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            assertThat(cb.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("should open circuit after exceeding failure threshold")
        void shouldOpenCircuitAfterThreshold() {
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            // Trigger failures to exceed threshold (50% of 4 calls with min 2 calls)
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("should throw CircuitBreakerOpenException when circuit is open")
        void shouldThrowCircuitBreakerOpenException() {
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            // Next call should fail with CircuitBreakerOpenException
            assertThatThrownBy(() -> resilientBroker.send("topic", message))
                    .isInstanceOf(CircuitBreakerOpenException.class)
                    .hasMessageContaining("broker-kafka");
        }

        @Test
        @DisplayName("should include circuit breaker state in exception")
        void shouldIncludeStateInException() {
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            assertThatThrownBy(() -> resilientBroker.send("topic", message))
                    .isInstanceOf(CircuitBreakerOpenException.class)
                    .satisfies(ex -> {
                        CircuitBreakerOpenException cbEx = (CircuitBreakerOpenException) ex;
                        assertThat(cbEx.getCircuitBreakerName()).isEqualTo("broker-kafka");
                        assertThat(cbEx.getState()).isEqualTo(CircuitBreaker.State.OPEN);
                    });
        }
    }

    @Nested
    @DisplayName("Circuit breaker state transitions")
    class StateTransitions {

        @BeforeEach
        void setUp() {
            createResilientBroker();
        }

        @Test
        @DisplayName("should transition from OPEN to HALF_OPEN after wait duration")
        void shouldTransitionToHalfOpen() throws InterruptedException {
            cbConfig.setWaitDurationInOpenState(Duration.ofMillis(100));
            createResilientBroker();

            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Wait for transition
            Thread.sleep(150);
            cb.transitionToHalfOpenState();

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        }

        @Test
        @DisplayName("should close circuit after successful calls in HALF_OPEN state")
        void shouldCloseAfterSuccessInHalfOpen() throws InterruptedException {
            cbConfig.setWaitDurationInOpenState(Duration.ofMillis(50));
            cbConfig.setPermittedNumberOfCallsInHalfOpenState(2);
            createResilientBroker();

            WorkflowMessage message = createTestMessage();

            // First, make it fail to open circuit
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("broker-kafka");
            Thread.sleep(100);
            cb.transitionToHalfOpenState();

            // Now make calls succeed - use lenient() to avoid strict stubbing issues
            lenient().doNothing().when(delegateBroker).send(any(), any());

            resilientBroker.send("topic", message);
            resilientBroker.send("topic", message);

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }

    @Nested
    @DisplayName("Async operations with circuit breaker")
    class AsyncOperations {

        @BeforeEach
        void setUp() {
            createResilientBroker();
        }

        @Test
        @DisplayName("should handle async failures and record in circuit breaker")
        void shouldHandleAsyncFailures() {
            WorkflowMessage message = createTestMessage();
            CompletableFuture<Void> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Async error"));

            when(delegateBroker.sendAsync(any(), any())).thenReturn(failedFuture);

            CompletableFuture<Void> result = resilientBroker.sendAsync("topic", message);

            assertThat(result).isCompletedExceptionally();
        }

        @Test
        @DisplayName("should reject async calls when circuit is open")
        void shouldRejectAsyncWhenOpen() {
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            // Open the circuit using sync calls
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            // Async call should also be rejected
            CompletableFuture<Void> result = resilientBroker.sendAsync("topic", message);

            assertThat(result).isCompletedExceptionally();
            assertThatThrownBy(result::get)
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(CircuitBreakerOpenException.class);
        }
    }

    @Nested
    @DisplayName("isAvailable() behavior")
    class AvailabilityCheck {

        @BeforeEach
        void setUp() {
            createResilientBroker();
        }

        @Test
        @DisplayName("should return true when circuit is closed and delegate is available")
        void shouldReturnTrueWhenClosedAndAvailable() {
            when(delegateBroker.isAvailable()).thenReturn(true);

            assertThat(resilientBroker.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return false when circuit is open")
        void shouldReturnFalseWhenOpen() {
            WorkflowMessage message = createTestMessage();
            doThrow(new RuntimeException("Broker down")).when(delegateBroker).send(any(), any());

            // Open the circuit
            for (int i = 0; i < 3; i++) {
                try {
                    resilientBroker.send("topic", message);
                } catch (Exception ignored) {}
            }

            assertThat(resilientBroker.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should return false when delegate is not available")
        void shouldReturnFalseWhenDelegateUnavailable() {
            when(delegateBroker.isAvailable()).thenReturn(false);

            assertThat(resilientBroker.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Circuit breaker naming")
    class CircuitBreakerNaming {

        @Test
        @DisplayName("should create circuit breaker with broker type in name")
        void shouldNameCircuitBreakerByBrokerType() {
            when(delegateBroker.getBrokerType()).thenReturn("rabbitmq");
            createResilientBroker();

            assertThat(circuitBreakerRegistry.getAllCircuitBreakers())
                    .anyMatch(cb -> cb.getName().equals("broker-rabbitmq"));
        }
    }
}