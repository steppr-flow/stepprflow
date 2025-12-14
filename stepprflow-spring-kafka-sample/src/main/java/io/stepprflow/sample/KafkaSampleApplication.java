package io.stepprflow.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public final class KafkaSampleApplication {

    /**
     * Private constructor to prevent instantiation.
     */
    private KafkaSampleApplication() {
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(KafkaSampleApplication.class, args);
    }
}
