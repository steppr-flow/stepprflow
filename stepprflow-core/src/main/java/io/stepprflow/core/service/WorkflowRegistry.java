package io.stepprflow.core.service;

import io.stepprflow.core.annotation.OnFailure;
import io.stepprflow.core.annotation.OnSuccess;
import io.stepprflow.core.annotation.Step;
import io.stepprflow.core.annotation.Timeout;
import io.stepprflow.core.annotation.Topic;
import io.stepprflow.core.model.StepDefinition;
import io.stepprflow.core.model.WorkflowDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for workflow definitions.
 *
 * <p>Scans for @Topic annotated beans on startup using @PostConstruct.
 * This ensures workflows are registered before RabbitMQ listeners start.
 */
@Component("workflowRegistry")
@RequiredArgsConstructor
@Slf4j
public class WorkflowRegistry {

    /** The Spring application context. */
    private final ApplicationContext applicationContext;

    /** Map of topic names to workflow definitions. */
    private final Map<String, WorkflowDefinition> definitions = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("Scanning for workflow definitions...");

        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Topic.class);

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();

            if (!(bean instanceof StepprFlow workflow)) {
                log.warn("Bean {} has @Topic but doesn't implement StepprFlow",
                         entry.getKey());
                continue;
            }

            Class<?> beanClass = bean.getClass();
            // Handle proxies
            if (beanClass.getName().contains("$$")) {
                beanClass = beanClass.getSuperclass();
            }

            Topic topic = beanClass.getAnnotation(Topic.class);
            if (topic == null) {
                continue;
            }

            WorkflowDefinition definition = buildDefinition(topic, workflow, beanClass);
            definitions.put(topic.value(), definition);

            log.info("Registered workflow: topic={}, steps={}",
                     topic.value(), definition.getTotalSteps());
        }

        log.info("Found {} workflow definitions", definitions.size());
    }

    @SuppressWarnings("unchecked")
    private WorkflowDefinition buildDefinition(
            final Topic topic,
            final StepprFlow handler,
            final Class<?> beanClass) {
        List<StepDefinition> steps = new ArrayList<>();
        Method onSuccessMethod = null;
        Method onFailureMethod = null;

        for (Method method : beanClass.getDeclaredMethods()) {
            Step stepAnnotation = method.getAnnotation(Step.class);
            if (stepAnnotation != null) {
                Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);
                Duration timeout = timeoutAnnotation != null
                        ? Duration.of(timeoutAnnotation.value(),
                                      timeoutAnnotation.unit().toChronoUnit())
                        : null;

                steps.add(StepDefinition.builder()
                        .id(stepAnnotation.id())
                        .label(stepAnnotation.label())
                        .description(stepAnnotation.description())
                        .method(method)
                        .skippable(stepAnnotation.skippable())
                        .continueOnFailure(stepAnnotation.continueOnFailure())
                        .timeout(timeout)
                        .build());
            }

            if (method.isAnnotationPresent(OnSuccess.class)) {
                onSuccessMethod = method;
            }

            if (method.isAnnotationPresent(OnFailure.class)) {
                onFailureMethod = method;
            }
        }

        // Sort steps by ID
        steps.sort(Comparator.comparingInt(StepDefinition::getId));

        // Workflow-level timeout
        Timeout workflowTimeout = beanClass.getAnnotation(Timeout.class);
        Duration timeout = workflowTimeout != null
                ? Duration.of(workflowTimeout.value(),
                             workflowTimeout.unit().toChronoUnit())
                : null;

        return WorkflowDefinition.builder()
                .topic(topic.value())
                .description(topic.description())
                .handler(handler)
                .handlerClass((Class<? extends StepprFlow>) beanClass)
                .steps(steps)
                .onSuccessMethod(onSuccessMethod)
                .onFailureMethod(onFailureMethod)
                .timeout(timeout)
                .partitions(topic.partitions())
                .replication(topic.replication())
                .build();
    }

    /**
     * Get workflow definition by topic.
     *
     * @param topic the topic name
     * @return the workflow definition or null if not found
     */
    public WorkflowDefinition getDefinition(final String topic) {
        return definitions.get(topic);
    }

    /**
     * Get all registered topics.
     *
     * @return list of topic names
     */
    public List<String> getTopics() {
        return new ArrayList<>(definitions.keySet());
    }

    /**
     * Get all workflow definitions.
     *
     * @return list of workflow definitions
     */
    public List<WorkflowDefinition> getAllDefinitions() {
        return new ArrayList<>(definitions.values());
    }
}
