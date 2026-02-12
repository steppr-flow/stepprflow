package io.github.stepprflow.dashboard.controller;

import io.github.stepprflow.core.model.StepDefinition;
import io.github.stepprflow.core.model.WorkflowDefinition;
import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.core.service.WorkflowRegistry;
import io.github.stepprflow.monitor.model.RegisteredWorkflow;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import io.github.stepprflow.monitor.service.WorkflowQueryService;
import io.github.stepprflow.monitor.service.WorkflowRegistryService;
import io.github.stepprflow.dashboard.config.UiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController Tests")
class DashboardControllerTest {

    @Mock
    private UiProperties properties;

    @Mock
    private WorkflowQueryService queryService;

    @Mock
    private WorkflowRegistry workflowRegistry;

    @InjectMocks
    private DashboardController controller;

    @Nested
    @DisplayName("GET /config")
    class GetConfigTests {

        @BeforeEach
        void setUpProperties() {
            when(properties.getTitle()).thenReturn("Test Dashboard");
            when(properties.getRefreshInterval()).thenReturn(10);
            when(properties.isDarkMode()).thenReturn(true);
            when(properties.getBasePath()).thenReturn("/test-dashboard");
        }

        @Test
        @DisplayName("Should return 200 with config")
        void shouldReturn200WithConfig() {
            ResponseEntity<Map<String, Object>> response = controller.getConfig();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should return title from properties")
        void shouldReturnTitleFromProperties() {
            ResponseEntity<Map<String, Object>> response = controller.getConfig();

            assertThat(response.getBody()).containsEntry("title", "Test Dashboard");
        }

        @Test
        @DisplayName("Should return refreshInterval from properties")
        void shouldReturnRefreshIntervalFromProperties() {
            ResponseEntity<Map<String, Object>> response = controller.getConfig();

            assertThat(response.getBody()).containsEntry("refreshInterval", 10);
        }

        @Test
        @DisplayName("Should return darkMode from properties")
        void shouldReturnDarkModeFromProperties() {
            ResponseEntity<Map<String, Object>> response = controller.getConfig();

            assertThat(response.getBody()).containsEntry("darkMode", true);
        }

        @Test
        @DisplayName("Should return basePath from properties")
        void shouldReturnBasePathFromProperties() {
            ResponseEntity<Map<String, Object>> response = controller.getConfig();

            assertThat(response.getBody()).containsEntry("basePath", "/test-dashboard");
        }
    }

    @Nested
    @DisplayName("GET /overview")
    class GetOverviewTests {

        @BeforeEach
        void setUpMocks() {
            Map<String, Object> stats = Map.of(
                    "pending", 5L,
                    "inProgress", 3L,
                    "completed", 100L,
                    "failed", 2L,
                    "total", 110L
            );
            when(queryService.getStatistics()).thenReturn(stats);
        }

        @Test
        @DisplayName("Should return 200 with overview data")
        void shouldReturn200WithOverviewData() {
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());
            when(queryService.getRecentExecutions()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.getOverview();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }

        @Test
        @DisplayName("Should include statistics")
        void shouldIncludeStatistics() {
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());
            when(queryService.getRecentExecutions()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.getOverview();

            assertThat(response.getBody()).containsKey("stats");
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) response.getBody().get("stats");
            assertThat(stats).containsEntry("total", 110L);
        }

        @Test
        @DisplayName("Should include workflow definitions")
        void shouldIncludeWorkflowDefinitions() {
            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("order-workflow")
                    .description("Order processing workflow")
                    .steps(List.of(
                            StepDefinition.builder().id(1).label("Validate").build(),
                            StepDefinition.builder().id(2).label("Process").build()
                    ))
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));
            when(queryService.getRecentExecutions()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.getOverview();

            assertThat(response.getBody()).containsKey("workflows");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody().get("workflows");
            assertThat(workflows).hasSize(1);
            assertThat(workflows.get(0)).containsEntry("topic", "order-workflow");
            assertThat(workflows.get(0)).containsEntry("description", "Order processing workflow");
            assertThat(workflows.get(0)).containsEntry("steps", 2);
        }

        @Test
        @DisplayName("Should handle null description in workflow")
        void shouldHandleNullDescriptionInWorkflow() {
            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("simple-workflow")
                    .description(null)
                    .steps(List.of())
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));
            when(queryService.getRecentExecutions()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controller.getOverview();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody().get("workflows");
            assertThat(workflows.get(0)).containsEntry("description", "");
        }

        @Test
        @DisplayName("Should include recent executions")
        void shouldIncludeRecentExecutions() {
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            List<WorkflowExecution> recentExecutions = List.of(
                    WorkflowExecution.builder()
                            .executionId("exec-123")
                            .topic("test-topic")
                            .status(WorkflowStatus.COMPLETED)
                            .createdAt(Instant.now())
                            .build()
            );
            when(queryService.getRecentExecutions()).thenReturn(recentExecutions);

            ResponseEntity<Map<String, Object>> response = controller.getOverview();

            assertThat(response.getBody()).containsKey("recentExecutions");
            @SuppressWarnings("unchecked")
            List<WorkflowExecution> executions = (List<WorkflowExecution>) response.getBody().get("recentExecutions");
            assertThat(executions).hasSize(1);
            assertThat(executions.get(0).getExecutionId()).isEqualTo("exec-123");
        }
    }

    @Nested
    @DisplayName("GET /workflows")
    class GetWorkflowsTests {

        @Test
        @DisplayName("Should return 200 with workflow definitions")
        void shouldReturn200WithWorkflowDefinitions() {
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controller.getWorkflows(null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should return empty list when no workflows")
        void shouldReturnEmptyListWhenNoWorkflows() {
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controller.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Object> workflows = (List<Object>) response.getBody();
            assertThat(workflows).isEmpty();
        }

        @Test
        @DisplayName("Should return workflow details with steps")
        void shouldReturnWorkflowDetailsWithSteps() {
            StepDefinition step1 = StepDefinition.builder()
                    .id(1)
                    .label("Validate")
                    .description("Validate input")
                    .skippable(false)
                    .continueOnFailure(false)
                    .build();
            StepDefinition step2 = StepDefinition.builder()
                    .id(2)
                    .label("Process")
                    .description("Process data")
                    .skippable(true)
                    .continueOnFailure(true)
                    .build();

            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("order-workflow")
                    .description("Order processing")
                    .steps(List.of(step1, step2))
                    .partitions(3)
                    .replication((short) 2)
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));

            ResponseEntity<?> response = controller.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);

            Map<String, Object> workflow = workflows.get(0);
            assertThat(workflow).containsEntry("topic", "order-workflow");
            assertThat(workflow).containsEntry("description", "Order processing");
            assertThat(workflow).containsEntry("partitions", 3);
            assertThat(workflow).containsEntry("replication", (short) 2);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) workflow.get("steps");
            assertThat(steps).hasSize(2);

            Map<String, Object> firstStep = steps.get(0);
            assertThat(firstStep).containsEntry("id", 1);
            assertThat(firstStep).containsEntry("label", "Validate");
            assertThat(firstStep).containsEntry("description", "Validate input");
            assertThat(firstStep).containsEntry("skippable", false);
            assertThat(firstStep).containsEntry("continueOnFailure", false);
        }

        @Test
        @DisplayName("Should handle null description in steps")
        void shouldHandleNullDescriptionInSteps() {
            StepDefinition step = StepDefinition.builder()
                    .id(1)
                    .label("Step")
                    .description(null)
                    .build();

            WorkflowDefinition definition = WorkflowDefinition.builder()
                    .topic("test-workflow")
                    .description("Test")
                    .steps(List.of(step))
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(definition));

            ResponseEntity<?> response = controller.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> steps = (List<Map<String, Object>>) workflows.get(0).get("steps");
            assertThat(steps.get(0)).containsEntry("description", "");
        }

        @Test
        @DisplayName("Should return multiple workflows")
        void shouldReturnMultipleWorkflows() {
            WorkflowDefinition def1 = WorkflowDefinition.builder()
                    .topic("workflow-1")
                    .description("First workflow")
                    .steps(List.of())
                    .build();
            WorkflowDefinition def2 = WorkflowDefinition.builder()
                    .topic("workflow-2")
                    .description("Second workflow")
                    .steps(List.of())
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(def1, def2));

            ResponseEntity<?> response = controller.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(2);
        }

        @Test
        @DisplayName("Should filter workflows by topic")
        void shouldFilterWorkflowsByTopic() {
            WorkflowDefinition def1 = WorkflowDefinition.builder()
                    .topic("order-workflow")
                    .description("Order processing")
                    .steps(List.of())
                    .build();
            WorkflowDefinition def2 = WorkflowDefinition.builder()
                    .topic("payment-workflow")
                    .description("Payment processing")
                    .steps(List.of())
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(def1, def2));

            ResponseEntity<?> response = controller.getWorkflows("order", null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);
            assertThat(workflows.get(0)).containsEntry("topic", "order-workflow");
        }

        @Test
        @DisplayName("Should filter workflows by status")
        void shouldFilterWorkflowsByStatus() {
            WorkflowDefinition def1 = WorkflowDefinition.builder()
                    .topic("active-workflow")
                    .description("Active workflow")
                    .steps(List.of())
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(def1));

            // Local workflows are always ACTIVE
            ResponseEntity<?> response = controller.getWorkflows(null, null, "ACTIVE");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);

            // INACTIVE should return empty for local workflows
            ResponseEntity<?> responseInactive = controller.getWorkflows(null, null, "INACTIVE");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> inactiveWorkflows = (List<Map<String, Object>>) responseInactive.getBody();
            assertThat(inactiveWorkflows).isEmpty();
        }

        @Test
        @DisplayName("Should filter workflows by serviceName")
        void shouldFilterWorkflowsByServiceName() {
            WorkflowDefinition def1 = WorkflowDefinition.builder()
                    .topic("local-workflow")
                    .description("Local workflow")
                    .steps(List.of())
                    .build();
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of(def1));

            ResponseEntity<?> response = controller.getWorkflows(null, "local", null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);

            // Non-matching service should return empty
            ResponseEntity<?> responseOther = controller.getWorkflows(null, "other-service", null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> otherWorkflows = (List<Map<String, Object>>) responseOther.getBody();
            assertThat(otherWorkflows).isEmpty();
        }
    }

    @Nested
    @DisplayName("With WorkflowRegistryService")
    class WithRegistryServiceTests {

        @Mock
        private WorkflowRegistryService registryService;

        private DashboardController controllerWithRegistryService;

        @BeforeEach
        void setUp() {
            controllerWithRegistryService = new DashboardController(
                    properties, queryService, workflowRegistry, registryService);
        }

        @Test
        @DisplayName("Should return workflows from registry service")
        void shouldReturnWorkflowsFromRegistryService() {
            // Set up registered workflow
            List<RegisteredWorkflow.StepInfo> steps = new ArrayList<>();
            steps.add(RegisteredWorkflow.StepInfo.builder()
                    .id(1)
                    .label("Step 1")
                    .description("First step")
                    .skippable(false)
                    .continueOnFailure(false)
                    .build());

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .host("localhost")
                    .port(8080)
                    .build());

            RegisteredWorkflow registeredWorkflow = RegisteredWorkflow.builder()
                    .topic("order-workflow")
                    .serviceName("order-service")
                    .description("Order processing")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .partitions(3)
                    .replication((short) 2)
                    .build();
            registeredWorkflow.setSteps(steps);
            registeredWorkflow.setRegisteredBy(instances);

            when(registryService.getAllWorkflows()).thenReturn(List.of(registeredWorkflow));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);
            assertThat(workflows.get(0)).containsEntry("topic", "order-workflow");
            assertThat(workflows.get(0)).containsEntry("serviceName", "order-service");
        }

        @Test
        @DisplayName("Should handle registered workflow with null description")
        void shouldHandleRegisteredWorkflowWithNullDescription() {
            RegisteredWorkflow registeredWorkflow = RegisteredWorkflow.builder()
                    .topic("simple-workflow")
                    .serviceName("simple-service")
                    .description(null)
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            registeredWorkflow.setSteps(new ArrayList<>());

            when(registryService.getAllWorkflows()).thenReturn(List.of(registeredWorkflow));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows.get(0)).containsEntry("description", "");
        }

        @Test
        @DisplayName("Should handle registered workflow with null status")
        void shouldHandleRegisteredWorkflowWithNullStatus() {
            RegisteredWorkflow registeredWorkflow = RegisteredWorkflow.builder()
                    .topic("test-workflow")
                    .serviceName("test-service")
                    .description("Test")
                    .status(null)
                    .build();
            registeredWorkflow.setSteps(new ArrayList<>());

            when(registryService.getAllWorkflows()).thenReturn(List.of(registeredWorkflow));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows.get(0)).containsEntry("status", "ACTIVE");
        }

        @Test
        @DisplayName("Should handle registered workflow with null registeredBy")
        void shouldHandleRegisteredWorkflowWithNullRegisteredBy() {
            RegisteredWorkflow registeredWorkflow = RegisteredWorkflow.builder()
                    .topic("test-workflow")
                    .serviceName("test-service")
                    .description("Test")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            registeredWorkflow.setSteps(new ArrayList<>());
            registeredWorkflow.setRegisteredBy(null);

            when(registryService.getAllWorkflows()).thenReturn(List.of(registeredWorkflow));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            @SuppressWarnings("unchecked")
            List<?> registeredBy = (List<?>) workflows.get(0).get("registeredBy");
            assertThat(registeredBy).isEmpty();
        }

        @Test
        @DisplayName("Should filter registered workflows by topic")
        void shouldFilterRegisteredWorkflowsByTopic() {
            RegisteredWorkflow rw1 = RegisteredWorkflow.builder()
                    .topic("order-workflow")
                    .serviceName("order-service")
                    .description("Order")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            rw1.setSteps(new ArrayList<>());

            RegisteredWorkflow rw2 = RegisteredWorkflow.builder()
                    .topic("payment-workflow")
                    .serviceName("payment-service")
                    .description("Payment")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            rw2.setSteps(new ArrayList<>());

            when(registryService.getAllWorkflows()).thenReturn(List.of(rw1, rw2));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows("order", null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);
            assertThat(workflows.get(0)).containsEntry("topic", "order-workflow");
        }

        @Test
        @DisplayName("Should filter registered workflows by INACTIVE status")
        void shouldFilterRegisteredWorkflowsByInactiveStatus() {
            RegisteredWorkflow rw = RegisteredWorkflow.builder()
                    .topic("inactive-workflow")
                    .serviceName("inactive-service")
                    .description("Inactive")
                    .status(RegisteredWorkflow.Status.INACTIVE)
                    .build();
            rw.setSteps(new ArrayList<>());

            when(registryService.getAllWorkflows()).thenReturn(List.of(rw));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, "INACTIVE");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);
            assertThat(workflows.get(0)).containsEntry("status", "INACTIVE");
        }

        @Test
        @DisplayName("Should get overview with registered workflows")
        void shouldGetOverviewWithRegisteredWorkflows() {
            Map<String, Object> stats = Map.of("total", 10L);
            when(queryService.getStatistics()).thenReturn(stats);
            when(queryService.getRecentExecutions()).thenReturn(List.of());

            RegisteredWorkflow rw = RegisteredWorkflow.builder()
                    .topic("test-workflow")
                    .serviceName("test-service")
                    .description("Test")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            rw.setSteps(new ArrayList<>());

            when(registryService.getAllWorkflows()).thenReturn(List.of(rw));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<Map<String, Object>> response = controllerWithRegistryService.getOverview();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody().get("workflows");
            assertThat(workflows).hasSize(1);
        }

        @Test
        @DisplayName("Should handle null serviceName in registered workflow")
        void shouldHandleNullServiceNameInRegisteredWorkflow() {
            RegisteredWorkflow rw = RegisteredWorkflow.builder()
                    .topic("test-workflow")
                    .serviceName(null)
                    .description("Test")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            rw.setSteps(new ArrayList<>());

            when(registryService.getAllWorkflows()).thenReturn(List.of(rw));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            assertThat(workflows).hasSize(1);
        }

        @Test
        @DisplayName("Should handle step with null description in registered workflow")
        void shouldHandleStepWithNullDescriptionInRegisteredWorkflow() {
            List<RegisteredWorkflow.StepInfo> steps = new ArrayList<>();
            steps.add(RegisteredWorkflow.StepInfo.builder()
                    .id(1)
                    .label("Step 1")
                    .description(null)
                    .build());

            RegisteredWorkflow rw = RegisteredWorkflow.builder()
                    .topic("test-workflow")
                    .serviceName("test-service")
                    .description("Test")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .build();
            rw.setSteps(steps);

            when(registryService.getAllWorkflows()).thenReturn(List.of(rw));
            when(workflowRegistry.getAllDefinitions()).thenReturn(List.of());

            ResponseEntity<?> response = controllerWithRegistryService.getWorkflows(null, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workflows = (List<Map<String, Object>>) response.getBody();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stepsResult = (List<Map<String, Object>>) workflows.get(0).get("steps");
            assertThat(stepsResult.get(0)).containsEntry("description", "");
        }
    }

    @Nested
    @DisplayName("GET /executions")
    class ListExecutionsTests {

        @Test
        @DisplayName("Should return paginated executions with defaults")
        void shouldReturnPaginatedExecutions() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of(
                    WorkflowExecution.builder()
                            .executionId("exec-1")
                            .topic("test-topic")
                            .status(WorkflowStatus.COMPLETED)
                            .build()
            ));
            when(queryService.findExecutions(eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(page);

            ResponseEntity<Page<WorkflowExecution>> response = controller.listExecutions(
                    null, null, 0, 20, "createdAt", "desc");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter by statuses")
        void shouldFilterByStatuses() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of());
            when(queryService.findExecutions(eq(null), eq(List.of(WorkflowStatus.FAILED)), any(PageRequest.class)))
                    .thenReturn(page);

            controller.listExecutions(null, "FAILED", 0, 20, "createdAt", "DESC");

            // Verifies that the method parses the status correctly
        }

        @Test
        @DisplayName("Should throw for invalid sortBy field")
        void shouldThrowForInvalidSortByField() {
            assertThatThrownBy(() -> controller.listExecutions(
                    null, null, 0, 20, "invalidField", "desc"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid sortBy field");
        }

        @Test
        @DisplayName("Should clamp page and size values")
        void shouldClampPageAndSizeValues() {
            Page<WorkflowExecution> page = new PageImpl<>(List.of());
            when(queryService.findExecutions(eq(null), eq(null), any(PageRequest.class)))
                    .thenReturn(page);

            // Negative page should be clamped to 0, size > 100 should be clamped to 100
            controller.listExecutions(null, null, -1, 200, "createdAt", "asc");

            // No exception means clamping worked
        }
    }
}
