package io.stepprflow.core.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an async workflow handler.
 * The topic name is used as the Kafka topic for this workflow.
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
     * Kafka topic name for this workflow.
     * Should be unique across the application.
     *
     * @return the topic name
     */
    String value();

    /**
     * Optional description for documentation.
     *
     * @return the topic description
     */
    String description() default "";

    /**
     * Number of partitions for auto-created topic.
     *
     * @return the number of partitions
     */
    int partitions() default 1;

    /**
     * Replication factor for auto-created topic.
     *
     * @return the replication factor
     */
    short replication() default 1;
}
