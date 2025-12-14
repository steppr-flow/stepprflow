package io.stepprflow.monitor.controller;

import io.stepprflow.monitor.dto.OutboxStatsDto;
import io.stepprflow.monitor.outbox.OutboxMessage.OutboxStatus;
import io.stepprflow.monitor.outbox.OutboxMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * TDD tests for OutboxController.
 * Tests written BEFORE implementation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxController Tests")
class OutboxControllerTest {

    @Mock
    private OutboxMessageRepository outboxRepository;

    private OutboxController controller;

    @BeforeEach
    void setUp() {
        controller = new OutboxController(outboxRepository);
    }

    @Nested
    @DisplayName("GET /api/outbox/stats")
    class GetStats {

        @Test
        @DisplayName("Should return outbox statistics")
        void shouldReturnOutboxStats() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(500L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(2L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPending()).isEqualTo(10L);
            assertThat(response.getBody().getSent()).isEqualTo(500L);
            assertThat(response.getBody().getFailed()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should return total count")
        void shouldReturnTotalCount() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(500L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(2L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then
            assertThat(response.getBody().getTotal()).isEqualTo(512L);
        }

        @Test
        @DisplayName("Should return health status UP when no failed")
        void shouldReturnHealthUpWhenNoFailed() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(5L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then
            assertThat(response.getBody().getHealth()).isEqualTo("UP");
        }

        @Test
        @DisplayName("Should return health status DOWN when has failed")
        void shouldReturnHealthDownWhenHasFailed() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(5L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(3L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then
            assertThat(response.getBody().getHealth()).isEqualTo("DOWN");
        }

        @Test
        @DisplayName("Should return health status WARNING when pending is high")
        void shouldReturnHealthWarningWhenPendingHigh() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(500L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(100L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then
            assertThat(response.getBody().getHealth()).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("Should calculate send rate")
        void shouldCalculateSendRate() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(10L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(90L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then - 90 sent out of 100 total = 90%
            assertThat(response.getBody().getSendRate()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("Should return zero stats when repository is empty")
        void shouldReturnZeroStatsWhenEmpty() {
            // Given
            when(outboxRepository.countByStatus(OutboxStatus.PENDING)).thenReturn(0L);
            when(outboxRepository.countByStatus(OutboxStatus.SENT)).thenReturn(0L);
            when(outboxRepository.countByStatus(OutboxStatus.FAILED)).thenReturn(0L);

            // When
            ResponseEntity<OutboxStatsDto> response = controller.getStats();

            // Then
            assertThat(response.getBody().getTotal()).isZero();
            assertThat(response.getBody().getSendRate()).isZero();
            assertThat(response.getBody().getHealth()).isEqualTo("UP");
        }
    }
}
