package io.stepprflow.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.stepprflow.core.model.WorkflowStatus;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * Micrometer-based metrics for workflow orchestration.
 *
 * <p>Provides the following metrics:
 * <ul>
 *   <li>stepprflow.workflow.started - Counter of started workflows
 *   (by topic)</li>
 *   <li>stepprflow.workflow.completed - Counter of completed workflows
 *   (by topic)</li>
 *   <li>stepprflow.workflow.failed - Counter of failed workflows
 *   (by topic)</li>
 *   <li>stepprflow.workflow.cancelled - Counter of cancelled workflows
 *   (by topic)</li>
 *   <li>stepprflow.workflow.active - Gauge of currently active workflows
 *   (by topic)</li>
 *   <li>stepprflow.workflow.duration - Timer of workflow execution
 *   duration (by topic, status)</li>
 *   <li>stepprflow.step.executed - Counter of executed steps
 *   (by topic, step)</li>
 *   <li>stepprflow.step.failed - Counter of failed steps
 *   (by topic, step)</li>
 *   <li>stepprflow.step.duration - Timer of step execution duration
 *   (by topic, step)</li>
 *   <li>stepprflow.step.timeout - Counter of timed out steps
 *   (by topic, step)</li>
 *   <li>stepprflow.retry.count - Counter of retry attempts
 *   (by topic)</li>
 *   <li>stepprflow.dlq.count - Counter of messages sent to DLQ
 *   (by topic)</li>
 * </ul>
 */
@Slf4j
public class WorkflowMetrics {

    private static final String PREFIX = "stepprflow";
    private static final String TAG_TOPIC = "topic";
    private static final String TAG_SERVICE = "service";
    private static final String TAG_STEP = "step";
    private static final String TAG_STATUS = "status";
    private static final String UNKNOWN_SERVICE = "unknown";

    /**
     * The meter registry.
     */
    private final MeterRegistry registry;

    /**
     * Gauges for active workflows per topic.
     */
    private final Map<String, AtomicLong> activeWorkflowsByTopic =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - started workflows.
     */
    private final Map<String, Counter> startedCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - completed workflows.
     */
    private final Map<String, Counter> completedCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - failed workflows.
     */
    private final Map<String, Counter> failedCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - cancelled workflows.
     */
    private final Map<String, Counter> cancelledCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - retry attempts.
     */
    private final Map<String, Counter> retryCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - DLQ messages.
     */
    private final Map<String, Counter> dlqCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - executed steps.
     */
    private final Map<String, Counter> stepExecutedCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - failed steps.
     */
    private final Map<String, Counter> stepFailedCounters =
            new ConcurrentHashMap<>();

    /**
     * Cached counters for performance - timed out steps.
     */
    private final Map<String, Counter> stepTimeoutCounters =
            new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param registry the meter registry
     */
    public WorkflowMetrics(final MeterRegistry registry) {
        this.registry = registry;
        log.info("WorkflowMetrics initialized with registry: {}",
                registry.getClass().getSimpleName());
    }

    // ========== Workflow Lifecycle Metrics ==========

    /**
     * Record a workflow start.
     *
     * @param topic the workflow topic
     */
    public void recordWorkflowStarted(final String topic) {
        recordWorkflowStarted(topic, null);
    }

    /**
     * Record a workflow start with service name.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     */
    public void recordWorkflowStarted(final String topic,
                                      final String serviceName) {
        String service = serviceName != null
                ? serviceName : UNKNOWN_SERVICE;
        getOrCreateCounter(startedCounters,
                PREFIX + ".workflow.started",
                TAG_TOPIC, topic, TAG_SERVICE, service).increment();
        getOrCreateActiveGauge(topic, service).incrementAndGet();
        log.debug("Recorded workflow started: topic={}, service={}",
                topic, service);
    }

    /**
     * Record a workflow completion.
     *
     * @param topic the workflow topic
     * @param duration the workflow duration
     */
    public void recordWorkflowCompleted(final String topic,
                                        final Duration duration) {
        recordWorkflowCompleted(topic, null, duration);
    }

    /**
     * Record a workflow completion with service name.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     * @param duration the workflow duration
     */
    public void recordWorkflowCompleted(final String topic,
                                        final String serviceName,
                                        final Duration duration) {
        String service = serviceName != null
                ? serviceName : UNKNOWN_SERVICE;
        getOrCreateCounter(completedCounters,
                PREFIX + ".workflow.completed",
                TAG_TOPIC, topic, TAG_SERVICE, service).increment();
        getOrCreateActiveGauge(topic, service).decrementAndGet();
        recordWorkflowDuration(topic, service,
                WorkflowStatus.COMPLETED, duration);
        log.debug("Recorded workflow completed: topic={}, service={}, "
                + "duration={}ms", topic, service, duration.toMillis());
    }

    /**
     * Record a workflow failure.
     *
     * @param topic the workflow topic
     * @param duration the workflow duration
     */
    public void recordWorkflowFailed(final String topic,
                                     final Duration duration) {
        recordWorkflowFailed(topic, null, duration);
    }

    /**
     * Record a workflow failure with service name.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     * @param duration the workflow duration
     */
    public void recordWorkflowFailed(final String topic,
                                     final String serviceName,
                                     final Duration duration) {
        String service = serviceName != null
                ? serviceName : UNKNOWN_SERVICE;
        getOrCreateCounter(failedCounters,
                PREFIX + ".workflow.failed",
                TAG_TOPIC, topic, TAG_SERVICE, service).increment();
        getOrCreateActiveGauge(topic, service).decrementAndGet();
        recordWorkflowDuration(topic, service,
                WorkflowStatus.FAILED, duration);
        log.debug("Recorded workflow failed: topic={}, service={}, "
                + "duration={}ms", topic, service, duration.toMillis());
    }

    /**
     * Record a workflow cancellation.
     *
     * @param topic the workflow topic
     */
    public void recordWorkflowCancelled(final String topic) {
        recordWorkflowCancelled(topic, null);
    }

    /**
     * Record a workflow cancellation with service name.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     */
    public void recordWorkflowCancelled(final String topic,
                                        final String serviceName) {
        String service = serviceName != null
                ? serviceName : UNKNOWN_SERVICE;
        getOrCreateCounter(cancelledCounters,
                PREFIX + ".workflow.cancelled",
                TAG_TOPIC, topic, TAG_SERVICE, service).increment();
        getOrCreateActiveGauge(topic, service).decrementAndGet();
        log.debug("Recorded workflow cancelled: topic={}, service={}",
                topic, service);
    }

    // ========== Step Metrics ==========

    /**
     * Record a step execution.
     *
     * @param topic the workflow topic
     * @param stepLabel the step label
     * @param duration the step duration
     */
    public void recordStepExecuted(final String topic,
                                   final String stepLabel,
                                   final Duration duration) {
        String key = topic + ":" + stepLabel;
        getOrCreateCounter(stepExecutedCounters,
                PREFIX + ".step.executed",
                TAG_TOPIC, topic, TAG_STEP, stepLabel).increment();
        recordStepDuration(topic, stepLabel, duration);
        log.debug("Recorded step executed: topic={}, step={}, "
                + "duration={}ms", topic, stepLabel, duration.toMillis());
    }

    /**
     * Record a step failure.
     *
     * @param topic the workflow topic
     * @param stepLabel the step label
     */
    public void recordStepFailed(final String topic,
                                 final String stepLabel) {
        String key = topic + ":" + stepLabel;
        getOrCreateCounter(stepFailedCounters,
                PREFIX + ".step.failed",
                TAG_TOPIC, topic, TAG_STEP, stepLabel).increment();
        log.debug("Recorded step failed: topic={}, step={}",
                topic, stepLabel);
    }

    /**
     * Record a step timeout.
     *
     * @param topic the workflow topic
     * @param stepLabel the step label
     */
    public void recordStepTimeout(final String topic,
                                  final String stepLabel) {
        String key = topic + ":" + stepLabel;
        getOrCreateCounter(stepTimeoutCounters,
                PREFIX + ".step.timeout",
                TAG_TOPIC, topic, TAG_STEP, stepLabel).increment();
        log.debug("Recorded step timeout: topic={}, step={}",
                topic, stepLabel);
    }

    // ========== Retry and DLQ Metrics ==========

    /**
     * Record a retry attempt.
     *
     * @param topic the workflow topic
     * @param attempt the retry attempt number
     */
    public void recordRetry(final String topic, final int attempt) {
        getOrCreateCounter(retryCounters,
                PREFIX + ".retry.count", TAG_TOPIC, topic).increment();
        log.debug("Recorded retry: topic={}, attempt={}", topic, attempt);
    }

    /**
     * Record a message sent to DLQ.
     *
     * @param topic the workflow topic
     */
    public void recordDlq(final String topic) {
        getOrCreateCounter(dlqCounters,
                PREFIX + ".dlq.count", TAG_TOPIC, topic).increment();
        log.debug("Recorded DLQ: topic={}", topic);
    }

    // ========== Duration Recording ==========

    /**
     * Record workflow duration.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     * @param status the workflow status
     * @param duration the duration
     */
    private void recordWorkflowDuration(final String topic,
                                        final String serviceName,
                                        final WorkflowStatus status,
                                        final Duration duration) {
        Timer.builder(PREFIX + ".workflow.duration")
                .tag(TAG_TOPIC, topic)
                .tag(TAG_SERVICE, serviceName)
                .tag(TAG_STATUS, status.name())
                .description("Workflow execution duration")
                .register(registry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Record step duration.
     *
     * @param topic the workflow topic
     * @param stepLabel the step label
     * @param duration the duration
     */
    private void recordStepDuration(final String topic,
                                    final String stepLabel,
                                    final Duration duration) {
        Timer.builder(PREFIX + ".step.duration")
                .tag(TAG_TOPIC, topic)
                .tag(TAG_STEP, stepLabel)
                .description("Step execution duration")
                .register(registry)
                .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ========== Helper Methods ==========

    /**
     * Get or create a counter.
     *
     * @param cache the counter cache
     * @param name the counter name
     * @param tags the tags
     * @return the counter
     */
    private Counter getOrCreateCounter(final Map<String, Counter> cache,
                                       final String name,
                                       final String... tags) {
        String key = name + ":" + String.join(":", tags);
        return cache.computeIfAbsent(key, k ->
                Counter.builder(name)
                        .tags(tags)
                        .register(registry)
        );
    }

    /**
     * Get or create an active gauge.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     * @return the atomic long for the gauge
     */
    private AtomicLong getOrCreateActiveGauge(final String topic,
                                              final String serviceName) {
        String key = topic + ":" + serviceName;
        return activeWorkflowsByTopic.computeIfAbsent(key, k -> {
            AtomicLong gauge = new AtomicLong(0);
            Gauge.builder(PREFIX + ".workflow.active",
                    gauge, AtomicLong::get)
                    .tag(TAG_TOPIC, topic)
                    .tag(TAG_SERVICE, serviceName)
                    .description("Number of active workflows")
                    .register(registry);
            return gauge;
        });
    }

    // ========== Metrics Summary (for API) ==========

    /**
     * Record for workflow identification by topic and service.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     */
    public record WorkflowKey(String topic, String serviceName) { }

    /**
     * Get a summary of all metrics for a specific topic.
     *
     * @param topic the workflow topic
     * @return the metrics summary
     */
    public MetricsSummary getSummary(final String topic) {
        return getSummary(topic, UNKNOWN_SERVICE);
    }

    /**
     * Get a summary of all metrics for a specific topic and serviceName.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     * @return the metrics summary
     */
    public MetricsSummary getSummary(final String topic,
                                     final String serviceName) {
        String key = topic + ":" + serviceName;
        return MetricsSummary.builder()
                .topic(topic)
                .serviceName(serviceName)
                .workflowsStarted(getCounterValue(startedCounters,
                        PREFIX + ".workflow.started",
                        TAG_TOPIC, topic, TAG_SERVICE, serviceName))
                .workflowsCompleted(getCounterValue(completedCounters,
                        PREFIX + ".workflow.completed",
                        TAG_TOPIC, topic, TAG_SERVICE, serviceName))
                .workflowsFailed(getCounterValue(failedCounters,
                        PREFIX + ".workflow.failed",
                        TAG_TOPIC, topic, TAG_SERVICE, serviceName))
                .workflowsCancelled(getCounterValue(cancelledCounters,
                        PREFIX + ".workflow.cancelled",
                        TAG_TOPIC, topic, TAG_SERVICE, serviceName))
                .workflowsActive(activeWorkflowsByTopic.getOrDefault(key,
                        new AtomicLong(0)).get())
                .retryCount(getCounterValue(retryCounters,
                        PREFIX + ".retry.count", TAG_TOPIC, topic))
                .dlqCount(getCounterValue(dlqCounters,
                        PREFIX + ".dlq.count", TAG_TOPIC, topic))
                .avgWorkflowDurationMs(getTimerMeanWithService(topic,
                        serviceName))
                .build();
    }

    /**
     * Get all active workflow keys (topic:serviceName pairs).
     *
     * @return the set of active workflow keys
     */
    public Set<WorkflowKey> getActiveWorkflowKeys() {
        Set<WorkflowKey> keys = new HashSet<>();
        String prefix = PREFIX + ".workflow.started:";
        for (String key : startedCounters.keySet()) {
            if (key.startsWith(prefix)) {
                // Key format:
                // stepprflow.workflow.started:topic:{topic}:service:{service}
                String remainder = key.substring(prefix.length());
                String[] parts = remainder.split(":");
                if (parts.length >= 4 && "topic".equals(parts[0])
                        && "service".equals(parts[2])) {
                    keys.add(new WorkflowKey(parts[1], parts[3]));
                }
            }
        }
        return keys;
    }

    /**
     * Get a global summary across all topics.
     *
     * @return the global metrics summary
     */
    public MetricsSummary getGlobalSummary() {
        long started = sumCounterValues(startedCounters);
        long completed = sumCounterValues(completedCounters);
        long failed = sumCounterValues(failedCounters);
        long cancelled = sumCounterValues(cancelledCounters);
        long active = activeWorkflowsByTopic.values().stream()
                .mapToLong(AtomicLong::get).sum();
        long retries = sumCounterValues(retryCounters);
        long dlq = sumCounterValues(dlqCounters);

        return MetricsSummary.builder()
                .topic("_global")
                .workflowsStarted(started)
                .workflowsCompleted(completed)
                .workflowsFailed(failed)
                .workflowsCancelled(cancelled)
                .workflowsActive(active)
                .retryCount(retries)
                .dlqCount(dlq)
                .successRate(started > 0
                        ? (double) completed / started * 100 : 0)
                .build();
    }

    /**
     * Get counter value.
     *
     * @param cache the counter cache
     * @param name the counter name
     * @param tags the tags
     * @return the counter value
     */
    private long getCounterValue(final Map<String, Counter> cache,
                                 final String name,
                                 final String... tags) {
        String key = name + ":" + String.join(":", tags);
        Counter counter = cache.get(key);
        return counter != null ? (long) counter.count() : 0L;
    }

    /**
     * Sum all counter values in a cache.
     *
     * @param cache the counter cache
     * @return the sum of all counter values
     */
    private long sumCounterValues(final Map<String, Counter> cache) {
        return cache.values().stream()
                .mapToLong(c -> (long) c.count())
                .sum();
    }

    /**
     * Get timer mean value.
     *
     * @param name the timer name
     * @param tags the tags
     * @return the mean value in milliseconds
     */
    private double getTimerMean(final String name, final String... tags) {
        Timer timer = registry.find(name).tags(tags).timer();
        return timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0;
    }

    /**
     * Get timer mean value with service tag.
     *
     * @param topic the workflow topic
     * @param serviceName the service name
     * @return the mean value in milliseconds
     */
    private double getTimerMeanWithService(final String topic,
                                           final String serviceName) {
        Timer timer = registry.find(PREFIX + ".workflow.duration")
                .tag(TAG_TOPIC, topic)
                .tag(TAG_SERVICE, serviceName)
                .timer();
        return timer != null ? timer.mean(TimeUnit.MILLISECONDS) : 0.0;
    }

    /**
     * Get all topics that have recorded metrics.
     * Extracts topic names from the startedCounters cache keys.
     *
     * @return the set of active topics
     */
    public Set<String> getActiveTopics() {
        Set<String> topics = new HashSet<>();
        String prefix = PREFIX + ".workflow.started:topic:";
        for (String key : startedCounters.keySet()) {
            if (key.startsWith(prefix)) {
                topics.add(key.substring(prefix.length()));
            }
        }
        return topics;
    }
}
