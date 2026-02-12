package io.github.stepprflow.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application demonstrating Steppr Flow workflow orchestration.
 *
 * <p>Run with different profiles to use different message brokers:</p>
 * <ul>
 *     <li>{@code --spring.profiles.active=kafka} - Use Apache Kafka</li>
 *     <li>{@code --spring.profiles.active=rabbitmq} - Use RabbitMQ</li>
 * </ul>
 */
@SpringBootApplication
public final class SampleApplication {

    /**
     * Private constructor to prevent instantiation.
     */
    private SampleApplication() {
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
