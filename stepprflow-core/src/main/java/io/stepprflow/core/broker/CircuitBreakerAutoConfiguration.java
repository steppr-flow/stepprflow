package io.stepprflow.core.broker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.stepprflow.core.StepprFlowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Auto-configuration for Circuit Breaker integration with MessageBroker.
 *
 * <p>This configuration wraps any existing MessageBroker
 * with a ResilientMessageBroker that provides circuit breaker protection.
 */
@Configuration
@ConditionalOnClass(CircuitBreakerRegistry.class)
@Slf4j
public class CircuitBreakerAutoConfiguration {

    /**
     * Default circuit breaker names to pre-register at startup.
     */
    private static final List<String> DEFAULT_CIRCUIT_BREAKERS = List.of(
            "broker-kafka",
            "broker-rabbitmq",
            "workflow-execution"
    );

    /**
     * Creates the default CircuitBreakerRegistry bean.
     *
     * @param properties the steppr-flow properties
     * @return the circuit breaker registry
     */
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry(
            final StepprFlowProperties properties) {
        log.info("Creating default CircuitBreakerRegistry");

        StepprFlowProperties.CircuitBreaker cbProps = properties.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(cbProps.getFailureRateThreshold())
                .slowCallRateThreshold(cbProps.getSlowCallRateThreshold())
                .slowCallDurationThreshold(cbProps.getSlowCallDurationThreshold())
                .slidingWindowSize(cbProps.getSlidingWindowSize())
                .minimumNumberOfCalls(cbProps.getMinimumNumberOfCalls())
                .permittedNumberOfCallsInHalfOpenState(
                        cbProps.getPermittedNumberOfCallsInHalfOpenState())
                .waitDurationInOpenState(cbProps.getWaitDurationInOpenState())
                .automaticTransitionFromOpenToHalfOpenEnabled(
                        cbProps.isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // Pre-register default circuit breakers
        for (String name : DEFAULT_CIRCUIT_BREAKERS) {
            CircuitBreaker cb = registry.circuitBreaker(name);
            log.info("Pre-registered circuit breaker: {} (state: {})",
                     name, cb.getState());
        }

        return registry;
    }

    /**
     * Creates circuit breaker metrics for Micrometer.
     *
     * @param registry the circuit breaker registry
     * @param meterRegistry the meter registry
     * @return the tagged circuit breaker metrics
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public TaggedCircuitBreakerMetrics circuitBreakerMetrics(
            final CircuitBreakerRegistry registry,
            final MeterRegistry meterRegistry) {
        log.info("Registering circuit breaker metrics with Micrometer");
        TaggedCircuitBreakerMetrics metrics =
                TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
        metrics.bindTo(meterRegistry);
        return metrics;
    }

    /**
     * Configuration for wrapping the primary broker with circuit breaker.
     */
    @Configuration
    @ConditionalOnProperty(
            prefix = "stepprflow.circuit-breaker",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    static class ResilientBrokerConfiguration {

        /**
         * Creates a resilient message broker wrapper.
         *
         * @param delegate the delegate broker
         * @param properties the steppr-flow properties
         * @param registry the circuit breaker registry
         * @return the resilient message broker
         */
        @Bean
        @Primary
        @ConditionalOnBean(MessageBroker.class)
        public ResilientMessageBroker resilientMessageBroker(
                final MessageBroker delegate,
                final StepprFlowProperties properties,
                final CircuitBreakerRegistry registry) {
            log.info("Wrapping MessageBroker '{}' with circuit breaker protection",
                    delegate.getBrokerType());
            return new ResilientMessageBroker(
                    delegate,
                    properties.getCircuitBreaker(),
                    registry);
        }
    }
}
