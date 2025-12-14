package io.stepprflow.monitor.service;

import io.stepprflow.core.model.WorkflowRegistrationRequest;
import io.stepprflow.monitor.MonitorProperties;
import io.stepprflow.monitor.model.RegisteredWorkflow;
import io.stepprflow.monitor.repository.RegisteredWorkflowRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowRegistryService Tests")
class WorkflowRegistryServiceTest {

    @Mock
    private RegisteredWorkflowRepository repository;

    @Mock
    private MonitorProperties properties;

    @InjectMocks
    private WorkflowRegistryService registryService;

    @Captor
    private ArgumentCaptor<RegisteredWorkflow> workflowCaptor;

    @Nested
    @DisplayName("cleanupStaleInstances() method")
    class CleanupStaleInstancesTests {

        @BeforeEach
        void setUp() {
            MonitorProperties.Registry registry = new MonitorProperties.Registry();
            registry.setInstanceTimeout(Duration.ofMinutes(5));
            when(properties.getRegistry()).thenReturn(registry);
        }

        @Test
        @DisplayName("Should remove instances without heartbeat for longer than timeout and mark workflow as INACTIVE")
        void shouldRemoveStaleInstancesAndMarkInactive() {
            // Given: a workflow with one stale instance (no heartbeat for 10 minutes)
            RegisteredWorkflow.ServiceInstance staleInstance = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .host("host1")
                    .port(8080)
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(staleInstance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: the stale instance should be removed and workflow marked as INACTIVE
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow savedWorkflow = workflowCaptor.getValue();
            assertThat(savedWorkflow.getRegisteredBy()).isEmpty();
            assertThat(savedWorkflow.getStatus()).isEqualTo(RegisteredWorkflow.Status.INACTIVE);
        }

        @Test
        @DisplayName("Should keep instances with recent heartbeat")
        void shouldKeepHealthyInstances() {
            // Given: a workflow with one healthy instance (heartbeat 1 minute ago)
            RegisteredWorkflow.ServiceInstance healthyInstance = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .host("host1")
                    .port(8080)
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(1)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(healthyInstance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: no changes should be saved (instance is healthy)
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should remove only stale instances, keep healthy ones, and keep workflow ACTIVE")
        void shouldRemoveOnlyStaleInstancesAndKeepActive() {
            // Given: a workflow with one stale and one healthy instance
            RegisteredWorkflow.ServiceInstance staleInstance = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .host("host1")
                    .port(8080)
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            RegisteredWorkflow.ServiceInstance healthyInstance = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-2")
                    .host("host2")
                    .port(8080)
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(1)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(staleInstance);
            instances.add(healthyInstance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: only the stale instance should be removed, workflow stays ACTIVE
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow savedWorkflow = workflowCaptor.getValue();
            assertThat(savedWorkflow.getRegisteredBy()).hasSize(1);
            assertThat(savedWorkflow.getRegisteredBy().iterator().next().getInstanceId())
                    .isEqualTo("instance-2");
            assertThat(savedWorkflow.getStatus()).isEqualTo(RegisteredWorkflow.Status.ACTIVE);
        }

        @Test
        @DisplayName("Should mark workflow with null registeredBy as INACTIVE")
        void shouldMarkWorkflowWithNullRegisteredByAsInactive() {
            // Given: a workflow with null registeredBy that is ACTIVE
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .registeredBy(null)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: workflow should be marked as INACTIVE
            verify(repository).save(workflowCaptor.capture());
            assertThat(workflowCaptor.getValue().getStatus()).isEqualTo(RegisteredWorkflow.Status.INACTIVE);
        }

        @Test
        @DisplayName("Should mark workflow with empty registeredBy as INACTIVE")
        void shouldMarkWorkflowWithEmptyRegisteredByAsInactive() {
            // Given: a workflow with empty registeredBy that is ACTIVE
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .registeredBy(new HashSet<>())
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: workflow should be marked as INACTIVE
            verify(repository).save(workflowCaptor.capture());
            assertThat(workflowCaptor.getValue().getStatus()).isEqualTo(RegisteredWorkflow.Status.INACTIVE);
        }

        @Test
        @DisplayName("Should not save workflow already INACTIVE with no instances")
        void shouldNotSaveAlreadyInactiveWorkflow() {
            // Given: a workflow that is already INACTIVE with no instances
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .status(RegisteredWorkflow.Status.INACTIVE)
                    .registeredBy(new HashSet<>())
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: no save should happen (already inactive)
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle instance with null lastHeartbeat as stale")
        void shouldTreatNullHeartbeatAsStale() {
            // Given: a workflow with an instance that has null lastHeartbeat
            RegisteredWorkflow.ServiceInstance instanceWithNullHeartbeat = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .host("host1")
                    .port(8080)
                    .lastHeartbeat(null)
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instanceWithNullHeartbeat);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: the instance with null heartbeat should be removed
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow savedWorkflow = workflowCaptor.getValue();
            assertThat(savedWorkflow.getRegisteredBy()).isEmpty();
        }

        @Test
        @DisplayName("Should process multiple workflows")
        void shouldProcessMultipleWorkflows() {
            // Given: two workflows, each with one stale instance
            RegisteredWorkflow.ServiceInstance staleInstance1 = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            RegisteredWorkflow.ServiceInstance staleInstance2 = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("payment-service")
                    .instanceId("instance-2")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances1 = new HashSet<>();
            instances1.add(staleInstance1);

            Set<RegisteredWorkflow.ServiceInstance> instances2 = new HashSet<>();
            instances2.add(staleInstance2);

            RegisteredWorkflow workflow1 = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances1)
                    .build();

            RegisteredWorkflow workflow2 = RegisteredWorkflow.builder()
                    .id("wf-2")
                    .topic("payment.processed")
                    .registeredBy(instances2)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow1, workflow2));

            // When
            registryService.cleanupStaleInstances();

            // Then: both workflows should be updated
            verify(repository, times(2)).save(any(RegisteredWorkflow.class));
        }

        @Test
        @DisplayName("Should update updatedAt when removing stale instances")
        void shouldUpdateTimestampWhenRemovingStaleInstances() {
            // Given
            RegisteredWorkflow.ServiceInstance staleInstance = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(staleInstance);

            Instant oldUpdatedAt = Instant.now().minus(Duration.ofHours(1));
            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances)
                    .updatedAt(oldUpdatedAt)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            Instant before = Instant.now();

            // When
            registryService.cleanupStaleInstances();

            Instant after = Instant.now();

            // Then
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow savedWorkflow = workflowCaptor.getValue();
            assertThat(savedWorkflow.getUpdatedAt()).isBetween(before, after.plusMillis(1));
        }

        @Test
        @DisplayName("Should use configurable timeout from properties")
        void shouldUseConfigurableTimeout() {
            // Given: timeout set to 2 minutes
            MonitorProperties.Registry registry = new MonitorProperties.Registry();
            registry.setInstanceTimeout(Duration.ofMinutes(2));
            when(properties.getRegistry()).thenReturn(registry);

            // Instance with heartbeat 3 minutes ago (should be stale with 2 min timeout)
            RegisteredWorkflow.ServiceInstance instance = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(3)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instance);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: the instance should be removed
            verify(repository).save(workflowCaptor.capture());
            assertThat(workflowCaptor.getValue().getRegisteredBy()).isEmpty();
        }

        @Test
        @DisplayName("Should log number of removed instances")
        void shouldLogRemovedInstancesCount() {
            // Given: 2 stale instances
            RegisteredWorkflow.ServiceInstance stale1 = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            RegisteredWorkflow.ServiceInstance stale2 = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-2")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(10)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(stale1);
            instances.add(stale2);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            int removedCount = registryService.cleanupStaleInstances();

            // Then
            assertThat(removedCount).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("registerWorkflows() method")
    class RegisterWorkflowsTests {

        @Test
        @DisplayName("Should create separate workflows for same topic from different services")
        void shouldCreateSeparateWorkflowsForSameTopicFromDifferentServices() {
            // Given: Kafka sample registering order-workflow
            WorkflowRegistrationRequest kafkaRequest = WorkflowRegistrationRequest.builder()
                    .serviceName("stepprflow-kafka-sample")
                    .instanceId("kafka-instance-1")
                    .host("localhost")
                    .port(8010)
                    .workflows(List.of(
                            WorkflowRegistrationRequest.WorkflowInfo.builder()
                                    .topic("order-workflow")
                                    .description("Processes customer orders via Kafka")
                                    .steps(List.of(
                                            WorkflowRegistrationRequest.StepInfo.builder()
                                                    .id(1)
                                                    .label("Validate Order")
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            // First registration - no existing workflow
            when(repository.findByTopicAndServiceName("order-workflow", "stepprflow-kafka-sample"))
                    .thenReturn(java.util.Optional.empty());

            // When: Register Kafka workflow
            registryService.registerWorkflows(kafkaRequest);

            // Then: A new workflow should be created with serviceName
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow savedWorkflow = workflowCaptor.getValue();
            assertThat(savedWorkflow.getTopic()).isEqualTo("order-workflow");
            assertThat(savedWorkflow.getServiceName()).isEqualTo("stepprflow-kafka-sample");

            // Given: RabbitMQ sample registering the same topic
            WorkflowRegistrationRequest rabbitRequest = WorkflowRegistrationRequest.builder()
                    .serviceName("stepprflow-rabbitmq-sample")
                    .instanceId("rabbit-instance-1")
                    .host("localhost")
                    .port(8011)
                    .workflows(List.of(
                            WorkflowRegistrationRequest.WorkflowInfo.builder()
                                    .topic("order-workflow")
                                    .description("Processes customer orders via RabbitMQ")
                                    .steps(List.of(
                                            WorkflowRegistrationRequest.StepInfo.builder()
                                                    .id(1)
                                                    .label("Validate Order")
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            // No existing workflow for RabbitMQ
            when(repository.findByTopicAndServiceName("order-workflow", "stepprflow-rabbitmq-sample"))
                    .thenReturn(java.util.Optional.empty());

            // Reset the captor
            reset(repository);
            when(repository.findByTopicAndServiceName("order-workflow", "stepprflow-rabbitmq-sample"))
                    .thenReturn(java.util.Optional.empty());

            // When: Register RabbitMQ workflow
            registryService.registerWorkflows(rabbitRequest);

            // Then: A separate workflow should be created for RabbitMQ
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow rabbitWorkflow = workflowCaptor.getValue();
            assertThat(rabbitWorkflow.getTopic()).isEqualTo("order-workflow");
            assertThat(rabbitWorkflow.getServiceName()).isEqualTo("stepprflow-rabbitmq-sample");
        }

        @Test
        @DisplayName("Should update existing workflow when same service registers same topic")
        void shouldUpdateExistingWorkflowForSameService() {
            // Given: An existing workflow from Kafka sample
            RegisteredWorkflow existingWorkflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order-workflow")
                    .serviceName("stepprflow-kafka-sample")
                    .description("Old description")
                    .status(RegisteredWorkflow.Status.ACTIVE)
                    .registeredBy(new HashSet<>())
                    .build();

            when(repository.findByTopicAndServiceName("order-workflow", "stepprflow-kafka-sample"))
                    .thenReturn(java.util.Optional.of(existingWorkflow));

            WorkflowRegistrationRequest request = WorkflowRegistrationRequest.builder()
                    .serviceName("stepprflow-kafka-sample")
                    .instanceId("kafka-instance-2")
                    .host("localhost")
                    .port(8010)
                    .workflows(List.of(
                            WorkflowRegistrationRequest.WorkflowInfo.builder()
                                    .topic("order-workflow")
                                    .description("Updated description")
                                    .steps(List.of())
                                    .build()
                    ))
                    .build();

            // When
            registryService.registerWorkflows(request);

            // Then: The existing workflow should be updated, not duplicated
            verify(repository).save(workflowCaptor.capture());
            RegisteredWorkflow savedWorkflow = workflowCaptor.getValue();
            assertThat(savedWorkflow.getId()).isEqualTo("wf-1");
            assertThat(savedWorkflow.getDescription()).isEqualTo("Updated description");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @BeforeEach
        void setUp() {
            MonitorProperties.Registry registry = new MonitorProperties.Registry();
            registry.setInstanceTimeout(Duration.ofMinutes(5));
            when(properties.getRegistry()).thenReturn(registry);
        }

        @Test
        @DisplayName("Should handle empty workflow list")
        void shouldHandleEmptyWorkflowList() {
            when(repository.findAll()).thenReturn(List.of());

            int removedCount = registryService.cleanupStaleInstances();

            assertThat(removedCount).isZero();
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle exactly at timeout boundary")
        void shouldHandleExactTimeoutBoundary() {
            // Given: instance with heartbeat exactly at timeout (5 minutes)
            // Should be considered stale (>= timeout)
            RegisteredWorkflow.ServiceInstance instanceAtBoundary = RegisteredWorkflow.ServiceInstance.builder()
                    .serviceName("order-service")
                    .instanceId("instance-1")
                    .lastHeartbeat(Instant.now().minus(Duration.ofMinutes(5)))
                    .build();

            Set<RegisteredWorkflow.ServiceInstance> instances = new HashSet<>();
            instances.add(instanceAtBoundary);

            RegisteredWorkflow workflow = RegisteredWorkflow.builder()
                    .id("wf-1")
                    .topic("order.created")
                    .registeredBy(instances)
                    .build();

            when(repository.findAll()).thenReturn(List.of(workflow));

            // When
            registryService.cleanupStaleInstances();

            // Then: instance at boundary should be removed
            verify(repository).save(workflowCaptor.capture());
            assertThat(workflowCaptor.getValue().getRegisteredBy()).isEmpty();
        }
    }
}
