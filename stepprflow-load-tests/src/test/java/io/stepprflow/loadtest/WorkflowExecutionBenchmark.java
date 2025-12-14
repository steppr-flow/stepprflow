package io.stepprflow.loadtest;

import org.junit.jupiter.api.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit-based performance benchmark for StepprFlow.
 *
 * This test measures:
 * - Workflow start throughput
 * - API response times
 * - Concurrent request handling
 *
 * Run with: mvn test -pl stepprflow-load-tests -Dtest=WorkflowExecutionBenchmark
 *
 * Prerequisites:
 * - StepprFlow sample application running on localhost:8080
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowExecutionBenchmark {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8010");
    private static final RestTemplate restTemplate = new RestTemplate();

    @Nested
    @DisplayName("API Response Time Benchmarks")
    class ApiResponseTimeBenchmarks {

        @Test
        @Order(1)
        @DisplayName("Benchmark: Dashboard Overview Response Time")
        void benchmarkDashboardOverview() {
            int iterations = 100;
            List<Long> responseTimes = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                ResponseEntity<String> response = restTemplate.getForEntity(
                        BASE_URL + "/api/dashboard/overview", String.class);
                long end = System.nanoTime();

                Assertions.assertEquals(200, response.getStatusCode().value());
                responseTimes.add(TimeUnit.NANOSECONDS.toMillis(end - start));
            }

            printStatistics("Dashboard Overview", responseTimes);
        }

        @Test
        @Order(2)
        @DisplayName("Benchmark: List Executions Response Time")
        void benchmarkListExecutions() {
            int iterations = 100;
            List<Long> responseTimes = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                ResponseEntity<String> response = restTemplate.getForEntity(
                        BASE_URL + "/api/dashboard/executions?page=0&size=20", String.class);
                long end = System.nanoTime();

                Assertions.assertEquals(200, response.getStatusCode().value());
                responseTimes.add(TimeUnit.NANOSECONDS.toMillis(end - start));
            }

            printStatistics("List Executions", responseTimes);
        }

        @Test
        @Order(3)
        @DisplayName("Benchmark: Metrics Dashboard Response Time")
        void benchmarkMetricsDashboard() {
            int iterations = 100;
            List<Long> responseTimes = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                ResponseEntity<String> response = restTemplate.getForEntity(
                        BASE_URL + "/api/metrics", String.class);
                long end = System.nanoTime();

                Assertions.assertEquals(200, response.getStatusCode().value());
                responseTimes.add(TimeUnit.NANOSECONDS.toMillis(end - start));
            }

            printStatistics("Metrics Dashboard", responseTimes);
        }
    }

    @Nested
    @DisplayName("Throughput Benchmarks")
    class ThroughputBenchmarks {

        @Test
        @Order(10)
        @DisplayName("Benchmark: Concurrent API Requests")
        void benchmarkConcurrentRequests() throws InterruptedException {
            int threads = 20;
            int requestsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            AtomicLong totalResponseTime = new AtomicLong(0);

            Instant start = Instant.now();

            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < requestsPerThread; i++) {
                            try {
                                long reqStart = System.nanoTime();
                                ResponseEntity<String> response = restTemplate.getForEntity(
                                        BASE_URL + "/api/dashboard/executions?page=0&size=10", String.class);
                                long reqEnd = System.nanoTime();

                                if (response.getStatusCode().is2xxSuccessful()) {
                                    successCount.incrementAndGet();
                                    totalResponseTime.addAndGet(TimeUnit.NANOSECONDS.toMillis(reqEnd - reqStart));
                                } else {
                                    errorCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                errorCount.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(120, TimeUnit.SECONDS);
            executor.shutdown();

            Instant end = Instant.now();
            long durationMs = Duration.between(start, end).toMillis();
            int totalRequests = threads * requestsPerThread;

            System.out.println("\n=== Concurrent Requests Benchmark ===");
            System.out.println("Threads: " + threads);
            System.out.println("Requests per thread: " + requestsPerThread);
            System.out.println("Total requests: " + totalRequests);
            System.out.println("Successful: " + successCount.get());
            System.out.println("Errors: " + errorCount.get());
            System.out.println("Duration: " + durationMs + " ms");
            System.out.println("Throughput: " + String.format("%.2f", (totalRequests * 1000.0) / durationMs) + " req/s");
            System.out.println("Avg response time: " + (successCount.get() > 0 ? totalResponseTime.get() / successCount.get() : 0) + " ms");

            Assertions.assertTrue(successCount.get() > totalRequests * 0.95, "Success rate should be > 95%");
        }

        @Test
        @Order(11)
        @DisplayName("Benchmark: Workflow Start Throughput")
        void benchmarkWorkflowStartThroughput() throws InterruptedException {
            int workflows = 100;
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(workflows);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Instant start = Instant.now();

            for (int i = 0; i < workflows; i++) {
                final int orderId = i;
                executor.submit(() -> {
                    try {
                        String payload = String.format("""
                                {
                                    "customerId": "CUST-%d",
                                    "customerEmail": "bench%d@test.com",
                                    "items": [{
                                        "productId": "PROD-%d",
                                        "productName": "Benchmark Product",
                                        "quantity": %d,
                                        "price": %.2f
                                    }],
                                    "payment": {
                                        "cardLast4": "4242",
                                        "cardType": "VISA"
                                    },
                                    "shipping": {
                                        "street": "123 Bench St",
                                        "city": "Bench City",
                                        "state": "BC",
                                        "zipCode": "12345",
                                        "country": "US"
                                    }
                                }
                                """, orderId % 100, orderId, orderId % 50, orderId % 5 + 1, 10.0 + orderId % 100);

                        HttpEntity<String> request = new HttpEntity<>(payload, headers);
                        ResponseEntity<String> response = restTemplate.postForEntity(
                                BASE_URL + "/api/orders", request, String.class);

                        if (response.getStatusCode().is2xxSuccessful()) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();

            Instant end = Instant.now();
            long durationMs = Duration.between(start, end).toMillis();

            System.out.println("\n=== Workflow Start Throughput ===");
            System.out.println("Workflows started: " + workflows);
            System.out.println("Successful: " + successCount.get());
            System.out.println("Errors: " + errorCount.get());
            System.out.println("Duration: " + durationMs + " ms");
            System.out.println("Throughput: " + String.format("%.2f", (workflows * 1000.0) / durationMs) + " workflows/s");

            Assertions.assertTrue(successCount.get() > workflows * 0.90, "Success rate should be > 90%");
        }
    }

    @Nested
    @DisplayName("Stress Tests")
    class StressTests {

        @Test
        @Order(20)
        @DisplayName("Stress Test: Sustained Load")
        void stressTestSustainedLoad() throws InterruptedException {
            int durationSeconds = 30;
            int requestsPerSecond = 50;
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(requestsPerSecond);
            ExecutorService executor = Executors.newFixedThreadPool(requestsPerSecond);

            Instant start = Instant.now();
            AtomicInteger requestsSent = new AtomicInteger(0);

            ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
                for (int i = 0; i < requestsPerSecond; i++) {
                    executor.submit(() -> {
                        try {
                            long reqStart = System.nanoTime();
                            ResponseEntity<String> response = restTemplate.getForEntity(
                                    BASE_URL + "/api/dashboard/executions?page=0&size=10", String.class);
                            long reqEnd = System.nanoTime();

                            requestsSent.incrementAndGet();
                            if (response.getStatusCode().is2xxSuccessful()) {
                                successCount.incrementAndGet();
                                responseTimes.add(TimeUnit.NANOSECONDS.toMillis(reqEnd - reqStart));
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    });
                }
            }, 0, 1, TimeUnit.SECONDS);

            Thread.sleep(durationSeconds * 1000L);
            task.cancel(false);
            scheduler.shutdown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            Instant end = Instant.now();
            long durationMs = Duration.between(start, end).toMillis();

            System.out.println("\n=== Sustained Load Stress Test ===");
            System.out.println("Duration: " + durationSeconds + " seconds");
            System.out.println("Target rate: " + requestsPerSecond + " req/s");
            System.out.println("Total requests: " + requestsSent.get());
            System.out.println("Successful: " + successCount.get());
            System.out.println("Errors: " + errorCount.get());
            System.out.println("Actual throughput: " + String.format("%.2f", (requestsSent.get() * 1000.0) / durationMs) + " req/s");

            if (!responseTimes.isEmpty()) {
                printStatistics("Response Times", new ArrayList<>(responseTimes));
            }

            double successRate = (successCount.get() * 100.0) / requestsSent.get();
            Assertions.assertTrue(successRate > 90.0, "Success rate should be > 90%");
        }
    }

    private void printStatistics(String name, List<Long> values) {
        if (values.isEmpty()) {
            System.out.println("\n=== " + name + " ===");
            System.out.println("No data collected");
            return;
        }

        Collections.sort(values);
        long min = values.get(0);
        long max = values.get(values.size() - 1);
        double avg = values.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = values.get((int) (values.size() * 0.50));
        long p95 = values.get((int) (values.size() * 0.95));
        long p99 = values.get((int) (values.size() * 0.99));

        System.out.println("\n=== " + name + " ===");
        System.out.println("Iterations: " + values.size());
        System.out.println("Min: " + min + " ms");
        System.out.println("Max: " + max + " ms");
        System.out.println("Avg: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50: " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms");
        System.out.println("P99: " + p99 + " ms");
    }
}
