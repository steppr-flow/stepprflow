package io.stepprflow.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Gatling simulation for testing workflow execution throughput.
 * <p>
 * This simulation tests the performance of starting new workflows
 * and monitoring their execution.
 * <p>
 * Run with: mvn gatling:test -pl stepprflow-load-tests -Pload-test -Dgatling.simulationClass=io.stepprflow.loadtest.WorkflowThroughputSimulation
 * <p>
 * Prerequisites:
 * - StepprFlow sample application running on localhost:8080 with OrderWorkflow
 * - Kafka and MongoDB containers running
 */
public class WorkflowThroughputSimulation extends Simulation {

    // Configuration
    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8010");
    private static final int WORKFLOWS_PER_SECOND = Integer.parseInt(System.getProperty("workflowsPerSecond", "600"));
    private static final int DURATION_SECONDS = Integer.parseInt(System.getProperty("duration", "60"));

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/ThroughputTest");

    // Feeder for generating order payloads
    FeederBuilder<Object> orderFeeder = listFeeder(
            java.util.stream.IntStream.range(0, 10000)
                    .mapToObj(i -> Map.<String, Object>of(
                            "customerId", "CUST-" + (1000 + i % 100),
                            "customerEmail", "customer" + (i % 100) + "@test.com",
                            "productId", "PROD-" + (i % 50),
                            "quantity", (i % 5) + 1,
                            "price", 10.0 + (i % 100)
                    ))
                    .toList()
    ).random();

    // Scenario: Start workflows at constant rate
    ScenarioBuilder startWorkflowScenario = scenario("Start Workflows")
            .feed(orderFeeder)
            .exec(
                    http("Start Order Workflow")
                            .post("/api/orders")
                            .body(StringBody("""
                                {
                                    "customerId": "#{customerId}",
                                    "customerEmail": "#{customerEmail}",
                                    "items": [{
                                        "productId": "#{productId}",
                                        "productName": "Test Product",
                                        "quantity": #{quantity},
                                        "price": #{price}
                                    }],
                                    "payment": {
                                        "cardLast4": "4242",
                                        "cardType": "VISA"
                                    },
                                    "shipping": {
                                        "street": "123 Test St",
                                        "city": "Test City",
                                        "state": "TS",
                                        "zipCode": "12345",
                                        "country": "US"
                                    }
                                }
                                """))
                            .check(status().in(200, 201, 202))
            );

    // Scenario: Monitor workflow progress
    ScenarioBuilder monitorWorkflowsScenario = scenario("Monitor Workflows")
            .exec(
                    http("Get Recent Executions")
                            .get("/api/workflows/recent")
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(500))
            .exec(
                    http("Get Statistics")
                            .get("/api/workflows/stats")
                            .check(status().is(200))
                            .check(jsonPath("$.total").saveAs("totalExecutions"))
            )
            .pause(Duration.ofMillis(500))
            .exec(
                    http("Get Metrics Summary")
                            .get("/api/metrics/summary")
                            .check(status().is(200))
            );

    // Scenario: Stress test - burst of workflows
    ScenarioBuilder burstScenario = scenario("Burst Workflows")
            .feed(orderFeeder)
            .repeat(10).on(
                    exec(
                            http("Start Order (Burst)")
                                    .post("/api/orders")
                                    .body(StringBody("""
                                        {
                                            "customerId": "BURST-#{customerId}",
                                            "customerEmail": "#{customerEmail}",
                                            "items": [{
                                                "productId": "#{productId}",
                                                "productName": "Burst Product",
                                                "quantity": #{quantity},
                                                "price": #{price}
                                            }],
                                            "payment": {
                                                "cardLast4": "1234",
                                                "cardType": "MASTERCARD"
                                            },
                                            "shipping": {
                                                "street": "456 Burst Ave",
                                                "city": "Burst City",
                                                "state": "BC",
                                                "zipCode": "67890",
                                                "country": "US"
                                            }
                                        }
                                        """))
                                    .check(status().in(200, 201, 202))
                    )
                    .pause(Duration.ofMillis(50), Duration.ofMillis(100))
            );

    {
        setUp(
                // Constant rate workflow creation
                startWorkflowScenario.injectOpen(
                        constantUsersPerSec(WORKFLOWS_PER_SECOND).during(Duration.ofSeconds(DURATION_SECONDS))
                ),
                // Monitoring in parallel
                monitorWorkflowsScenario.injectOpen(
                        rampUsers(5).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(2).during(Duration.ofSeconds(DURATION_SECONDS - 10))
                ),
                // Occasional bursts
                burstScenario.injectOpen(
                        nothingFor(Duration.ofSeconds(20)),
                        atOnceUsers(5),
                        nothingFor(Duration.ofSeconds(30)),
                        atOnceUsers(10)
                )
        ).protocols(httpProtocol)
        .assertions(
                global().responseTime().max().lt(10000),
                global().responseTime().percentile3().lt(3000),
                global().successfulRequests().percent().gt(90.0),
                forAll().failedRequests().percent().lt(10.0)
        );
    }
}
