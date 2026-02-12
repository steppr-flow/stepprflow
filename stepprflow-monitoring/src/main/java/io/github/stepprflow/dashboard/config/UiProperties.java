package io.github.stepprflow.dashboard.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the StepprFlow Dashboard UI.
 */
@ConfigurationProperties(prefix = "stepprflow.ui")
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UiProperties {

    /**
     * Enable UI module.
     */
    private boolean enabled = true;

    /**
     * Base path for the dashboard.
     */
    private String basePath = "/dashboard";

    /**
     * Title of the dashboard.
     */
    private String title = "StepprFlow Dashboard";

    /**
     * Refresh interval in seconds.
     */
    private int refreshInterval = 5;

    /**
     * Enable dark mode by default.
     */
    private boolean darkMode = false;

    /**
     * CORS configuration.
     */
    private Cors cors = new Cors();

    /**
     * CORS configuration for the dashboard.
     */
    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    public static class Cors {
        /**
         * Allowed origins for CORS.
         * Default: empty (same-origin only).
         * Set to specific origins like {@code "http://localhost:3000"} for development
         * or {@code "https://yourdomain.com"} for production.
         */
        @Getter(lombok.AccessLevel.NONE)
        @Setter(lombok.AccessLevel.NONE)
        private String[] allowedOrigins = {};

        /**
         * Allowed HTTP methods for CORS.
         */
        @Getter(lombok.AccessLevel.NONE)
        @Setter(lombok.AccessLevel.NONE)
        private String[] allowedMethods = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};

        /**
         * Allowed headers for CORS.
         */
        @Getter(lombok.AccessLevel.NONE)
        @Setter(lombok.AccessLevel.NONE)
        private String[] allowedHeaders = {"*"};

        /**
         * Whether to allow credentials (cookies, authorization headers).
         */
        private boolean allowCredentials = false;

        /**
         * Max age of the CORS preflight cache in seconds.
         */
        private long maxAge = 3600;

        /**
         * Returns a defensive copy of allowed origins.
         */
        public String[] getAllowedOrigins() {
            return allowedOrigins == null ? new String[0] : allowedOrigins.clone();
        }

        /**
         * Sets allowed origins with a defensive copy.
         */
        public void setAllowedOrigins(String[] allowedOrigins) {
            this.allowedOrigins = allowedOrigins == null ? null : allowedOrigins.clone();
        }

        /**
         * Returns a defensive copy of allowed methods.
         */
        public String[] getAllowedMethods() {
            return allowedMethods == null ? new String[0] : allowedMethods.clone();
        }

        /**
         * Sets allowed methods with a defensive copy.
         */
        public void setAllowedMethods(String[] allowedMethods) {
            this.allowedMethods = allowedMethods == null ? null : allowedMethods.clone();
        }

        /**
         * Returns a defensive copy of allowed headers.
         */
        public String[] getAllowedHeaders() {
            return allowedHeaders == null ? new String[0] : allowedHeaders.clone();
        }

        /**
         * Sets allowed headers with a defensive copy.
         */
        public void setAllowedHeaders(String[] allowedHeaders) {
            this.allowedHeaders = allowedHeaders == null ? null : allowedHeaders.clone();
        }
    }
}
