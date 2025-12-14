package io.stepprflow.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for health status response.
 * Used by the UI to display component health.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDto {

    /**
     * Overall health status: UP, DOWN, or DEGRADED.
     */
    private String status;

    /**
     * Individual component health statuses.
     */
    private Map<String, ComponentHealth> components;

    /**
     * Individual component health.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        /**
         * Component status: UP or DOWN.
         */
        private String status;

        /**
         * Additional details about the component.
         */
        private Map<String, Object> details;
    }
}
