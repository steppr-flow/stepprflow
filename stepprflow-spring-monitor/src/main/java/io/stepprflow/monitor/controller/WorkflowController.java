package io.stepprflow.monitor.controller;

import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.service.PayloadManagementService;
import io.stepprflow.monitor.service.WorkflowCommandService;
import io.stepprflow.monitor.service.WorkflowQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST API for workflow monitoring.
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Validated
@Tag(name = "Workflows", description = "Workflow execution monitoring and control")
public class WorkflowController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "status", "topic", "currentStep"
    );

    private final WorkflowQueryService queryService;
    private final WorkflowCommandService commandService;
    private final PayloadManagementService payloadService;

    @Operation(summary = "Get execution by ID",
            description = "Retrieve a specific workflow execution by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Execution found",
                    content = @Content(schema = @Schema(implementation = WorkflowExecution.class))),
            @ApiResponse(responseCode = "404", description = "Execution not found", content = @Content)
    })
    @GetMapping("/{executionId}")
    public ResponseEntity<WorkflowExecution> getExecution(
            @Parameter(description = "Unique execution identifier") @PathVariable String executionId) {
        return queryService.getExecution(executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List executions",
            description = "Get a paginated list of workflow executions with optional filtering")
    @ApiResponse(responseCode = "200", description = "List of executions")
    @GetMapping
    public ResponseEntity<Page<WorkflowExecution>> listExecutions(
            @Parameter(description = "Filter by workflow topic")
            @RequestParam(required = false) String topic,
            @Parameter(description = "Filter by statuses (comma-separated)")
            @RequestParam(required = false) String statuses,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page number must be >= 0") int page,
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be >= 1")
            @Max(value = 100, message = "Page size must be <= 100") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        // Validate sortBy field
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy field: " + sortBy + ". Allowed: " + ALLOWED_SORT_FIELDS);
        }

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

    @Operation(summary = "Get recent executions",
            description = "Retrieve the most recent workflow executions (last 10)")
    @ApiResponse(responseCode = "200", description = "List of recent executions")
    @GetMapping("/recent")
    public ResponseEntity<List<WorkflowExecution>> getRecentExecutions() {
        return ResponseEntity.ok(queryService.getRecentExecutions());
    }

    @Operation(summary = "Get statistics", description = "Get aggregated workflow execution statistics")
    @ApiResponse(responseCode = "200", description = "Statistics map with counts by status")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(queryService.getStatistics());
    }

    @Operation(summary = "Resume execution",
            description = "Resume a failed or paused workflow execution")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Resume request accepted"),
            @ApiResponse(responseCode = "404", description = "Execution not found",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Cannot resume (invalid state)",
                    content = @Content)
    })
    @PostMapping("/{executionId}/resume")
    public ResponseEntity<Void> resume(
            @Parameter(description = "Execution ID to resume")
            @PathVariable String executionId,
            @Parameter(description = "Step number to resume from (optional)")
            @RequestParam(required = false) Integer fromStep) {
        commandService.resume(executionId, fromStep, "UI User");
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Cancel execution", description = "Cancel a running workflow execution")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Execution cancelled"),
            @ApiResponse(responseCode = "404", description = "Execution not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Cannot cancel (already completed)", content = @Content)
    })
    @DeleteMapping("/{executionId}")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "Execution ID to cancel") @PathVariable String executionId) {
        commandService.cancel(executionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update payload field",
            description = "Update a specific field in the execution payload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payload updated",
                    content = @Content(schema = @Schema(implementation = WorkflowExecution.class))),
            @ApiResponse(responseCode = "404", description = "Execution not found",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Cannot update payload",
                    content = @Content)
    })
    @PatchMapping("/{executionId}/payload")
    public ResponseEntity<WorkflowExecution> updatePayloadField(
            @Parameter(description = "Execution ID") @PathVariable String executionId,
            @Valid @RequestBody PayloadFieldUpdateRequest request) {
        WorkflowExecution updated = payloadService.updatePayloadField(
                executionId,
                request.getFieldPath(),
                request.getNewValue(),
                request.getChangedBy(),
                request.getReason()
        );
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Restore payload",
            description = "Restore payload to its original state")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payload restored",
                    content = @Content(schema = @Schema(implementation = WorkflowExecution.class))),
            @ApiResponse(responseCode = "404", description = "Execution not found",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Cannot restore payload",
                    content = @Content)
    })
    @PostMapping("/{executionId}/payload/restore")
    public ResponseEntity<WorkflowExecution> restorePayload(
            @Parameter(description = "Execution ID") @PathVariable String executionId) {
        WorkflowExecution restored = payloadService.restorePayload(executionId);
        return ResponseEntity.ok(restored);
    }

    @Data
    @Schema(description = "Request to update a payload field")
    public static class PayloadFieldUpdateRequest {
        /**
         * Valid field path pattern: alphanumeric, dots, underscores, hyphens, and array notation.
         * Examples: "customer.email", "items[0].name", "order_id"
         */
        private static final String FIELD_PATH_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_.\\-\\[\\]]*$";

        @Schema(description = "JSON path to the field (e.g., 'customer.email')", example = "customer.email")
        @NotBlank(message = "Field path is required")
        @Size(max = 255, message = "Field path must not exceed 255 characters")
        @Pattern(regexp = FIELD_PATH_PATTERN, message = "Field path contains invalid characters")
        private String fieldPath;

        @Schema(description = "New value for the field", example = "updated@email.com")
        private Object newValue;

        @Schema(description = "User who made the change", example = "admin")
        @Size(max = 100, message = "ChangedBy must not exceed 100 characters")
        private String changedBy;

        @Schema(description = "Reason for the change", example = "Correcting typo in email")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        private String reason;
    }
}
