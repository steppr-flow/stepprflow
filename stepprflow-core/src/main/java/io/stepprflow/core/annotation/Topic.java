package io.stepprflow.core.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a StepprFlow workflow handler.
 * The value is used as the workflow identifier (topic name for Kafka, queue name for RabbitMQ).
 *
 * <p>Example usage:
 * <pre>
 * &#64;Service
 * &#64;Topic("order-processing")
 * public class OrderWorkflow implements StepprFlow { }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Topic {

    /**
     * Workflow identifier (used as topic name for Kafka, queue name for RabbitMQ).
     * Should be unique across the application.
     *
     * @return the workflow identifier
     */
    String value();

    /**
     * Optional description for documentation.
     *
     * @return the workflow description
     */
    String description() default "";

    /**
     * Number of partitions for auto-created topic (Kafka) or consumers (RabbitMQ).
     *
     * @return the number of partitions
     */
    int partitions() default 1;

    /**
     * Replication factor for auto-created topic (Kafka only).
     *
     * @return the replication factor
     */
    short replication() default 1;
}
