package io.stepprflow.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Gatling simulation for StepprFlow REST API load testing.
 *
 * Run with: mvn gatling:test -pl stepprflow-load-tests -Pload-test
 *
 * Prerequisites:
 * - StepprFlow sample application running on localhost:8080
 * - Kafka and MongoDB containers running
 */
public class WorkflowApiSimulation extends Simulation {

    // Configuration - can be overridden via system properties
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8010");
    private static final int USERS = Integer.parseInt(System.getProperty("users", "100"));
    private static final int RAMP_UP_SECONDS = Integer.parseInt(System.getProperty("rampUp", "30"));

    // HTTP Protocol configuration
    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/LoadTest");

    // Feeder for generating random data
    FeederBuilder<Object> executionIdFeeder = listFeeder(
            java.util.stream.IntStream.range(0, 1000)
                    .mapToObj(i -> java.util.Map.<String, Object>of("executionId", UUID.randomUUID().toString()))
                    .toList()
    ).random();

    // Scenario: Dashboard Overview
    ScenarioBuilder dashboardOverviewScenario = scenario("Dashboard Overview")
            .exec(
                    http("Get Dashboard Config")
                            .get("/api/dashboard/config")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(500))
            .exec(
                    http("Get Dashboard Overview")
                            .get("/api/dashboard/overview")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(500))
            .exec(
                    http("Get Workflows")
                            .get("/api/dashboard/workflows")
                            .check(status().is(200))
            );

    // Scenario: List Executions with Pagination
    ScenarioBuilder listExecutionsScenario = scenario("List Executions")
            .exec(
                    http("List Executions - Page 0")
                            .get("/api/dashboard/executions?page=0&size=20")
                            .check(status().is(200))
                            .check(jsonPath("$.totalElements").saveAs("totalElements"))
            )
            .pause(Duration.ofMillis(300))
            .exec(
                    http("List Executions - Page 1")
                            .get("/api/dashboard/executions?page=1&size=20")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(300))
            .exec(
                    http("List Executions - Filtered by Status")
                            .get("/api/dashboard/executions?statuses=IN_PROGRESS,FAILED&page=0&size=50")
                            .check(status().is(200))
            );

    // Scenario: Metrics Dashboard
    ScenarioBuilder metricsScenario = scenario("Metrics Dashboard")
            .exec(
                    http("Get Metrics Dashboard")
                            .get("/api/metrics")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(500))
            .exec(
                    http("Get Global Summary")
                            .get("/api/metrics/summary")
                            .check(status().is(200))
            );

    // Scenario: Circuit Breaker Monitoring
    ScenarioBuilder circuitBreakerScenario = scenario("Circuit Breaker Monitoring")
            .exec(
                    http("Get Circuit Breaker Config")
                            .get("/api/circuit-breakers/config")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(300))
            .exec(
                    http("List Circuit Breakers")
                            .get("/api/circuit-breakers")
                            .check(status().is(200))
            );

    // Scenario: Workflow API
    ScenarioBuilder workflowApiScenario = scenario("Workflow API")
            .exec(
                    http("List Workflows")
                            .get("/api/workflows?page=0&size=10")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(300))
            .exec(
                    http("Get Recent Executions")
                            .get("/api/workflows/recent")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(300))
            .exec(
                    http("Get Statistics")
                            .get("/api/workflows/stats")
                            .check(status().is(200))
            );

    // Scenario: Mixed workload (realistic user behavior)
    ScenarioBuilder mixedWorkloadScenario = scenario("Mixed Workload")
            .exec(
                    http("Get Dashboard Config")
                            .get("/api/dashboard/config")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1), Duration.ofSeconds(3))
            .exec(
                    http("Get Dashboard Overview")
                            .get("/api/dashboard/overview")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(2), Duration.ofSeconds(5))
            .repeat(3).on(
                    exec(
                            http("List Executions")
                                    .get("/api/dashboard/executions?page=#{pageNum}&size=20")
                                    .check(status().is(200))
                    )
                    .pause(Duration.ofSeconds(1), Duration.ofSeconds(2))
            )
            .exec(
                    http("Get Metrics")
                            .get("/api/metrics")
                            .check(status().is(200))
            )
            .pause(Duration.ofSeconds(1), Duration.ofSeconds(3))
            .exec(
                    http("Get Circuit Breakers")
                            .get("/api/circuit-breakers")
                            .check(status().is(200))
            );

    // Scenario: High frequency polling (simulates real-time dashboard)
    ScenarioBuilder pollingScenario = scenario("Real-time Polling")
            .during(Duration.ofSeconds(60)).on(
                    exec(
                            http("Poll Executions")
                                    .get("/api/dashboard/executions?page=0&size=10")
                                    .check(status().is(200))
                    )
                    .pause(Duration.ofSeconds(2))
                    .exec(
                            http("Poll Metrics")
                                    .get("/api/metrics/summary")
                                    .check(status().is(200))
                    )
                    .pause(Duration.ofSeconds(2))
            );

    {
        setUp(
                // Dashboard Overview: 30% of users
                dashboardOverviewScenario.injectOpen(
                        rampUsers((int) (USERS * 0.3)).during(Duration.ofSeconds(RAMP_UP_SECONDS))
                ),
                // List Executions: 25% of users
                listExecutionsScenario.injectOpen(
                        rampUsers((int) (USERS * 0.25)).during(Duration.ofSeconds(RAMP_UP_SECONDS))
                ),
                // Metrics: 15% of users
                metricsScenario.injectOpen(
                        rampUsers((int) (USERS * 0.15)).during(Duration.ofSeconds(RAMP_UP_SECONDS))
                ),
                // Circuit Breaker: 10% of users
                circuitBreakerScenario.injectOpen(
                        rampUsers((int) (USERS * 0.1)).during(Duration.ofSeconds(RAMP_UP_SECONDS))
                ),
                // Workflow API: 10% of users
                workflowApiScenario.injectOpen(
                        rampUsers((int) (USERS * 0.1)).during(Duration.ofSeconds(RAMP_UP_SECONDS))
                ),
                // Polling: 10% of users
                pollingScenario.injectOpen(
                        rampUsers((int) (USERS * 0.1)).during(Duration.ofSeconds(RAMP_UP_SECONDS))
                )
        ).protocols(httpProtocol)
        .assertions(
                global().responseTime().max().lt(5000),
                global().responseTime().percentile3().lt(2000),
                global().successfulRequests().percent().gt(95.0)
        );
    }
}
