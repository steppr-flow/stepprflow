package io.github.stepprflow.dashboard.controller;

import io.github.stepprflow.core.model.WorkflowStatus;
import io.github.stepprflow.core.service.WorkflowRegistry;
import io.github.stepprflow.monitor.model.RegisteredWorkflow;
import io.github.stepprflow.monitor.model.WorkflowExecution;
import io.github.stepprflow.monitor.service.WorkflowQueryService;
import io.github.stepprflow.monitor.service.WorkflowRegistryService;
import io.github.stepprflow.dashboard.config.UiProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for dashboard-specific endpoints.
 * <p>
 * This controller provides endpoints specific to the dashboard module:
 * - UI configuration (title, refresh interval, dark mode)
 * - Overview combining local workflows and registered services
 * - Workflow definitions from both local registry and MongoDB
 * <p>
 * For workflow execution operations (list, get, resume, cancel, payload updates),
 * use the /api/workflows endpoints provided by stepprflow-monitor.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard configuration and overview endpoints")
public class DashboardController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "status", "topic", "currentStep"
    );

    private final UiProperties properties;
    private final WorkflowQueryService queryService;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowRegistryService registryService;

    @Autowired
    public DashboardController(
            UiProperties properties,
            WorkflowQueryService queryService,
            @Autowired(required = false) WorkflowRegistry workflowRegistry,
            @Autowired(required = false) WorkflowRegistryService registryService) {
        this.properties = properties;
        this.queryService = queryService;
        this.workflowRegistry = workflowRegistry;
        this.registryService = registryService;
    }

    @Operation(summary = "Get dashboard configuration",
            description = "Get UI configuration settings")
    @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully")
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("title", properties.getTitle());
        config.put("refreshInterval", properties.getRefreshInterval());
        config.put("darkMode", properties.isDarkMode());
        config.put("basePath", properties.getBasePath());
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Get dashboard overview",
            description = "Get dashboard overview data")
    @ApiResponse(responseCode = "200", description = "Overview data retrieved successfully")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();

        // Statistics
        overview.put("stats", queryService.getStatistics());

        // Available workflows (combined from local registry and registered services)
        overview.put("workflows", getCombinedWorkflows().stream()
                .map(w -> Map.of(
                        "topic", w.get("topic"),
                        "description", w.get("description") != null ? w.get("description") : "",
                        "steps", w.get("stepCount")
                ))
                .toList());

        // Recent executions
        overview.put("recentExecutions", queryService.getRecentExecutions());

        return ResponseEntity.ok(overview);
    }

    @Operation(summary = "Get workflow definitions",
            description = "Get all workflow definitions with their steps and configuration")
    @ApiResponse(responseCode = "200", description = "Workflow definitions retrieved successfully")
    @GetMapping("/workflows")
    public ResponseEntity<?> getWorkflows(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(getCombinedWorkflowDefinitions(topic, serviceName, status));
    }

    @Operation(summary = "List executions",
            description = "Get a paginated list of workflow executions with optional filtering")
    @ApiResponse(responseCode = "200", description = "List of executions")
    @GetMapping("/executions")
    public ResponseEntity<Page<WorkflowExecution>> listExecutions(
            @Parameter(description = "Filter by workflow topic")
            @RequestParam(required = false) String topic,
            @Parameter(description = "Filter by statuses (comma-separated)")
            @RequestParam(required = false) String statuses,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        // Validate sortBy field
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy field: " + sortBy + ". Allowed: " + ALLOWED_SORT_FIELDS);
        }

        // Clamp page and size to valid ranges
        page = Math.max(0, page);
        size = Math.max(1, Math.min(100, size));

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        List<WorkflowStatus> statusList = null;
        if (statuses != null && !statuses.isBlank()) {
            statusList = Arrays.stream(statuses.split(","))
                    .map(String::trim)
                    .map(WorkflowStatus::valueOf)
                    .toList();
        }

        return ResponseEntity.ok(queryService.findExecutions(topic, statusList, pageable));
    }

    /**
     * Get combined workflow definitions from local registry and registered services.
     * Uses composite key (topic + serviceName) to allow same topic from different services.
     */
    private List<Map<String, Object>> getCombinedWorkflowDefinitions(
            String topicFilter, String serviceNameFilter, String statusFilter) {
        Map<String, Map<String, Object>> workflowsByKey = new LinkedHashMap<>();

        // First, add workflows from registered services (MongoDB)
        if (registryService != null) {
            for (RegisteredWorkflow rw : registryService.getAllWorkflows()) {
                String workflowStatus = rw.getStatus() != null ? rw.getStatus().name() : "ACTIVE";
                String workflowServiceName = rw.getServiceName();

                // Apply filters
                if (!matchesFilters(rw.getTopic(), workflowServiceName, workflowStatus,
                        topicFilter, serviceNameFilter, statusFilter)) {
                    continue;
                }

                Map<String, Object> workflow = new HashMap<>();
                workflow.put("topic", rw.getTopic());
                workflow.put("serviceName", workflowServiceName);
                workflow.put("description", rw.getDescription() != null ? rw.getDescription() : "");
                workflow.put("status", workflowStatus);
                workflow.put("steps", rw.getSteps().stream()
                        .map(step -> {
                            Map<String, Object> stepMap = new HashMap<>();
                            stepMap.put("id", step.getId());
                            stepMap.put("label", step.getLabel());
                            stepMap.put("description", step.getDescription() != null ? step.getDescription() : "");
                            stepMap.put("skippable", step.isSkippable());
                            stepMap.put("continueOnFailure", step.isContinueOnFailure());
                            return stepMap;
                        })
                        .toList());
                workflow.put("partitions", rw.getPartitions());
                workflow.put("replication", rw.getReplication());
                workflow.put("registeredBy", rw.getRegisteredBy() != null ?
                        rw.getRegisteredBy().stream()
                                .map(si -> Map.of(
                                        "serviceName", si.getServiceName(),
                                        "instanceId", si.getInstanceId(),
                                        "host", si.getHost(),
                                        "port", si.getPort()
                                ))
                                .toList() : List.of());
                // Use composite key: topic + serviceName
                String key = rw.getTopic() + ":" + (workflowServiceName != null ? workflowServiceName : "unknown");
                workflowsByKey.put(key, workflow);
            }
        }

        // Then, add workflows from local registry (may override or add new ones)
        if (workflowRegistry != null) {
            for (var def : workflowRegistry.getAllDefinitions()) {
                String workflowStatus = "ACTIVE"; // Local workflows are always active

                // Apply filters
                if (!matchesFilters(def.getTopic(), "local", workflowStatus,
                        topicFilter, serviceNameFilter, statusFilter)) {
                    continue;
                }

                String key = def.getTopic() + ":local";
                if (!workflowsByKey.containsKey(key)) {
                    Map<String, Object> workflow = new HashMap<>();
                    workflow.put("topic", def.getTopic());
                    workflow.put("serviceName", "local");
                    workflow.put("description", def.getDescription() != null ? def.getDescription() : "");
                    workflow.put("status", workflowStatus);
                    workflow.put("steps", def.getSteps().stream()
                            .map(step -> {
                                Map<String, Object> stepMap = new HashMap<>();
                                stepMap.put("id", step.getId());
                                stepMap.put("label", step.getLabel());
                                stepMap.put("description", step.getDescription() != null ? step.getDescription() : "");
                                stepMap.put("skippable", step.isSkippable());
                                stepMap.put("continueOnFailure", step.isContinueOnFailure());
                                return stepMap;
                            })
                            .toList());
                    workflow.put("partitions", def.getPartitions());
                    workflow.put("replication", def.getReplication());
                    workflow.put("registeredBy", List.of(Map.of("serviceName", "local", "instanceId", "local")));
                    workflowsByKey.put(key, workflow);
                }
            }
        }

        return new ArrayList<>(workflowsByKey.values());
    }

    /**
     * Check if a workflow matches the given filters.
     */
    private boolean matchesFilters(String topic, String serviceName, String status,
                                   String topicFilter, String serviceNameFilter, String statusFilter) {
        // Topic filter: partial match (case-insensitive)
        if (topicFilter != null && !topicFilter.isBlank()) {
            if (topic == null || !topic.toLowerCase().contains(topicFilter.toLowerCase())) {
                return false;
            }
        }

        // ServiceName filter: exact match
        if (serviceNameFilter != null && !serviceNameFilter.isBlank()) {
            if (!serviceNameFilter.equals(serviceName)) {
                return false;
            }
        }

        // Status filter: exact match
        if (statusFilter != null && !statusFilter.isBlank()) {
            if (!statusFilter.equalsIgnoreCase(status)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get combined workflows for overview (simplified format).
     * Uses composite key (topic + serviceName) to allow same topic from different services.
     */
    private List<Map<String, Object>> getCombinedWorkflows() {
        Map<String, Map<String, Object>> workflowsByKey = new LinkedHashMap<>();

        // First, add workflows from registered services
        if (registryService != null) {
            for (RegisteredWorkflow rw : registryService.getAllWorkflows()) {
                Map<String, Object> workflow = new HashMap<>();
                workflow.put("topic", rw.getTopic());
                workflow.put("serviceName", rw.getServiceName());
                workflow.put("description", rw.getDescription() != null ? rw.getDescription() : "");
                workflow.put("status", rw.getStatus() != null ? rw.getStatus().name() : "ACTIVE");
                workflow.put("stepCount", rw.getSteps() != null ? rw.getSteps().size() : 0);
                String key = rw.getTopic() + ":" + (rw.getServiceName() != null ? rw.getServiceName() : "unknown");
                workflowsByKey.put(key, workflow);
            }
        }

        // Then, add workflows from local registry
        if (workflowRegistry != null) {
            for (var def : workflowRegistry.getAllDefinitions()) {
                String key = def.getTopic() + ":local";
                if (!workflowsByKey.containsKey(key)) {
                    Map<String, Object> workflow = new HashMap<>();
                    workflow.put("topic", def.getTopic());
                    workflow.put("serviceName", "local");
                    workflow.put("description", def.getDescription() != null ? def.getDescription() : "");
                    workflow.put("status", "ACTIVE");
                    workflow.put("stepCount", def.getTotalSteps());
                    workflowsByKey.put(key, workflow);
                }
            }
        }

        return new ArrayList<>(workflowsByKey.values());
    }
}
