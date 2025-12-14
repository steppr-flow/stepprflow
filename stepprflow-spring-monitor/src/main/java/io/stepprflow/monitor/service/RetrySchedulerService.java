package io.stepprflow.monitor.service;

import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.model.WorkflowExecution;
import io.stepprflow.monitor.repository.WorkflowExecutionRepository;
import io.stepprflow.monitor.util.WorkflowMessageFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service that processes pending retries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "stepprflow.monitor.retry-scheduler",
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class RetrySchedulerService {

    private final WorkflowExecutionRepository repository;
    private final MessageBroker messageBroker;
    private final MonitorProperties properties;
    private final WorkflowMessageFactory messageFactory;

    /**
     * Process pending retries in parallel for better throughput.
     */
    @Scheduled(fixedDelayString = "${stepprflow.monitor.retry-scheduler.check-interval:30000}")
    public void processPendingRetries() {
        List<WorkflowExecution> pendingRetries = repository.findPendingRetries(Instant.now());

        if (pendingRetries.isEmpty()) {
            return;
        }

        log.info("Processing {} pending retries in parallel", pendingRetries.size());

        // Process retries in parallel using virtual threads
        List<CompletableFuture<Void>> futures = pendingRetries.stream()
                .map(execution -> CompletableFuture.runAsync(() -> {
                    try {
                        processRetry(execution);
                    } catch (Exception e) {
                        log.error("Error processing retry for {}: {}",
                                execution.getExecutionId(), e.getMessage(), e);
                    }
                }))
                .toList();

        // Wait for all retries to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processRetry(WorkflowExecution execution) {
        log.info("Processing retry for workflow {} (attempt {})",
                execution.getExecutionId(),
                execution.getRetryInfo() != null ? execution.getRetryInfo().getAttempt() : 1);

        WorkflowMessage message = messageFactory.createRetryMessage(execution);
        messageBroker.send(execution.getTopic(), message);
    }

    /**
     * Clean up old executions.
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void cleanupOldExecutions() {
        log.info("Starting cleanup of old executions");

        // Clean completed executions
        Instant completedCutoff = Instant.now().minus(properties.getRetention().getCompletedTtl());
        List<WorkflowExecution> oldCompleted = repository.findCompletedBefore(completedCutoff);
        if (!oldCompleted.isEmpty()) {
            repository.deleteAll(oldCompleted);
            log.info("Deleted {} old completed executions", oldCompleted.size());
        }

        // Clean failed executions
        Instant failedCutoff = Instant.now().minus(properties.getRetention().getFailedTtl());
        List<WorkflowExecution> oldFailed = repository.findFailedBefore(failedCutoff);
        if (!oldFailed.isEmpty()) {
            repository.deleteAll(oldFailed);
            log.info("Deleted {} old failed executions", oldFailed.size());
        }
    }
}
