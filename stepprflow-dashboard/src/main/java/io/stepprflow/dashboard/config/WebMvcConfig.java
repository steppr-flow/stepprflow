package io.stepprflow.dashboard.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for serving the dashboard UI.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    private final UiProperties properties;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect /dashboard to /index.html
        registry.addViewController(properties.getBasePath())
                .setViewName("forward:/index.html");

        registry.addViewController(properties.getBasePath() + "/")
                .setViewName("forward:/index.html");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        UiProperties.Cors corsConfig = properties.getCors();
        String[] allowedOrigins = corsConfig.getAllowedOrigins();

        // Only configure CORS if explicit origins are specified
        if (allowedOrigins != null && allowedOrigins.length > 0) {
            log.info("Configuring CORS for origins: {}", (Object) allowedOrigins);
            registry.addMapping("/api/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods(corsConfig.getAllowedMethods())
                    .allowedHeaders(corsConfig.getAllowedHeaders())
                    .allowCredentials(corsConfig.isAllowCredentials())
                    .maxAge(corsConfig.getMaxAge());
        } else {
            // By default, no CORS is enabled (same-origin only)
            // This is more secure than allowing all origins
            log.info("CORS is not configured - same-origin policy applies. " +
                    "Set stepprflow.ui.cors.allowed-origins to enable cross-origin requests.");
        }
    }
}
