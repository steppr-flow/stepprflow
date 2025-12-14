package io.stepprflow.core.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of workflow metrics for API exposure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsSummary {

    /**
     * Topic name or "_global" for aggregate metrics.
     */
    private String topic;

    /**
     * Service name that owns this workflow.
     */
    private String serviceName;

    /**
     * Total workflows started.
     */
    private long workflowsStarted;

    /**
     * Total workflows completed successfully.
     */
    private long workflowsCompleted;

    /**
     * Total workflows failed.
     */
    private long workflowsFailed;

    /**
     * Total workflows cancelled.
     */
    private long workflowsCancelled;

    /**
     * Currently active workflows.
     */
    private long workflowsActive;

    /**
     * Total retry attempts.
     */
    private long retryCount;

    /**
     * Total messages sent to DLQ.
     */
    private long dlqCount;

    /**
     * Average workflow duration in milliseconds.
     */
    private double avgWorkflowDurationMs;

    /**
     * Success rate percentage (completed / started * 100).
     */
    private double successRate;

    /**
     * Calculate success rate.
     *
     * @return the success rate percentage
     */
    public double getSuccessRate() {
        if (successRate > 0) {
            return successRate;
        }
        return workflowsStarted > 0
                ? (double) workflowsCompleted / workflowsStarted * 100 : 0;
    }

    /**
     * Calculate failure rate percentage.
     *
     * @return the failure rate percentage
     */
    public double getFailureRate() {
        return workflowsStarted > 0
                ? (double) workflowsFailed / workflowsStarted * 100 : 0;
    }

    /**
     * Get throughput (completed + failed per started).
     *
     * @return the throughput rate percentage
     */
    public double getThroughputRate() {
        return workflowsStarted > 0
                ? (double) (workflowsCompleted + workflowsFailed)
                / workflowsStarted * 100 : 0;
    }
}
