package io.stepprflow.core.service;

import io.stepprflow.core.annotation.OnFailure;
import io.stepprflow.core.annotation.OnSuccess;
import io.stepprflow.core.annotation.Step;
import io.stepprflow.core.annotation.Timeout;
import io.stepprflow.core.annotation.Topic;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRegistry Tests")
class WorkflowRegistryTest {

    @Mock
    private ApplicationContext applicationContext;

    private WorkflowRegistry workflowRegistry;

    @BeforeEach
    void setUp() {
        workflowRegistry = new WorkflowRegistry(applicationContext);
    }

    @Nested
    @DisplayName("init() method - Bean scanning")
    class InitTests {

        @Test
        @DisplayName("Should register workflow with @Topic annotation")
        void shouldRegisterWorkflowWithTopicAnnotation() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");
            assertThat(definition).isNotNull();
            assertThat(definition.getTopic()).isEqualTo("test-workflow");
            assertThat(definition.getDescription()).isEqualTo("Test workflow description");
        }

        @Test
        @DisplayName("Should skip beans without StepprFlow interface")
        void shouldSkipBeansWithoutStepprFlowInterface() {
            Object nonWorkflow = new Object();
            Map<String, Object> beans = Map.of("nonWorkflow", nonWorkflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            assertThat(workflowRegistry.getTopics()).isEmpty();
        }

        @Test
        @DisplayName("Should register multiple workflows")
        void shouldRegisterMultipleWorkflows() {
            TestWorkflow workflow1 = new TestWorkflow();
            AnotherWorkflow workflow2 = new AnotherWorkflow();
            Map<String, Object> beans = new HashMap<>();
            beans.put("testWorkflow", workflow1);
            beans.put("anotherWorkflow", workflow2);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            assertThat(workflowRegistry.getTopics()).hasSize(2);
            assertThat(workflowRegistry.getTopics()).containsExactlyInAnyOrder("test-workflow", "another-workflow");
        }
    }

    @Nested
    @DisplayName("Step discovery")
    class StepDiscoveryTests {

        @Test
        @DisplayName("Should discover all @Step annotated methods")
        void shouldDiscoverAllStepMethods() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");
            assertThat(definition.getTotalSteps()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should sort steps by ID")
        void shouldSortStepsById() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");
            List<StepDefinition> steps = definition.getSteps();

            assertThat(steps.get(0).getId()).isEqualTo(1);
            assertThat(steps.get(1).getId()).isEqualTo(2);
            assertThat(steps.get(2).getId()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should extract step labels and descriptions")
        void shouldExtractStepLabelsAndDescriptions() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");
            StepDefinition step1 = definition.getStep(1);

            assertThat(step1.getLabel()).isEqualTo("Validate Input");
            assertThat(step1.getDescription()).isEqualTo("Validates the input payload");
        }

        @Test
        @DisplayName("Should extract step configuration")
        void shouldExtractStepConfiguration() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");
            StepDefinition step2 = definition.getStep(2);

            assertThat(step2.isSkippable()).isTrue();
            assertThat(step2.isContinueOnFailure()).isFalse();
        }
    }

    @Nested
    @DisplayName("Timeout configuration")
    class TimeoutConfigurationTests {

        @Test
        @DisplayName("Should extract step-level timeout")
        void shouldExtractStepLevelTimeout() {
            WorkflowWithTimeout workflow = new WorkflowWithTimeout();
            Map<String, Object> beans = Map.of("workflowWithTimeout", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("timeout-workflow");
            StepDefinition step = definition.getStep(1);

            assertThat(step.getTimeout()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("Should extract workflow-level timeout")
        void shouldExtractWorkflowLevelTimeout() {
            WorkflowWithTimeout workflow = new WorkflowWithTimeout();
            Map<String, Object> beans = Map.of("workflowWithTimeout", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("timeout-workflow");

            assertThat(definition.getTimeout()).isEqualTo(Duration.ofMinutes(5));
        }
    }

    @Nested
    @DisplayName("Callback discovery")
    class CallbackDiscoveryTests {

        @Test
        @DisplayName("Should discover @OnSuccess callback")
        void shouldDiscoverOnSuccessCallback() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");

            assertThat(definition.getOnSuccessMethod()).isNotNull();
            assertThat(definition.getOnSuccessMethod().getName()).isEqualTo("handleSuccess");
        }

        @Test
        @DisplayName("Should discover @OnFailure callback")
        void shouldDiscoverOnFailureCallback() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");

            assertThat(definition.getOnFailureMethod()).isNotNull();
            assertThat(definition.getOnFailureMethod().getName()).isEqualTo("handleFailure");
        }
    }

    @Nested
    @DisplayName("Topic configuration")
    class TopicConfigurationTests {

        @Test
        @DisplayName("Should extract partitions and replication factor")
        void shouldExtractPartitionsAndReplication() {
            TestWorkflow workflow = new TestWorkflow();
            Map<String, Object> beans = Map.of("testWorkflow", workflow);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);

            workflowRegistry.init();

            WorkflowDefinition definition = workflowRegistry.getDefinition("test-workflow");

            assertThat(definition.getPartitions()).isEqualTo(3);
            assertThat(definition.getReplication()).isEqualTo((short) 2);
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethodsTests {

        @BeforeEach
        void setUpWorkflows() {
            TestWorkflow workflow1 = new TestWorkflow();
            AnotherWorkflow workflow2 = new AnotherWorkflow();
            Map<String, Object> beans = new HashMap<>();
            beans.put("testWorkflow", workflow1);
            beans.put("anotherWorkflow", workflow2);
            when(applicationContext.getBeansWithAnnotation(Topic.class)).thenReturn(beans);
            workflowRegistry.init();
        }

        @Test
        @DisplayName("Should return null for unknown topic")
        void shouldReturnNullForUnknownTopic() {
            WorkflowDefinition definition = workflowRegistry.getDefinition("unknown-topic");
            assertThat(definition).isNull();
        }

        @Test
        @DisplayName("Should return all topics")
        void shouldReturnAllTopics() {
            List<String> topics = workflowRegistry.getTopics();

            assertThat(topics).hasSize(2);
            assertThat(topics).containsExactlyInAnyOrder("test-workflow", "another-workflow");
        }

        @Test
        @DisplayName("Should return all definitions")
        void shouldReturnAllDefinitions() {
            List<WorkflowDefinition> definitions = workflowRegistry.getAllDefinitions();

            assertThat(definitions).hasSize(2);
        }
    }

    // Test workflow classes
    @Topic(value = "test-workflow", description = "Test workflow description", partitions = 3, replication = 2)
    static class TestWorkflow implements StepprFlow {

        @Step(id = 1, label = "Validate Input", description = "Validates the input payload")
        public void validateInput(Object payload) {
        }

        @Step(id = 2, label = "Process Data", skippable = true)
        public void processData(Object payload) {
        }

        @Step(id = 3, label = "Send Notification", continueOnFailure = true)
        public void sendNotification(Object payload) {
        }

        @OnSuccess
        public void handleSuccess(Object payload) {
        }

        @OnFailure
        public void handleFailure(Object payload, Throwable error) {
        }
    }

    @Topic(value = "another-workflow", description = "Another test workflow")
    static class AnotherWorkflow implements StepprFlow {

        @Step(id = 1, label = "Single Step")
        public void singleStep(Object payload) {
        }
    }

    @Topic("timeout-workflow")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    static class WorkflowWithTimeout implements StepprFlow {

        @Step(id = 1, label = "Timed Step")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        public void timedStep(Object payload) {
        }
    }
}