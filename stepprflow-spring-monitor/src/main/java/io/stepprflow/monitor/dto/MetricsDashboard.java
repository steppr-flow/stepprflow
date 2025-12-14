package io.stepprflow.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Dashboard metrics response DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDashboard {

    // Global counters
    private long totalStarted;
    private long totalCompleted;
    private long totalFailed;
    private long totalCancelled;
    private long totalActive;
    private long totalRetries;
    private long totalDlq;

    // Global rates
    private double globalSuccessRate;
    private double globalFailureRate;

    // Per-workflow metrics
    private List<WorkflowMetricsDto> workflowMetrics;
}
