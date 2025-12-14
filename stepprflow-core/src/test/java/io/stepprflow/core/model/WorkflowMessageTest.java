package io.stepprflow.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowMessage Tests")
class WorkflowMessageTest {

    private WorkflowMessage baseMessage;

    @BeforeEach
    void setUp() {
        baseMessage = WorkflowMessage.builder()
                .executionId("exec-123")
                .correlationId("corr-456")
                .topic("test-topic")
                .currentStep(1)
                .totalSteps(3)
                .status(WorkflowStatus.IN_PROGRESS)
                .payload(Map.of("key", "value"))
                .payloadType(Map.class.getName())
                .securityContext("token-abc")
                .metadata(Map.of("user", "test-user"))
                .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
                .build();
    }

    @Nested
    @DisplayName("createNew() factory method")
    class CreateNewTests {

        @Test
        @DisplayName("Should create new message with generated IDs")
        void shouldCreateNewMessageWithGeneratedIds() {
            Object payload = Map.of("data", "test");

            WorkflowMessage message = WorkflowMessage.createNew("my-topic", payload);

            assertThat(message.getExecutionId()).isNotNull().isNotEmpty();
            assertThat(message.getCorrelationId()).isNotNull().isNotEmpty();
            assertThat(message.getTopic()).isEqualTo("my-topic");
            assertThat(message.getCurrentStep()).isEqualTo(1);
            assertThat(message.getStatus()).isEqualTo(WorkflowStatus.PENDING);
        }

        @Test
        @DisplayName("Should set payload type from payload class")
        void shouldSetPayloadTypeFromPayloadClass() {
            TestPayload payload = new TestPayload("test");

            WorkflowMessage message = WorkflowMessage.createNew("my-topic", payload);

            assertThat(message.getPayload()).isEqualTo(payload);
            assertThat(message.getPayloadType()).isEqualTo(TestPayload.class.getName());
        }

        @Test
        @DisplayName("Should generate unique IDs for each call")
        void shouldGenerateUniqueIds() {
            WorkflowMessage message1 = WorkflowMessage.createNew("topic", "payload1");
            WorkflowMessage message2 = WorkflowMessage.createNew("topic", "payload2");

            assertThat(message1.getExecutionId()).isNotEqualTo(message2.getExecutionId());
            assertThat(message1.getCorrelationId()).isNotEqualTo(message2.getCorrelationId());
        }
    }

    @Nested
    @DisplayName("nextStep() method")
    class NextStepTests {

        @Test
        @DisplayName("Should increment current step")
        void shouldIncrementCurrentStep() {
            WorkflowMessage nextMessage = baseMessage.nextStep();

            assertThat(nextMessage.getCurrentStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should preserve execution and correlation IDs")
        void shouldPreserveIds() {
            WorkflowMessage nextMessage = baseMessage.nextStep();

            assertThat(nextMessage.getExecutionId()).isEqualTo("exec-123");
            assertThat(nextMessage.getCorrelationId()).isEqualTo("corr-456");
        }

        @Test
        @DisplayName("Should set status to IN_PROGRESS")
        void shouldSetStatusToInProgress() {
            WorkflowMessage nextMessage = baseMessage.nextStep();

            assertThat(nextMessage.getStatus()).isEqualTo(WorkflowStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should preserve payload and metadata")
        void shouldPreservePayloadAndMetadata() {
            WorkflowMessage nextMessage = baseMessage.nextStep();

            assertThat(nextMessage.getPayload()).isEqualTo(baseMessage.getPayload());
            assertThat(nextMessage.getPayloadType()).isEqualTo(baseMessage.getPayloadType());
            assertThat(nextMessage.getMetadata()).isEqualTo(baseMessage.getMetadata());
            assertThat(nextMessage.getSecurityContext()).isEqualTo(baseMessage.getSecurityContext());
        }

        @Test
        @DisplayName("Should preserve original createdAt and set new updatedAt")
        void shouldPreserveCreatedAtAndSetUpdatedAt() {
            WorkflowMessage nextMessage = baseMessage.nextStep();

            assertThat(nextMessage.getCreatedAt()).isEqualTo(baseMessage.getCreatedAt());
            assertThat(nextMessage.getUpdatedAt()).isNotNull();
            assertThat(nextMessage.getUpdatedAt()).isAfterOrEqualTo(baseMessage.getCreatedAt());
        }

        @Test
        @DisplayName("Should preserve total steps")
        void shouldPreserveTotalSteps() {
            WorkflowMessage nextMessage = baseMessage.nextStep();

            assertThat(nextMessage.getTotalSteps()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("fail() method")
    class FailTests {

        @Test
        @DisplayName("Should set status to FAILED")
        void shouldSetStatusToFailed() {
            WorkflowMessage failedMessage = baseMessage.fail("Something went wrong", "ERR_001");

            assertThat(failedMessage.getStatus()).isEqualTo(WorkflowStatus.FAILED);
        }

        @Test
        @DisplayName("Should create ErrorInfo with message and code")
        void shouldCreateErrorInfo() {
            WorkflowMessage failedMessage = baseMessage.fail("Something went wrong", "ERR_001");

            assertThat(failedMessage.getErrorInfo()).isNotNull();
            assertThat(failedMessage.getErrorInfo().getMessage()).isEqualTo("Something went wrong");
            assertThat(failedMessage.getErrorInfo().getCode()).isEqualTo("ERR_001");
            assertThat(failedMessage.getErrorInfo().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("Should preserve all other fields")
        void shouldPreserveOtherFields() {
            WorkflowMessage failedMessage = baseMessage.fail("Error", "ERR");

            assertThat(failedMessage.getExecutionId()).isEqualTo(baseMessage.getExecutionId());
            assertThat(failedMessage.getCorrelationId()).isEqualTo(baseMessage.getCorrelationId());
            assertThat(failedMessage.getTopic()).isEqualTo(baseMessage.getTopic());
            assertThat(failedMessage.getCurrentStep()).isEqualTo(baseMessage.getCurrentStep());
            assertThat(failedMessage.getPayload()).isEqualTo(baseMessage.getPayload());
        }

        @Test
        @DisplayName("Should preserve retry info if present")
        void shouldPreserveRetryInfo() {
            RetryInfo retryInfo = RetryInfo.builder()
                    .attempt(2)
                    .maxAttempts(3)
                    .build();
            baseMessage = baseMessage.toBuilder()
                    .retryInfo(retryInfo)
                    .build();

            WorkflowMessage failedMessage = baseMessage.fail("Error", "ERR");

            assertThat(failedMessage.getRetryInfo()).isEqualTo(retryInfo);
        }
    }

    @Nested
    @DisplayName("complete() method")
    class CompleteTests {

        @Test
        @DisplayName("Should set status to COMPLETED")
        void shouldSetStatusToCompleted() {
            WorkflowMessage completedMessage = baseMessage.complete();

            assertThat(completedMessage.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should preserve all fields")
        void shouldPreserveAllFields() {
            WorkflowMessage completedMessage = baseMessage.complete();

            assertThat(completedMessage.getExecutionId()).isEqualTo(baseMessage.getExecutionId());
            assertThat(completedMessage.getCorrelationId()).isEqualTo(baseMessage.getCorrelationId());
            assertThat(completedMessage.getTopic()).isEqualTo(baseMessage.getTopic());
            assertThat(completedMessage.getCurrentStep()).isEqualTo(baseMessage.getCurrentStep());
            assertThat(completedMessage.getTotalSteps()).isEqualTo(baseMessage.getTotalSteps());
            assertThat(completedMessage.getPayload()).isEqualTo(baseMessage.getPayload());
            assertThat(completedMessage.getMetadata()).isEqualTo(baseMessage.getMetadata());
        }

        @Test
        @DisplayName("Should set updatedAt timestamp")
        void shouldSetUpdatedAtTimestamp() {
            WorkflowMessage completedMessage = baseMessage.complete();

            assertThat(completedMessage.getUpdatedAt()).isNotNull();
            assertThat(completedMessage.getUpdatedAt()).isAfterOrEqualTo(baseMessage.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Builder default values")
    class BuilderDefaultsTests {

        @Test
        @DisplayName("Should set default createdAt to now")
        void shouldSetDefaultCreatedAt() {
            Instant before = Instant.now();

            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId("test")
                    .build();

            Instant after = Instant.now();

            assertThat(message.getCreatedAt()).isNotNull();
            assertThat(message.getCreatedAt()).isBetween(before, after.plusMillis(1));
        }
    }

    // Test payload record
    record TestPayload(String data) {
    }
}