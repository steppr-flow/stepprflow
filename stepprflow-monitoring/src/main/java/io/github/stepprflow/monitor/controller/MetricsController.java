package io.github.stepprflow.monitor.controller;

import io.github.stepprflow.core.metrics.MetricsSummary;
import io.github.stepprflow.core.metrics.WorkflowMetrics;
import io.github.stepprflow.monitor.dto.MetricsDashboard;
import io.github.stepprflow.monitor.dto.WorkflowMetricsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for workflow metrics.
 * Exposes metrics for all workflows that have recorded activity.
 * Returns empty metrics if WorkflowMetrics is not available.
 */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Workflow performance metrics")
public class MetricsController {

    private final WorkflowMetrics workflowMetrics;

    @Autowired
    public MetricsController(@Autowired(required = false) WorkflowMetrics workflowMetrics) {
        this.workflowMetrics = workflowMetrics;
    }

    @Operation(summary = "Get metrics dashboard", description = "Get global metrics and per-workflow breakdown")
    @ApiResponse(responseCode = "200", description = "Metrics dashboard retrieved successfully")
    @GetMapping
    public ResponseEntity<MetricsDashboard> getMetricsDashboard() {
        if (workflowMetrics == null) {
            return ResponseEntity.ok(MetricsDashboard.builder()
                    .workflowMetrics(List.of()).build());
        }
        MetricsSummary global = workflowMetrics.getGlobalSummary();

        List<WorkflowMetricsDto> byTopicService = workflowMetrics.getActiveWorkflowKeys().stream()
                .map(key -> {
                    MetricsSummary summary = workflowMetrics.getSummary(key.topic(), key.serviceName());
                    return WorkflowMetricsDto.builder()
                            .topic(key.topic())
                            .serviceName(key.serviceName())
                            .started(summary.getWorkflowsStarted())
                            .completed(summary.getWorkflowsCompleted())
                            .failed(summary.getWorkflowsFailed())
                            .cancelled(summary.getWorkflowsCancelled())
                            .active(summary.getWorkflowsActive())
                            .retries(summary.getRetryCount())
                            .dlq(summary.getDlqCount())
                            .avgDurationMs(summary.getAvgWorkflowDurationMs())
                            .successRate(summary.getSuccessRate())
                            .build();
                })
                .collect(Collectors.toList());

        MetricsDashboard dashboard = MetricsDashboard.builder()
                .totalStarted(global.getWorkflowsStarted())
                .totalCompleted(global.getWorkflowsCompleted())
                .totalFailed(global.getWorkflowsFailed())
                .totalCancelled(global.getWorkflowsCancelled())
                .totalActive(global.getWorkflowsActive())
                .totalRetries(global.getRetryCount())
                .totalDlq(global.getDlqCount())
                .globalSuccessRate(global.getSuccessRate())
                .globalFailureRate(global.getFailureRate())
                .workflowMetrics(byTopicService)
                .build();

        return ResponseEntity.ok(dashboard);
    }

    @Operation(summary = "Get metrics by topic", description = "Get metrics for a specific workflow topic")
    @ApiResponse(responseCode = "200", description = "Workflow metrics retrieved successfully")
    @GetMapping("/{topic}")
    public ResponseEntity<WorkflowMetricsDto> getWorkflowMetrics(
            @Parameter(description = "Workflow topic name") @PathVariable String topic) {
        if (workflowMetrics == null) {
            return ResponseEntity.ok(WorkflowMetricsDto.builder()
                    .topic(topic).build());
        }
        MetricsSummary summary = workflowMetrics.getSummary(topic);

        WorkflowMetricsDto dto = WorkflowMetricsDto.builder()
                .topic(topic)
                .started(summary.getWorkflowsStarted())
                .completed(summary.getWorkflowsCompleted())
                .failed(summary.getWorkflowsFailed())
                .cancelled(summary.getWorkflowsCancelled())
                .active(summary.getWorkflowsActive())
                .retries(summary.getRetryCount())
                .dlq(summary.getDlqCount())
                .avgDurationMs(summary.getAvgWorkflowDurationMs())
                .successRate(summary.getSuccessRate())
                .build();

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Get global summary", description = "Get aggregated metrics summary across all workflows")
    @ApiResponse(responseCode = "200", description = "Global summary retrieved successfully")
    @GetMapping("/summary")
    public ResponseEntity<MetricsSummary> getGlobalSummary() {
        if (workflowMetrics == null) {
            return ResponseEntity.ok(new MetricsSummary());
        }
        return ResponseEntity.ok(workflowMetrics.getGlobalSummary());
    }
}
