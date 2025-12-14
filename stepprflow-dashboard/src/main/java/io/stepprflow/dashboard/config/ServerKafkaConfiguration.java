package io.stepprflow.dashboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stepprflow.core.StepprFlowProperties;
import io.stepprflow.core.model.WorkflowMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the monitoring dashboard.
 *
 * This configuration sets up Kafka consumers specifically for monitoring purposes.
 * The dashboard uses a separate consumer group from the workflow processors to ensure
 * it receives all messages independently.
 */
@Configuration
@EnableConfigurationProperties(StepprFlowProperties.class)
public class ServerKafkaConfiguration {

    /**
     * Consumer factory for the monitoring dashboard.
     * Uses a dedicated consumer group for monitoring.
     */
    @Bean
    @Primary
    public ConsumerFactory<String, WorkflowMessage> monitoringConsumerFactory(
            StepprFlowProperties properties, ObjectMapper objectMapper) {

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafka().getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Use a dedicated group ID for monitoring
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "stepprflow-monitoring");
        // Low latency settings for real-time monitoring
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);        // Don't wait for batch
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 100);    // Poll every 100ms max

        JsonDeserializer<WorkflowMessage> deserializer = new JsonDeserializer<>(WorkflowMessage.class, objectMapper);
        deserializer.addTrustedPackages("io.stepprflow.core.model", "io.stepprflow.monitor.model");
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    /**
     * Kafka listener container factory for the monitoring dashboard.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "workflowKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, WorkflowMessage> workflowKafkaListenerContainerFactory(
            ConsumerFactory<String, WorkflowMessage> monitoringConsumerFactory,
            StepprFlowProperties properties) {

        ConcurrentKafkaListenerContainerFactory<String, WorkflowMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(monitoringConsumerFactory);
        factory.setConcurrency(properties.getKafka().getConsumer().getConcurrency());
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}
