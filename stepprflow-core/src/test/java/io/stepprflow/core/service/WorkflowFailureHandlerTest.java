package io.stepprflow.core.service;

import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.*;
import io.stepprflow.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowFailureHandler Tests")
class WorkflowFailureHandlerTest {

    @Mock
    private MessageBroker messageBroker;

    @Mock
    private StepprFlowProperties properties;

    @Mock
    private CallbackMethodInvoker callbackInvoker;

    @Mock
    private BackoffCalculator backoffCalculator;

    @Mock
    private CoreMessageFactory messageFactory;

    private WorkflowFailureHandler failureHandler;

    @Captor
    private ArgumentCaptor<WorkflowMessage> messageCaptor;

    private WorkflowMessage testMessage;
    private StepDefinition testStep;
    private WorkflowDefinition testDefinition;
    private StepprFlowProperties.Retry retryConfig;
    private StepprFlowProperties.Dlq dlqConfig;

    @BeforeEach
    void setUp() throws Exception {
        failureHandler = new WorkflowFailureHandler(messageBroker, properties, callbackInvoker, backoffCalculator, messageFactory);

        testMessage = WorkflowMessage.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .currentStep(1)
                .totalSteps(3)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("key", "value"))
                .createdAt(Instant.now())
                .build();

        testStep = StepDefinition.builder()
                .id(1)
                .label("step1")
                .method(TestHandler.class.getDeclaredMethod("step1", Object.class))
                .build();

        testDefinition = WorkflowDefinition.builder()
                .topic("test-topic")
                .handler(new TestHandler())
                .steps(List.of(testStep))
                .build();

        // Setup retry configuration
        retryConfig = new StepprFlowProperties.Retry();
        retryConfig.setMaxAttempts(3);
        retryConfig.setInitialDelay(Duration.ofSeconds(1));
        retryConfig.setMultiplier(2.0);
        retryConfig.setMaxDelay(Duration.ofMinutes(5));
        retryConfig.setNonRetryableExceptions(List.of("java.lang.IllegalArgumentException"));
        lenient().when(properties.getRetry()).thenReturn(retryConfig);

        // Setup DLQ configuration
        dlqConfig = new StepprFlowProperties.Dlq();
        dlqConfig.setEnabled(true);
        dlqConfig.setSuffix(".dlq");
        lenient().when(properties.getDlq()).thenReturn(dlqConfig);

        // Setup default backoff calculator response
        lenient().when(backoffCalculator.calculate(anyInt())).thenReturn(Duration.ofSeconds(1));

        // Setup default message factory responses
        lenient().when(messageFactory.createRetryMessage(any(WorkflowMessage.class), any(RetryInfo.class)))
                .thenAnswer(invocation -> {
                    WorkflowMessage msg = invocation.getArgument(0);
                    RetryInfo retry = invocation.getArgument(1);
                    return msg.toBuilder()
                            .status(WorkflowStatus.RETRY_PENDING)
                            .retryInfo(retry)
                            .build();
                });

        lenient().when(messageFactory.createErrorInfo(any(Throwable.class), any(StepDefinition.class)))
                .thenAnswer(invocation -> {
                    Throwable error = invocation.getArgument(0);
                    StepDefinition step = invocation.getArgument(1);
                    return ErrorInfo.builder()
                            .code("STEP_EXECUTION_FAILED")
                            .message(error.getMessage())
                            .exceptionType(error.getClass().getName())
                            .stepId(step.getId())
                            .stepLabel(step.getLabel())
                            .build();
                });

        lenient().when(messageFactory.createDlqMessage(any(WorkflowMessage.class), any(ErrorInfo.class)))
                .thenAnswer(invocation -> {
                    WorkflowMessage msg = invocation.getArgument(0);
                    ErrorInfo errorInfo = invocation.getArgument(1);
                    return msg.toBuilder()
                            .status(WorkflowStatus.FAILED)
                            .errorInfo(errorInfo)
                            .build();
                });
    }

    @Nested
    @DisplayName("handleFailure() method")
    class HandleFailureTests {

        @Test
        @DisplayName("Should schedule retry when not exhausted and error is retryable")
        void shouldScheduleRetryWhenNotExhausted() {
            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker).send(eq("test-topic.retry"), messageCaptor.capture());
            WorkflowMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getStatus()).isEqualTo(WorkflowStatus.RETRY_PENDING);
            assertThat(sentMessage.getRetryInfo()).isNotNull();
            // Le code crée RetryInfo avec attempt=1 puis appelle nextAttempt() qui incrémente à 2
            assertThat(sentMessage.getRetryInfo().getAttempt()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should send to DLQ when retries are exhausted")
        void shouldSendToDlqWhenRetriesExhausted() {
            RetryInfo exhaustedRetry = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .build();
            testMessage = testMessage.toBuilder()
                    .retryInfo(exhaustedRetry)
                    .build();

            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker).send(eq("test-topic.dlq"), any(WorkflowMessage.class));
        }

        @Test
        @DisplayName("Should send to DLQ for non-retryable exceptions")
        void shouldSendToDlqForNonRetryableExceptions() {
            Exception error = new IllegalArgumentException("Invalid argument");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker).send(eq("test-topic.dlq"), any(WorkflowMessage.class));
            verify(messageBroker, never()).send(eq("test-topic.retry"), any());
        }

        @Test
        @DisplayName("Should continue to next step when continueOnFailure is true")
        void shouldContinueToNextStepWhenContinueOnFailure() {
            testStep = StepDefinition.builder()
                    .id(1)
                    .label("step1")
                    .continueOnFailure(true)
                    .build();
            testDefinition = WorkflowDefinition.builder()
                    .topic("test-topic")
                    .handler(new TestHandler())
                    .steps(List.of(testStep,
                            StepDefinition.builder().id(2).label("step2").build()))
                    .build();

            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getCurrentStep()).isEqualTo(2);
            assertThat(sentMessage.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should not continue on failure for last step")
        void shouldNotContinueOnFailureForLastStep() {
            testStep = StepDefinition.builder()
                    .id(3)
                    .label("step3")
                    .continueOnFailure(true)
                    .build();
            testMessage = testMessage.toBuilder().currentStep(3).build();
            testDefinition = WorkflowDefinition.builder()
                    .topic("test-topic")
                    .handler(new TestHandler())
                    .steps(List.of(
                            StepDefinition.builder().id(1).label("step1").build(),
                            StepDefinition.builder().id(2).label("step2").build(),
                            testStep))
                    .build();

            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            // Should go to retry, not continue
            verify(messageBroker).send(eq("test-topic.retry"), any());
        }

        @Test
        @DisplayName("Should call failure callback when defined")
        void shouldCallFailureCallbackWhenDefined() throws Exception {
            TestHandler handler = new TestHandler();
            Method onFailure = TestHandler.class.getDeclaredMethod("onFailure", Throwable.class);
            testDefinition = WorkflowDefinition.builder()
                    .topic("test-topic")
                    .handler(handler)
                    .steps(List.of(testStep))
                    .onFailureMethod(onFailure)
                    .build();

            // Exhaust retries to trigger DLQ and callback
            RetryInfo exhaustedRetry = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .build();
            testMessage = testMessage.toBuilder()
                    .retryInfo(exhaustedRetry)
                    .build();

            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            // Verify callback invoker was called with failure callback
            verify(callbackInvoker).invokeRaw(
                    eq(onFailure),
                    eq(handler),
                    eq(testMessage),
                    eq(error)
            );
        }
    }

    @Nested
    @DisplayName("isRetryable() method")
    class IsRetryableTests {

        @Test
        @DisplayName("Should return true for retryable exceptions")
        void shouldReturnTrueForRetryableExceptions() {
            Exception error = new RuntimeException("Test error");

            boolean result = failureHandler.isRetryable(error);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-retryable exceptions")
        void shouldReturnFalseForNonRetryableExceptions() {
            Exception error = new IllegalArgumentException("Invalid argument");

            boolean result = failureHandler.isRetryable(error);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateBackoff() method")
    class CalculateBackoffTests {

        @Test
        @DisplayName("Should delegate to BackoffCalculator")
        void shouldDelegateToBackoffCalculator() {
            when(backoffCalculator.calculate(1)).thenReturn(Duration.ofSeconds(1));

            Duration backoff = failureHandler.calculateBackoff(1);

            assertThat(backoff).isEqualTo(Duration.ofSeconds(1));
            verify(backoffCalculator).calculate(1);
        }

        @Test
        @DisplayName("Should pass attempt number to calculator")
        void shouldPassAttemptNumberToCalculator() {
            when(backoffCalculator.calculate(5)).thenReturn(Duration.ofSeconds(16));

            Duration backoff = failureHandler.calculateBackoff(5);

            assertThat(backoff).isEqualTo(Duration.ofSeconds(16));
            verify(backoffCalculator).calculate(5);
        }

        @Test
        @DisplayName("Should return calculator result unchanged")
        void shouldReturnCalculatorResultUnchanged() {
            Duration expectedDelay = Duration.ofMinutes(3);
            when(backoffCalculator.calculate(10)).thenReturn(expectedDelay);

            Duration backoff = failureHandler.calculateBackoff(10);

            assertThat(backoff).isEqualTo(expectedDelay);
        }
    }

    @Nested
    @DisplayName("DLQ message construction")
    class DlqMessageTests {

        @BeforeEach
        void setup() {
            // Exhaust retries so we go to DLQ
            RetryInfo exhaustedRetry = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .build();
            testMessage = testMessage.toBuilder()
                    .retryInfo(exhaustedRetry)
                    .build();
        }

        @Test
        @DisplayName("Should include error info in DLQ message")
        void shouldIncludeErrorInfoInDlqMessage() {
            Exception error = new RuntimeException("Test error message");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker).send(eq("test-topic.dlq"), messageCaptor.capture());
            WorkflowMessage dlqMessage = messageCaptor.getValue();

            assertThat(dlqMessage.getErrorInfo()).isNotNull();
            assertThat(dlqMessage.getErrorInfo().getMessage()).isEqualTo("Test error message");
            assertThat(dlqMessage.getErrorInfo().getCode()).isEqualTo("STEP_EXECUTION_FAILED");
            assertThat(dlqMessage.getErrorInfo().getExceptionType()).isEqualTo("java.lang.RuntimeException");
            assertThat(dlqMessage.getErrorInfo().getStepId()).isEqualTo(1);
            assertThat(dlqMessage.getErrorInfo().getStepLabel()).isEqualTo("step1");
        }

        @Test
        @DisplayName("Should set status to FAILED in DLQ message")
        void shouldSetStatusToFailedInDlqMessage() {
            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker).send(eq("test-topic.dlq"), messageCaptor.capture());
            WorkflowMessage dlqMessage = messageCaptor.getValue();

            assertThat(dlqMessage.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        }

        @Test
        @DisplayName("Should not send to DLQ when disabled")
        void shouldNotSendToDlqWhenDisabled() {
            dlqConfig.setEnabled(false);

            Exception error = new RuntimeException("Test error");

            failureHandler.handleFailure(testMessage, testStep, testDefinition, error);

            verify(messageBroker, never()).send(contains(".dlq"), any());
        }
    }

    // Test handler class
    public static class TestHandler implements StepprFlow {
        public boolean failureCalled = false;

        public void step1(Object payload) {}

        public void onFailure(Throwable error) {
            failureCalled = true;
        }
    }
}
