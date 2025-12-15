package io.stepprflow.monitor.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegisteredWorkflow Tests")
class RegisteredWorkflowTest {

    @Nested
    @DisplayName("getSteps()")
    class GetStepsTests {

        @Test
        @DisplayName("Should return empty list when steps is null")
        void shouldReturnEmptyListWhenStepsIsNull() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("test-topic")
                    .build();

            List<RegisteredWorkflow.StepInfo> result = workflow.getSteps();

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return defensive copy when steps has elements")
        void shouldReturnDefensiveCopyWhenStepsHasElements() {
            RegisteredWorkflow.StepInfo step = RegisteredWorkflow.StepInfo.builder()
                    .id(1)
                    .label("Step 1")
                    .build();

            ArrayList<RegisteredWorkflow.StepInfo> steps = new ArrayList<>();
            steps.add(step);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("test-topic")
                    .steps(steps)
                    .build();

            List<RegisteredWorkflow.StepInfo> result = workflow.getSteps();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("setSteps()")
    class SetStepsTests {

        @Test
        @DisplayName("Should set to null when input is null")
        void shouldSetToNullWhenInputIsNull() {
            RegisteredWorkflow.StepInfo step = RegisteredWorkflow.StepInfo.builder()
                    .id(1)
                    .build();

            ArrayList<RegisteredWorkflow.StepInfo> steps = new ArrayList<>();
            steps.add(step);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .steps(steps)
                    .build();

            workflow.setSteps(null);

            // getSteps returns empty list when internal is null
            assertThat(workflow.getSteps()).isEmpty();
        }

        @Test
        @DisplayName("Should set defensive copy when input is not null")
        void shouldSetDefensiveCopyWhenInputIsNotNull() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .build();

            RegisteredWorkflow.StepInfo step = RegisteredWorkflow.StepInfo.builder()
                    .id(2)
                    .label("Step 2")
                    .build();

            ArrayList<RegisteredWorkflow.StepInfo> steps = new ArrayList<>();
            steps.add(step);

            workflow.setSteps(steps);

            assertThat(workflow.getSteps()).hasSize(1);
            assertThat(workflow.getSteps().get(0).getId()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getRegisteredBy()")
    class GetRegisteredByTests {

        @Test
        @DisplayName("Should return empty set when registeredBy is null")
        void shouldReturnEmptySetWhenRegisteredByIsNull() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> result = workflow.getRegisteredBy();

            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return defensive copy when registeredBy has elements")
        void shouldReturnDefensiveCopyWhenRegisteredByHasElements() {
            RegisteredWorkflow.ServiceInstance instance = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-1")
                    .host("localhost")
                    .build();

            HashSet<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .registeredBy(instances)
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> result = workflow.getRegisteredBy();

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("addServiceInstance()")
    class AddServiceInstanceTests {

        @Test
        @DisplayName("Should initialize set when null and add instance")
        void shouldInitializeSetWhenNullAndAddInstance() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .build();

            RegisteredWorkflow.ServiceInstance instance = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-1")
                    .host("localhost")
                    .build();

            workflow.addServiceInstance(instance);

            assertThat(workflow.getRegisteredBy()).hasSize(1);
        }

        @Test
        @DisplayName("Should add to existing set when not null")
        void shouldAddToExistingSetWhenNotNull() {
            RegisteredWorkflow.ServiceInstance existing = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-1")
                    .host("host1")
                    .build();

            HashSet<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(existing);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .registeredBy(instances)
                    .build();

            RegisteredWorkflow.ServiceInstance newInstance = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-2")
                    .host("host2")
                    .build();

            workflow.addServiceInstance(newInstance);

            assertThat(workflow.getRegisteredBy()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("removeServiceInstancesIf()")
    class RemoveServiceInstancesIfTests {

        @Test
        @DisplayName("Should return false when registeredBy is null")
        void shouldReturnFalseWhenRegisteredByIsNull() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .build();

            boolean result = workflow.removeServiceInstancesIf(i -> true);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when instances are removed")
        void shouldReturnTrueWhenInstancesAreRemoved() {
            RegisteredWorkflow.ServiceInstance instance1 = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-1")
                    .host("localhost")
                    .lastHeartbeat(Instant.now().minusSeconds(100))
                    .build();

            RegisteredWorkflow.ServiceInstance instance2 = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-2")
                    .host("localhost")
                    .lastHeartbeat(Instant.now())
                    .build();

            HashSet<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instance1);
            instances.add(instance2);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .registeredBy(instances)
                    .build();

            Instant threshold = Instant.now().minusSeconds(50);
            boolean result = workflow.removeServiceInstancesIf(i ->
                i.getLastHeartbeat() != null && i.getLastHeartbeat().isBefore(threshold));

            assertThat(result).isTrue();
            assertThat(workflow.getRegisteredBy()).hasSize(1);
        }

        @Test
        @DisplayName("Should return false when no instances match filter")
        void shouldReturnFalseWhenNoInstancesMatchFilter() {
            RegisteredWorkflow.ServiceInstance instance = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-1")
                    .host("localhost")
                    .build();

            HashSet<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .registeredBy(instances)
                    .build();

            // Filter that never matches
            boolean result = workflow.removeServiceInstancesIf(i -> false);

            assertThat(result).isFalse();
            assertThat(workflow.getRegisteredBy()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("hasNoServiceInstances()")
    class HasNoServiceInstancesTests {

        @Test
        @DisplayName("Should return true when registeredBy is null")
        void shouldReturnTrueWhenRegisteredByIsNull() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .build();

            assertThat(workflow.hasNoServiceInstances()).isTrue();
        }

        @Test
        @DisplayName("Should return true when registeredBy is empty")
        void shouldReturnTrueWhenRegisteredByIsEmpty() {
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .registeredBy(new HashSet<>())
                    .build();

            assertThat(workflow.hasNoServiceInstances()).isTrue();
        }

        @Test
        @DisplayName("Should return false when registeredBy has instances")
        void shouldReturnFalseWhenRegisteredByHasInstances() {
            RegisteredWorkflow.ServiceInstance instance = RegisteredWorkflow.ServiceInstance.builder()
                    .instanceId("instance-1")
                    .build();

            HashSet<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .registeredBy(instances)
                    .build();

            assertThat(workflow.hasNoServiceInstances()).isFalse();
        }
    }
}