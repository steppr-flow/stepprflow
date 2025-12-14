package io.stepprflow.core.integration;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test application for integration tests.
 * Required because async-workflow-core is a library without a main application class.
 */
@SpringBootApplication(scanBasePackages = "io.stepprflow.core")
public class TestApplication {
}