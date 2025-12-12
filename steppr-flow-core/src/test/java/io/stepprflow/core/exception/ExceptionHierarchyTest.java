package io.stepprflow.core.exception;

import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the exception hierarchy.
 */
@DisplayName("Exception Hierarchy Tests")
class ExceptionHierarchyTest {

    @Nested
    @DisplayName("WorkflowException (base)")
    class WorkflowExceptionTests {

        @Test
        @DisplayName("should be a RuntimeException")
        void shouldBeRuntimeException() {
            WorkflowException exception = new WorkflowException("test");
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should preserve message")
        void shouldPreserveMessage() {
            WorkflowException exception = new WorkflowException("test message");
            assertThat(exception.getMessage()).isEqualTo("test message");
        }

        @Test
        @DisplayName("should preserve cause")
        void shouldPreserveCause() {
            Exception cause = new IllegalStateException("original");
            WorkflowException exception = new WorkflowException("wrapper", cause);
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("WorkflowNotFoundException")
    class WorkflowNotFoundExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            WorkflowNotFoundException exception = new WorkflowNotFoundException("exec-123");
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should contain execution ID in message")
        void shouldContainExecutionIdInMessage() {
            WorkflowNotFoundException exception = new WorkflowNotFoundException("exec-123");
            assertThat(exception)
                    .extracting(
                            WorkflowNotFoundException::getExecutionId,
                            e -> e.getMessage().contains("exec-123"))
                    .containsExactly("exec-123", true);
        }
    }

    @Nested
    @DisplayName("WorkflowStateException")
    class WorkflowStateExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            WorkflowStateException exception = new WorkflowStateException(
                    "exec-123", WorkflowStatus.COMPLETED, "resumed");
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should contain all context information")
        void shouldContainAllContextInformation() {
            WorkflowStateException exception = new WorkflowStateException(
                    "exec-123", WorkflowStatus.COMPLETED, "resumed");

            assertThat(exception)
                    .extracting(
                            WorkflowStateException::getExecutionId,
                            WorkflowStateException::getCurrentStatus,
                            WorkflowStateException::getOperation)
                    .containsExactly("exec-123", WorkflowStatus.COMPLETED, "resumed");
            assertThat(exception.getMessage()).contains("COMPLETED", "resumed");
        }
    }

    @Nested
    @DisplayName("StepTimeoutException")
    class StepTimeoutExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            StepTimeoutException exception = new StepTimeoutException(
                    "stepLabel", 1, Duration.ofSeconds(30));
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should contain timeout information")
        void shouldContainTimeoutInformation() {
            StepTimeoutException exception = new StepTimeoutException(
                    "processPayment", 2, Duration.ofSeconds(30), Duration.ofSeconds(45));

            assertThat(exception)
                    .extracting(
                            StepTimeoutException::getStepLabel,
                            StepTimeoutException::getStepId,
                            StepTimeoutException::getTimeout,
                            StepTimeoutException::getElapsed)
                    .containsExactly("processPayment", 2, Duration.ofSeconds(30), Duration.ofSeconds(45));
            assertThat(exception.getMessage()).contains("processPayment", "timed out");
        }
    }

    @Nested
    @DisplayName("StepExecutionException")
    class StepExecutionExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            StepExecutionException exception = new StepExecutionException(
                    "stepLabel", 1, "Error message");
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should contain step information")
        void shouldContainStepInformation() {
            StepExecutionException exception = new StepExecutionException(
                    "validateOrder", 3, "Validation failed", new IllegalArgumentException("bad input"));

            assertThat(exception)
                    .extracting(
                            StepExecutionException::getStepLabel,
                            StepExecutionException::getStepId)
                    .containsExactly("validateOrder", 3);
            assertThat(exception.getMessage()).contains("validateOrder");
            assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("MessageBrokerException")
    class MessageBrokerExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            MessageBrokerException exception = new MessageBrokerException("Failed to send");
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should include broker type when provided")
        void shouldIncludeBrokerType() {
            MessageBrokerException exception = new MessageBrokerException(
                    "kafka", "Failed to connect");
            assertThat(exception)
                    .extracting(
                            MessageBrokerException::getBrokerType,
                            e -> e.getMessage().contains("kafka"))
                    .containsExactly("kafka", true);
        }
    }

    @Nested
    @DisplayName("BrokerConnectionException")
    class BrokerConnectionExceptionTests {

        @Test
        @DisplayName("should extend MessageBrokerException")
        void shouldExtendMessageBrokerException() {
            BrokerConnectionException exception = new BrokerConnectionException(
                    "kafka", "localhost:9092", "Connection refused");
            assertThat(exception).isInstanceOf(MessageBrokerException.class);
        }

        @Test
        @DisplayName("should contain connection details")
        void shouldContainConnectionDetails() {
            BrokerConnectionException exception = new BrokerConnectionException(
                    "kafka", "localhost:9092", "Connection refused");

            assertThat(exception)
                    .extracting(
                            BrokerConnectionException::getBrokerType,
                            BrokerConnectionException::getBootstrapServers,
                            e -> e.getMessage().contains("localhost:9092"))
                    .containsExactly("kafka", "localhost:9092", true);
        }
    }

    @Nested
    @DisplayName("MessageSendException")
    class MessageSendExceptionTests {

        @Test
        @DisplayName("should extend MessageBrokerException")
        void shouldExtendMessageBrokerException() {
            MessageSendException exception = new MessageSendException(
                    "kafka", "order-topic", "Failed to send");
            assertThat(exception).isInstanceOf(MessageBrokerException.class);
        }

        @Test
        @DisplayName("should contain topic information")
        void shouldContainTopicInformation() {
            MessageSendException exception = new MessageSendException(
                    "rabbitmq", "payment-queue", "Queue full");

            assertThat(exception)
                    .extracting(
                            MessageSendException::getBrokerType,
                            MessageSendException::getTopic,
                            e -> e.getMessage().contains("payment-queue"))
                    .containsExactly("rabbitmq", "payment-queue", true);
        }
    }

    @Nested
    @DisplayName("WorkflowDefinitionException")
    class WorkflowDefinitionExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            WorkflowDefinitionException exception = new WorkflowDefinitionException(
                    "Invalid workflow definition");
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should include topic when provided")
        void shouldIncludeTopic() {
            WorkflowDefinitionException exception = new WorkflowDefinitionException(
                    "order-workflow", "No steps defined");

            assertThat(exception)
                    .extracting(
                            WorkflowDefinitionException::getTopic,
                            e -> e.getMessage().contains("order-workflow"))
                    .containsExactly("order-workflow", true);
        }
    }

    @Nested
    @DisplayName("RetryExhaustedException")
    class RetryExhaustedExceptionTests {

        @Test
        @DisplayName("should extend WorkflowException")
        void shouldExtendWorkflowException() {
            RetryExhaustedException exception = new RetryExhaustedException(
                    "exec-123", 3, "All retries failed");
            assertThat(exception).isInstanceOf(WorkflowException.class);
        }

        @Test
        @DisplayName("should contain retry information")
        void shouldContainRetryInformation() {
            RetryExhaustedException exception = new RetryExhaustedException(
                    "exec-456", 5, "Database unavailable");

            assertThat(exception)
                    .extracting(
                            RetryExhaustedException::getExecutionId,
                            RetryExhaustedException::getAttempts)
                    .containsExactly("exec-456", 5);
            assertThat(exception.getMessage()).contains("exec-456", "5");
        }
    }

    @Nested
    @DisplayName("MessageAcknowledgeException")
    class MessageAcknowledgeExceptionTests {

        @Test
        @DisplayName("should extend MessageBrokerException")
        void shouldExtendMessageBrokerException() {
            MessageAcknowledgeException exception = new MessageAcknowledgeException(
                    "rabbitmq", "12345", "IO error");
            assertThat(exception).isInstanceOf(MessageBrokerException.class);
        }

        @Test
        @DisplayName("should contain delivery tag information")
        void shouldContainDeliveryTagInformation() {
            MessageAcknowledgeException exception = new MessageAcknowledgeException(
                    "rabbitmq", "12345", "Channel closed");

            assertThat(exception)
                    .extracting(
                            MessageAcknowledgeException::getBrokerType,
                            MessageAcknowledgeException::getDeliveryTag)
                    .containsExactly("rabbitmq", "12345");
            assertThat(exception.getMessage()).contains("12345", "acknowledge");
        }

        @Test
        @DisplayName("should preserve cause when provided")
        void shouldPreserveCause() {
            Exception cause = new java.io.IOException("Connection lost");
            MessageAcknowledgeException exception = new MessageAcknowledgeException(
                    "rabbitmq", "12345", "IO error", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("MessageRejectException")
    class MessageRejectExceptionTests {

        @Test
        @DisplayName("should extend MessageBrokerException")
        void shouldExtendMessageBrokerException() {
            MessageRejectException exception = new MessageRejectException(
                    "rabbitmq", "12345", true, "IO error");
            assertThat(exception).isInstanceOf(MessageBrokerException.class);
        }

        @Test
        @DisplayName("should contain delivery tag and requeue information")
        void shouldContainDeliveryTagAndRequeueInformation() {
            MessageRejectException exception = new MessageRejectException(
                    "rabbitmq", "67890", false, "Channel closed");

            assertThat(exception)
                    .extracting(
                            MessageRejectException::getBrokerType,
                            MessageRejectException::getDeliveryTag,
                            MessageRejectException::isRequeue)
                    .containsExactly("rabbitmq", "67890", false);
            assertThat(exception.getMessage()).contains("67890", "reject");
        }

        @Test
        @DisplayName("should preserve cause when provided")
        void shouldPreserveCause() {
            Exception cause = new java.io.IOException("Connection lost");
            MessageRejectException exception = new MessageRejectException(
                    "rabbitmq", "12345", true, "IO error", cause);

            assertThat(exception)
                    .extracting(
                            MessageRejectException::getCause,
                            MessageRejectException::isRequeue)
                    .containsExactly(cause, true);
        }
    }

    @Nested
    @DisplayName("CircuitBreakerOpenException")
    class CircuitBreakerOpenExceptionTests {

        @Test
        @DisplayName("should extend MessageBrokerException")
        void shouldExtendMessageBrokerException() {
            CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
                    "order-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
            assertThat(exception).isInstanceOf(MessageBrokerException.class);
        }

        @Test
        @DisplayName("should contain circuit breaker name and state")
        void shouldContainCircuitBreakerNameAndState() {
            CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
                    "payment-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);

            assertThat(exception)
                    .extracting(
                            CircuitBreakerOpenException::getCircuitBreakerName,
                            CircuitBreakerOpenException::getState)
                    .containsExactly("payment-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
            assertThat(exception.getMessage()).contains("payment-broker", "OPEN");
        }

        @Test
        @DisplayName("should preserve cause when provided")
        void shouldPreserveCauseWhenProvided() {
            Exception cause = new RuntimeException("Original error");
            CircuitBreakerOpenException exception = new CircuitBreakerOpenException(
                    "inventory-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN, cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getState()).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN);
        }

        @Test
        @DisplayName("should handle different circuit breaker states")
        void shouldHandleDifferentCircuitBreakerStates() {
            CircuitBreakerOpenException openException = new CircuitBreakerOpenException(
                    "test-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
            CircuitBreakerOpenException halfOpenException = new CircuitBreakerOpenException(
                    "test-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN);
            CircuitBreakerOpenException forcedOpenException = new CircuitBreakerOpenException(
                    "test-broker", io.github.resilience4j.circuitbreaker.CircuitBreaker.State.FORCED_OPEN);

            assertThat(openException.getMessage()).contains("OPEN");
            assertThat(halfOpenException.getMessage()).contains("HALF_OPEN");
            assertThat(forcedOpenException.getMessage()).contains("FORCED_OPEN");
        }
    }

    @Nested
    @DisplayName("MessageSendException extended tests")
    class MessageSendExceptionExtendedTests {

        @Test
        @DisplayName("should include execution ID when provided with cause")
        void shouldIncludeExecutionIdWhenProvidedWithCause() {
            Exception cause = new RuntimeException("Send timeout");
            MessageSendException exception = new MessageSendException(
                    "kafka", "order-topic", "exec-123", "Send timeout", cause);

            assertThat(exception)
                    .extracting(
                            MessageSendException::getBrokerType,
                            MessageSendException::getTopic,
                            MessageSendException::getExecutionId)
                    .containsExactly("kafka", "order-topic", "exec-123");
            assertThat(exception.getMessage()).contains("exec-123");
        }

        @Test
        @DisplayName("should preserve cause with execution ID")
        void shouldPreserveCauseWithExecutionId() {
            Exception cause = new java.io.IOException("Network error");
            MessageSendException exception = new MessageSendException(
                    "rabbitmq", "payment-queue", "exec-456", "Failed to send", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getExecutionId()).isEqualTo("exec-456");
        }
    }

    @Nested
    @DisplayName("RetryExhaustedException extended tests")
    class RetryExhaustedExceptionExtendedTests {

        @Test
        @DisplayName("should preserve cause when provided")
        void shouldPreserveCauseWhenProvided() {
            Exception cause = new RuntimeException("Database unavailable");
            RetryExhaustedException exception = new RetryExhaustedException(
                    "exec-789", 5, "All retries failed", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getExecutionId()).isEqualTo("exec-789");
            assertThat(exception.getAttempts()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("BrokerConnectionException extended tests")
    class BrokerConnectionExceptionExtendedTests {

        @Test
        @DisplayName("should preserve cause when provided")
        void shouldPreserveCauseWhenProvided() {
            Exception cause = new java.net.ConnectException("Connection refused");
            BrokerConnectionException exception = new BrokerConnectionException(
                    "kafka", "localhost:9092", "Connection refused", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getBootstrapServers()).isEqualTo("localhost:9092");
        }
    }

    @Nested
    @DisplayName("WorkflowDefinitionException extended tests")
    class WorkflowDefinitionExceptionExtendedTests {

        @Test
        @DisplayName("should preserve cause when provided")
        void shouldPreserveCauseWhenProvided() {
            Exception cause = new IllegalArgumentException("Invalid step configuration");
            WorkflowDefinitionException exception = new WorkflowDefinitionException(
                    "order-workflow", "Invalid step", cause);

            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getTopic()).isEqualTo("order-workflow");
        }
    }
}