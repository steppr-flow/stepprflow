package io.stepprflow.monitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for StepprFlow Monitor REST API.
 */
@Configuration
public class OpenApiConfig {

    @Value("${stepprflow.version:1.0.0}")
    private String version;

    @Bean
    public OpenAPI stepprflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StepprFlow Monitor API")
                        .description("""
                                REST API for monitoring and managing StepprFlow workflows.

                                ## Features
                                - **Workflow Monitoring**: Track workflow executions and history
                                - **Metrics**: Access workflow performance metrics and statistics
                                - **Circuit Breaker**: Monitor and manage circuit breaker states
                                - **Execution Control**: Resume failed workflows, cancel running executions

                                ## Authentication
                                No authentication required by default. Configure Spring Security if needed.
                                """)
                        .version(version)
                        .contact(new Contact()
                                .name("StepprFlow Team")
                                .url("https://github.com/stepprflow/stepprflow"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Default Server")))
                .tags(List.of(
                        new Tag()
                                .name("Workflows")
                                .description("Workflow execution monitoring and control"),
                        new Tag()
                                .name("Metrics")
                                .description("Workflow performance metrics"),
                        new Tag()
                                .name("Circuit Breaker")
                                .description("Circuit breaker monitoring and management"),
                        new Tag()
                                .name("Registry")
                                .description("Workflow registration from microservices")));
    }
}
