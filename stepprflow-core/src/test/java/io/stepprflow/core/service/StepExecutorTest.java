package io.stepprflow.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StepExecutor Tests")
class StepExecutorTest {

    @Mock
    private WorkflowRegistry registry;

    @Mock
    private MessageBroker messageBroker;

    @Mock
    private StepprFlowProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StepExecutor stepExecutor;

    @Captor
    private ArgumentCaptor<WorkflowMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    private WorkflowMessage testMessage;
    private WorkflowDefinition testDefinition;
    private TestWorkflow testWorkflow;

    @BeforeEach
    void setUp() {
        testWorkflow = new TestWorkflow();

        // Ne pas mettre payloadType pour éviter l'appel à objectMapper.convertValue()
        // Le payload brut sera utilisé directement par deserializePayload()
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
    }

    @Nested
    @DisplayName("execute() method")
    class ExecuteTests {

        @Test
        @DisplayName("Should skip execution when workflow definition is not found")
        void shouldSkipExecutionWhenDefinitionNotFound() {
            when(registry.getDefinition("test-topic")).thenReturn(null);

            stepExecutor.execute(testMessage);

            verify(messageBroker, never()).send(any(), any());
        }

        @Test
        @DisplayName("Should skip execution when step is not found")
        void shouldSkipExecutionWhenStepNotFound() throws Exception {
            // Le message a currentStep=1, mais on définit seulement le step 99
            // Donc le step 1 ne sera pas trouvé
            StepDefinition step = createStepDefinition(99, "step1");
            testDefinition = createWorkflowDefinition(List.of(step));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker, never()).send(any(), any());
        }

        @Test
        @DisplayName("Should execute step and advance to next step")
        void shouldExecuteStepAndAdvanceToNextStep() throws Exception {
            StepDefinition step1 = createStepDefinition(1, "step1");
            StepDefinition step2 = createStepDefinition(2, "step2");
            StepDefinition step3 = createStepDefinition(3, "step3");
            testDefinition = createWorkflowDefinition(List.of(step1, step2, step3));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getCurrentStep()).isEqualTo(2);
            assertThat(sentMessage.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
            assertThat(testWorkflow.step1Called).isTrue();
        }

        @Test
        @DisplayName("Should complete workflow on last step")
        void shouldCompleteWorkflowOnLastStep() throws Exception {
            testMessage = testMessage.toBuilder()
                    .currentStep(3)
                    .build();

            StepDefinition step1 = createStepDefinition(1, "step1");
            StepDefinition step2 = createStepDefinition(2, "step2");
            StepDefinition step3 = createStepDefinition(3, "step3");
            testDefinition = createWorkflowDefinition(List.of(step1, step2, step3));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic.completed"), messageCaptor.capture());
            WorkflowMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should call success callback on completion")
        void shouldCallSuccessCallbackOnCompletion() throws Exception {
            testMessage = testMessage.toBuilder()
                    .currentStep(3)
                    .build();

            StepDefinition step1 = createStepDefinition(1, "step1");
            StepDefinition step2 = createStepDefinition(2, "step2");
            StepDefinition step3 = createStepDefinition(3, "step3");
            Method onSuccessMethod = TestWorkflow.class.getDeclaredMethod("onSuccess", Object.class);
            testDefinition = createWorkflowDefinition(List.of(step1, step2, step3), onSuccessMethod, null);

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            assertThat(testWorkflow.successCalled).isTrue();
        }
    }

    @Nested
    @DisplayName("Failure handling")
    class FailureHandlingTests {

        private StepprFlowProperties.Retry retryConfig;
        private StepprFlowProperties.Dlq dlqConfig;

        @BeforeEach
        void setUpRetryProperties() {
            retryConfig = new StepprFlowProperties.Retry();
            retryConfig.setMaxAttempts(3);
            retryConfig.setInitialDelay(Duration.ofSeconds(1));
            retryConfig.setMaxDelay(Duration.ofMinutes(5));
            retryConfig.setMultiplier(2.0);
            retryConfig.setNonRetryableExceptions(List.of("java.lang.IllegalArgumentException"));

            dlqConfig = new StepprFlowProperties.Dlq();
            dlqConfig.setEnabled(true);
            dlqConfig.setSuffix(".dlq");
        }

        @Test
        @DisplayName("Should schedule retry on retryable exception")
        void shouldScheduleRetryOnRetryableException() throws Exception {
            when(properties.getRetry()).thenReturn(retryConfig);

            StepDefinition step = createFailingStepDefinition(1, "failingStep");
            testDefinition = createWorkflowDefinition(List.of(step));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic.retry"), messageCaptor.capture());
            WorkflowMessage retryMessage = messageCaptor.getValue();

            assertThat(retryMessage.getStatus()).isEqualTo(WorkflowStatus.RETRY_PENDING);
            assertThat(retryMessage.getRetryInfo()).isNotNull();
            // Le code crée RetryInfo avec attempt=1 puis appelle nextAttempt() qui incrémente à 2
            assertThat(retryMessage.getRetryInfo().getAttempt()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should send to DLQ when retries exhausted")
        void shouldSendToDlqWhenRetriesExhausted() throws Exception {
            // Seulement getDlq() est utilisé car isExhausted() court-circuite l'appel à getRetry()
            when(properties.getDlq()).thenReturn(dlqConfig);

            RetryInfo exhaustedRetry = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .build();
            testMessage = testMessage.toBuilder()
                    .retryInfo(exhaustedRetry)
                    .build();

            StepDefinition step = createFailingStepDefinition(1, "failingStep");
            testDefinition = createWorkflowDefinition(List.of(step));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic.dlq"), messageCaptor.capture());
            WorkflowMessage dlqMessage = messageCaptor.getValue();

            assertThat(dlqMessage.getStatus()).isEqualTo(WorkflowStatus.FAILED);
            assertThat(dlqMessage.getErrorInfo()).isNotNull();
            assertThat(dlqMessage.getErrorInfo().getCode()).isEqualTo("STEP_EXECUTION_FAILED");
        }

        @Test
        @DisplayName("Should send to DLQ on non-retryable exception")
        void shouldSendToDlqOnNonRetryableException() throws Exception {
            when(properties.getRetry()).thenReturn(retryConfig);
            when(properties.getDlq()).thenReturn(dlqConfig);

            StepDefinition step = createIllegalArgumentStepDefinition(1, "illegalStep");
            testDefinition = createWorkflowDefinition(List.of(step));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic.dlq"), any(WorkflowMessage.class));
        }

        @Test
        @DisplayName("Should continue on failure when configured")
        void shouldContinueOnFailureWhenConfigured() throws Exception {
            // Pas besoin de configurer properties car continueOnFailure court-circuite le retry
            StepDefinition step1 = createFailingStepDefinition(1, "failingStep", true);
            StepDefinition step2 = createStepDefinition(2, "step2");
            testDefinition = createWorkflowDefinition(List.of(step1, step2));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage nextMessage = messageCaptor.getValue();

            assertThat(nextMessage.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should call failure callback on DLQ")
        void shouldCallFailureCallbackOnDlq() throws Exception {
            when(properties.getDlq()).thenReturn(dlqConfig);

            RetryInfo exhaustedRetry = RetryInfo.builder()
                    .attempt(3)
                    .maxAttempts(3)
                    .build();
            testMessage = testMessage.toBuilder()
                    .retryInfo(exhaustedRetry)
                    .build();

            StepDefinition step = createFailingStepDefinition(1, "failingStep");
            Method onFailureMethod = TestWorkflow.class.getDeclaredMethod("onFailure", Object.class, Throwable.class);
            testDefinition = createWorkflowDefinition(List.of(step), null, onFailureMethod);

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            assertThat(testWorkflow.failureCalled).isTrue();
        }
    }

    @Nested
    @DisplayName("Backoff calculation")
    class BackoffCalculationTests {

        @BeforeEach
        void setUpRetryProperties() {
            StepprFlowProperties.Retry retryConfig = new StepprFlowProperties.Retry();
            retryConfig.setMaxAttempts(5);
            retryConfig.setInitialDelay(Duration.ofSeconds(1));
            retryConfig.setMaxDelay(Duration.ofMinutes(5));
            retryConfig.setMultiplier(2.0);
            retryConfig.setNonRetryableExceptions(List.of());

            when(properties.getRetry()).thenReturn(retryConfig);
        }

        @Test
        @DisplayName("Should apply exponential backoff on retries")
        void shouldApplyExponentialBackoff() throws Exception {
            // Test with attempt 1
            testMessage = testMessage.toBuilder()
                    .retryInfo(RetryInfo.builder().attempt(1).maxAttempts(5).build())
                    .build();

            StepDefinition step = createFailingStepDefinition(1, "failingStep");
            testDefinition = createWorkflowDefinition(List.of(step));

            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            stepExecutor.execute(testMessage);

            verify(messageBroker).send(eq("test-topic.retry"), messageCaptor.capture());
            WorkflowMessage retryMessage = messageCaptor.getValue();

            assertThat(retryMessage.getRetryInfo().getNextRetryAt()).isNotNull();
            assertThat(retryMessage.getRetryInfo().getAttempt()).isEqualTo(2);
        }
    }

    // Helper methods
    private StepDefinition createStepDefinition(int id, String methodName) throws Exception {
        Method method = TestWorkflow.class.getDeclaredMethod(methodName, Object.class);
        return StepDefinition.builder()
                .id(id)
                .label(methodName)
                .description("Test step " + id)
                .method(method)
                .skippable(false)
                .continueOnFailure(false)
                .build();
    }

    private StepDefinition createFailingStepDefinition(int id, String methodName) throws Exception {
        return createFailingStepDefinition(id, methodName, false);
    }

    private StepDefinition createFailingStepDefinition(int id, String methodName, boolean continueOnFailure) throws Exception {
        Method method = TestWorkflow.class.getDeclaredMethod(methodName, Object.class);
        return StepDefinition.builder()
                .id(id)
                .label(methodName)
                .description("Failing step " + id)
                .method(method)
                .skippable(false)
                .continueOnFailure(continueOnFailure)
                .build();
    }

    private StepDefinition createIllegalArgumentStepDefinition(int id, String methodName) throws Exception {
        Method method = TestWorkflow.class.getDeclaredMethod(methodName, Object.class);
        return StepDefinition.builder()
                .id(id)
                .label(methodName)
                .description("Illegal argument step " + id)
                .method(method)
                .skippable(false)
                .continueOnFailure(false)
                .build();
    }

    private WorkflowDefinition createWorkflowDefinition(List<StepDefinition> steps) {
        return createWorkflowDefinition(steps, null, null);
    }

    private WorkflowDefinition createWorkflowDefinition(List<StepDefinition> steps,
                                                        Method onSuccessMethod,
                                                        Method onFailureMethod) {
        return WorkflowDefinition.builder()
                .topic("test-topic")
                .description("Test workflow")
                .handler(testWorkflow)
                .handlerClass(TestWorkflow.class)
                .steps(steps)
                .onSuccessMethod(onSuccessMethod)
                .onFailureMethod(onFailureMethod)
                .partitions(1)
                .replication((short) 1)
                .build();
    }

    // Test workflow class
    static class TestWorkflow implements StepprFlow {
        boolean step1Called = false;
        boolean step2Called = false;
        boolean step3Called = false;
        boolean successCalled = false;
        boolean failureCalled = false;

        public void step1(Object payload) {
            step1Called = true;
        }

        public void step2(Object payload) {
            step2Called = true;
        }

        public void step3(Object payload) {
            step3Called = true;
        }

        public void failingStep(Object payload) {
            throw new RuntimeException("Step failed intentionally");
        }

        public void illegalStep(Object payload) {
            throw new IllegalArgumentException("Invalid argument");
        }

        public void onSuccess(Object payload) {
            successCalled = true;
        }

        public void onFailure(Object payload, Throwable error) {
            failureCalled = true;
        }
    }
}