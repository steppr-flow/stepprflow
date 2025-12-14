package io.stepprflow.monitor.controller;

import io.stepprflow.monitor.dto.OutboxStatsDto;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import io.stepprflow.monitor.outbox.OutboxMessageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for outbox statistics.
 * Provides outbox queue status for the UI dashboard.
 */
@RestController
@RequestMapping("/api/outbox")
@ConditionalOnBean(OutboxMessageRepository.class)
@Tag(name = "Outbox", description = "Outbox queue statistics")
public class OutboxController {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_WARNING = "WARNING";
    private static final long PENDING_WARNING_THRESHOLD = 100;

    private final OutboxMessageRepository outboxRepository;

    public OutboxController(OutboxMessageRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Operation(
            summary = "Get outbox statistics",
            description = "Get current outbox queue statistics")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    @GetMapping("/stats")
    public ResponseEntity<OutboxStatsDto> getStats() {
        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long sent = outboxRepository.countByStatus(OutboxStatus.SENT);
        long failed = outboxRepository.countByStatus(OutboxStatus.FAILED);

        long total = pending + sent + failed;
        double sendRate = total > 0 ? (sent * 100.0 / total) : 0.0;

        String health = calculateHealth(pending, failed);

        OutboxStatsDto dto = OutboxStatsDto.builder()
                .pending(pending)
                .sent(sent)
                .failed(failed)
                .total(total)
                .sendRate(sendRate)
                .health(health)
                .build();

        return ResponseEntity.ok(dto);
    }

    private String calculateHealth(long pending, long failed) {
        if (failed > 0) {
            return STATUS_DOWN;
        }
        if (pending > PENDING_WARNING_THRESHOLD) {
            return STATUS_WARNING;
        }
        return STATUS_UP;
    }
}
