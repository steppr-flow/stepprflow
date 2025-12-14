package io.stepprflow.core.service;

import io.stepprflow.core.exception.WorkflowException;
import io.stepprflow.core.broker.MessageBroker;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowStarterImpl Tests")
class WorkflowStarterImplTest {

    @Mock
    private WorkflowRegistry registry;

    @Mock
    private MessageBroker messageBroker;

    @InjectMocks
    private WorkflowStarterImpl workflowStarter;

    @Captor
    private ArgumentCaptor<WorkflowMessage> messageCaptor;

    private WorkflowDefinition testDefinition;

    @BeforeEach
    void setUp() {
        testDefinition = WorkflowDefinition.builder()
                .topic("test-topic")
                .description("Test workflow")
                .steps(List.of())
                .partitions(1)
                .replication((short) 1)
                .build();
    }

    @Nested
    @DisplayName("start() method")
    class StartTests {

        @Test
        @DisplayName("Should throw exception when topic is unknown")
        void shouldThrowExceptionWhenTopicUnknown() {
            when(registry.getDefinition("unknown-topic")).thenReturn(null);

            assertThatThrownBy(() -> workflowStarter.start("unknown-topic", new TestPayload("")))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining("Unknown workflow topic: unknown-topic");
        }

        @Test
        @DisplayName("Should create and send workflow message")
        void shouldCreateAndSendWorkflowMessage() {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            String executionId = workflowStarter.start("test-topic", new TestPayload("test"));

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage message = messageCaptor.getValue();

            assertThat(executionId).isNotNull();
            assertThat(message.getExecutionId()).isEqualTo(executionId);
            assertThat(message.getTopic()).isEqualTo("test-topic");
            assertThat(message.getCurrentStep()).isEqualTo(1);
            assertThat(message.getStatus()).isEqualTo(WorkflowStatus.PENDING);
        }

        @Test
        @DisplayName("Should generate unique execution ID")
        void shouldGenerateUniqueExecutionId() {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            String executionId1 = workflowStarter.start("test-topic", new TestPayload("test1"));
            String executionId2 = workflowStarter.start("test-topic", new TestPayload("test2"));

            assertThat(executionId1).isNotEqualTo(executionId2);
        }

        @Test
        @DisplayName("Should include payload and payload type")
        void shouldIncludePayloadAndPayloadType() {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);
            TestPayload payload = new TestPayload("data");

            workflowStarter.start("test-topic", payload);

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage message = messageCaptor.getValue();

            assertThat(message.getPayload()).isEqualTo(payload);
            assertThat(message.getPayloadType()).isEqualTo(TestPayload.class.getName());
        }

        @Test
        @DisplayName("Should set correct total steps from definition")
        void shouldSetCorrectTotalSteps() {
            // Definition with 3 steps
            testDefinition = testDefinition.toBuilder()
                    .steps(List.of(
                            StepDefinition.builder().id(1).build(),
                            StepDefinition.builder().id(2).build(),
                            StepDefinition.builder().id(3).build()
                    ))
                    .build();
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            workflowStarter.start("test-topic", new TestPayload("test"));

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage message = messageCaptor.getValue();

            assertThat(message.getTotalSteps()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("start() with metadata")
    class StartWithMetadataTests {

        @Test
        @DisplayName("Should include metadata in message")
        void shouldIncludeMetadataInMessage() {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);
            Map<String, Object> metadata = Map.of("userId", "user-123", "source", "api");

            workflowStarter.start("test-topic", new TestPayload("test"), metadata);

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage message = messageCaptor.getValue();

            assertThat(message.getMetadata()).isNotNull();
            assertThat(message.getMetadata()).containsEntry("userId", "user-123");
            assertThat(message.getMetadata()).containsEntry("source", "api");
        }

        @Test
        @DisplayName("Should handle null metadata")
        void shouldHandleNullMetadata() {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            workflowStarter.start("test-topic", new TestPayload("test"), null);

            verify(messageBroker).send(eq("test-topic"), messageCaptor.capture());
            WorkflowMessage message = messageCaptor.getValue();

            assertThat(message.getMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("startAsync() method")
    class StartAsyncTests {

        @Test
        @DisplayName("Should return CompletableFuture with execution ID")
        void shouldReturnCompletableFutureWithExecutionId() throws ExecutionException, InterruptedException, TimeoutException {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            CompletableFuture<String> future = workflowStarter.startAsync("test-topic", new TestPayload("test"));
            String executionId = future.get(5, TimeUnit.SECONDS);

            assertThat(executionId).isNotNull();
            verify(messageBroker).send(eq("test-topic"), any(WorkflowMessage.class));
        }

        @Test
        @DisplayName("Should complete exceptionally for unknown topic")
        void shouldCompleteExceptionallyForUnknownTopic() {
            when(registry.getDefinition("unknown-topic")).thenReturn(null);

            CompletableFuture<String> future = workflowStarter.startAsync("unknown-topic", new TestPayload("test"));

            assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(WorkflowException.class);
        }
    }

    @Nested
    @DisplayName("startAndGetMessage() method")
    class StartAndGetMessageTests {

        @Test
        @DisplayName("Should return WorkflowMessage with correct data")
        void shouldReturnWorkflowMessageWithCorrectData() {
            when(registry.getDefinition("test-topic")).thenReturn(testDefinition);

            WorkflowMessage message = workflowStarter.startAndGetMessage("test-topic", new TestPayload("test"));

            assertThat(message).isNotNull();
            assertThat(message.getExecutionId()).isNotNull();
            assertThat(message.getCorrelationId()).isNotNull();
            assertThat(message.getTopic()).isEqualTo("test-topic");
            assertThat(message.getStatus()).isEqualTo(WorkflowStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw exception for unknown topic")
        void shouldThrowExceptionForUnknownTopic() {
            when(registry.getDefinition("unknown-topic")).thenReturn(null);

            assertThatThrownBy(() -> workflowStarter.startAndGetMessage("unknown-topic", new TestPayload("test")))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining("Unknown workflow topic");
        }
    }

    @Nested
    @DisplayName("Unsupported operations")
    class UnsupportedOperationsTests {

        @Test
        @DisplayName("resume() should throw UnsupportedOperationException")
        void resumeShouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> workflowStarter.resume("exec-123", 1))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("async-workflow-monitor");
        }

        @Test
        @DisplayName("cancel() should throw UnsupportedOperationException")
        void cancelShouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> workflowStarter.cancel("exec-123"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("async-workflow-monitor");
        }
    }

    // Test payload class
    record TestPayload(String data) {
    }
}