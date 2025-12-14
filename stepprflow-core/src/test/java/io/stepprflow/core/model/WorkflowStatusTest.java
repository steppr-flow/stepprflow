package io.stepprflow.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowStatus Tests")
class WorkflowStatusTest {

    @Test
    @DisplayName("Should have all expected status values")
    void shouldHaveAllExpectedStatusValues() {
        assertThat(WorkflowStatus.values()).containsExactlyInAnyOrder(
                WorkflowStatus.PENDING,
                WorkflowStatus.IN_PROGRESS,
                WorkflowStatus.COMPLETED,
                WorkflowStatus.FAILED,
                WorkflowStatus.CANCELLED,
                WorkflowStatus.PAUSED,
                WorkflowStatus.RETRY_PENDING,
                WorkflowStatus.TIMED_OUT,
                WorkflowStatus.SKIPPED,
                WorkflowStatus.PASSED
        );
    }

    @Test
    @DisplayName("PASSED status should exist for intermediate steps")
    void passedStatusShouldExist() {
        WorkflowStatus passed = WorkflowStatus.valueOf("PASSED");
        assertThat(passed).isEqualTo(WorkflowStatus.PASSED);
    }

    @Test
    @DisplayName("Should be able to convert status to string and back")
    void shouldConvertToStringAndBack() {
        for (WorkflowStatus status : WorkflowStatus.values()) {
            String name = status.name();
            WorkflowStatus parsed = WorkflowStatus.valueOf(name);
            assertThat(parsed).isEqualTo(status);
        }
    }
}