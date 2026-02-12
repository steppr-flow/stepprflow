package io.github.stepprflow.core.registration;

import io.github.stepprflow.core.broker.MessageBroker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegistrationAutoConfiguration Tests")
class RegistrationAutoConfigurationTest {

    @Test
    @DisplayName("Should have @AutoConfiguration annotation")
    void shouldHaveAutoConfigurationAnnotation() {
        assertThat(RegistrationAutoConfiguration.class
                .isAnnotationPresent(AutoConfiguration.class)).isTrue();
    }

    @Test
    @DisplayName("Should have @ConditionalOnBean for MessageBroker")
    void shouldHaveConditionalOnBeanAnnotation() {
        ConditionalOnBean annotation = RegistrationAutoConfiguration.class
                .getAnnotation(ConditionalOnBean.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(MessageBroker.class);
    }

    @Test
    @DisplayName("Should enable RegistrationProperties configuration")
    void shouldEnableConfigurationProperties() {
        EnableConfigurationProperties annotation = RegistrationAutoConfiguration.class
                .getAnnotation(EnableConfigurationProperties.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(RegistrationProperties.class);
    }

    @Test
    @DisplayName("Should enable scheduling")
    void shouldEnableScheduling() {
        assertThat(RegistrationAutoConfiguration.class
                .isAnnotationPresent(EnableScheduling.class)).isTrue();
    }
}
