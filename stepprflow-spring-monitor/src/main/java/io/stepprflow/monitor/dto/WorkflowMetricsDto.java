package io.stepprflow.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-workflow metrics DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowMetricsDto {

    private String topic;
    private String serviceName;
    private long started;
    private long completed;
    private long failed;
    private long cancelled;
    private long active;
    private long retries;
    private long dlq;
    private double avgDurationMs;
    private double successRate;
}
