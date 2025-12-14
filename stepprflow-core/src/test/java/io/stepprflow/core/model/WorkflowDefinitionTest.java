package io.stepprflow.core.model;

import io.stepprflow.core.service.StepprFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowDefinition Tests")
class WorkflowDefinitionTest {

    private WorkflowDefinition definition;
    private List<StepDefinition> steps;

    @BeforeEach
    void setUp() {
        steps = List.of(
                StepDefinition.builder().id(1).label("Step 1").build(),
                StepDefinition.builder().id(2).label("Step 2").build(),
                StepDefinition.builder().id(3).label("Step 3").build()
        );

        definition = WorkflowDefinition.builder()
                .topic("test-topic")
                .description("Test workflow")
                .steps(steps)
                .partitions(3)
                .replication((short) 2)
                .timeout(Duration.ofMinutes(30))
                .build();
    }

    @Nested
    @DisplayName("getStep() method")
    class GetStepTests {

        @Test
        @DisplayName("Should return step by ID")
        void shouldReturnStepById() {
            StepDefinition step = definition.getStep(2);

            assertThat(step).isNotNull();
            assertThat(step.getId()).isEqualTo(2);
            assertThat(step.getLabel()).isEqualTo("Step 2");
        }

        @Test
        @DisplayName("Should return null for non-existent step ID")
        void shouldReturnNullForNonExistentStepId() {
            StepDefinition step = definition.getStep(99);

            assertThat(step).isNull();
        }

        @Test
        @DisplayName("Should return first step")
        void shouldReturnFirstStep() {
            StepDefinition step = definition.getStep(1);

            assertThat(step).isNotNull();
            assertThat(step.getLabel()).isEqualTo("Step 1");
        }

        @Test
        @DisplayName("Should return last step")
        void shouldReturnLastStep() {
            StepDefinition step = definition.getStep(3);

            assertThat(step).isNotNull();
            assertThat(step.getLabel()).isEqualTo("Step 3");
        }
    }

    @Nested
    @DisplayName("getTotalSteps() method")
    class GetTotalStepsTests {

        @Test
        @DisplayName("Should return correct count of steps")
        void shouldReturnCorrectCount() {
            assertThat(definition.getTotalSteps()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero for empty steps list")
        void shouldReturnZeroForEmptySteps() {
            definition = definition.toBuilder()
                    .steps(List.of())
                    .build();

            assertThat(definition.getTotalSteps()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return one for single step")
        void shouldReturnOneForSingleStep() {
            definition = definition.toBuilder()
                    .steps(List.of(StepDefinition.builder().id(1).build()))
                    .build();

            assertThat(definition.getTotalSteps()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isLastStep() method")
    class IsLastStepTests {

        @Test
        @DisplayName("Should return true for highest step ID")
        void shouldReturnTrueForHighestStepId() {
            assertThat(definition.isLastStep(3)).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-last step ID")
        void shouldReturnFalseForNonLastStepId() {
            assertThat(definition.isLastStep(1)).isFalse();
            assertThat(definition.isLastStep(2)).isFalse();
        }

        @Test
        @DisplayName("Should return false for non-existent step ID")
        void shouldReturnFalseForNonExistentStepId() {
            assertThat(definition.isLastStep(99)).isFalse();
        }

        @Test
        @DisplayName("Should handle non-sequential step IDs")
        void shouldHandleNonSequentialStepIds() {
            List<StepDefinition> nonSequentialSteps = List.of(
                    StepDefinition.builder().id(10).label("Step A").build(),
                    StepDefinition.builder().id(20).label("Step B").build(),
                    StepDefinition.builder().id(15).label("Step C").build()
            );
            definition = definition.toBuilder()
                    .steps(nonSequentialSteps)
                    .build();

            assertThat(definition.isLastStep(20)).isTrue();
            assertThat(definition.isLastStep(10)).isFalse();
            assertThat(definition.isLastStep(15)).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty steps list")
        void shouldReturnFalseForEmptySteps() {
            definition = definition.toBuilder()
                    .steps(List.of())
                    .build();

            assertThat(definition.isLastStep(1)).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder and accessors")
    class BuilderAndAccessorsTests {

        @Test
        @DisplayName("Should build definition with all fields")
        void shouldBuildDefinitionWithAllFields() throws NoSuchMethodException {
            TestWorkflow handler = new TestWorkflow();

            WorkflowDefinition fullDefinition = WorkflowDefinition.builder()
                    .topic("full-topic")
                    .description("Full description")
                    .handler(handler)
                    .handlerClass(TestWorkflow.class)
                    .steps(steps)
                    .onSuccessMethod(TestWorkflow.class.getMethod("onSuccess"))
                    .onFailureMethod(TestWorkflow.class.getMethod("onFailure"))
                    .timeout(Duration.ofHours(1))
                    .partitions(6)
                    .replication((short) 3)
                    .build();

            assertThat(fullDefinition.getTopic()).isEqualTo("full-topic");
            assertThat(fullDefinition.getDescription()).isEqualTo("Full description");
            assertThat(fullDefinition.getHandler()).isEqualTo(handler);
            assertThat(fullDefinition.getHandlerClass()).isEqualTo(TestWorkflow.class);
            assertThat(fullDefinition.getSteps()).hasSize(3);
            assertThat(fullDefinition.getOnSuccessMethod()).isNotNull();
            assertThat(fullDefinition.getOnFailureMethod()).isNotNull();
            assertThat(fullDefinition.getTimeout()).isEqualTo(Duration.ofHours(1));
            assertThat(fullDefinition.getPartitions()).isEqualTo(6);
            assertThat(fullDefinition.getReplication()).isEqualTo((short) 3);
        }

        @Test
        @DisplayName("Should allow null optional fields")
        void shouldAllowNullOptionalFields() {
            WorkflowDefinition minimalDefinition = WorkflowDefinition.builder()
                    .topic("minimal-topic")
                    .steps(List.of())
                    .build();

            assertThat(minimalDefinition.getTopic()).isEqualTo("minimal-topic");
            assertThat(minimalDefinition.getDescription()).isNull();
            assertThat(minimalDefinition.getHandler()).isNull();
            assertThat(minimalDefinition.getOnSuccessMethod()).isNull();
            assertThat(minimalDefinition.getOnFailureMethod()).isNull();
            assertThat(minimalDefinition.getTimeout()).isNull();
        }
    }

    // Test workflow class
    static class TestWorkflow implements StepprFlow {
        public void onSuccess() {
        }

        public void onFailure() {
        }
    }
}