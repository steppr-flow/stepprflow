package io.stepprflow.sample.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock inventory service for demonstration.
 * In a real application, this would connect to an inventory management system.
 */
@Slf4j
@Service
public class InventoryService {

    // Simulated inventory stock
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>(Map.of(
        "PROD-001", 100,
        "PROD-002", 50,
        "PROD-003", 25,
        "PROD-004", 0  // Out of stock
    ));

    // Reserved quantities
    private final Map<String, Integer> reserved = new ConcurrentHashMap<>();

    /**
     * Reserve inventory for a product.
     *
     * @param productId the product ID
     * @param quantity  the quantity to reserve
     * @return true if reservation successful, false otherwise
     */
    public boolean reserve(String productId, int quantity) {
        log.debug("Attempting to reserve {} units of product {}", quantity, productId);

        // Simulate processing time
        simulateDelay(200);

        int available = inventory.getOrDefault(productId, 0);
        int alreadyReserved = reserved.getOrDefault(productId, 0);
        int actualAvailable = available - alreadyReserved;

        if (actualAvailable >= quantity) {
            reserved.merge(productId, quantity, Integer::sum);
            log.info("Reserved {} units of {} (available: {})", quantity, productId, actualAvailable - quantity);
            return true;
        }

        log.warn("Cannot reserve {} units of {} (only {} available)", quantity, productId, actualAvailable);
        return false;
    }

    /**
     * Release reserved inventory.
     *
     * @param productId the product ID
     * @param quantity  the quantity to release
     */
    public void release(String productId, int quantity) {
        log.info("Releasing {} units of product {}", quantity, productId);
        reserved.merge(productId, -quantity, (a, b) -> Math.max(0, a + b));
    }

    /**
     * Commit reserved inventory (after successful order).
     *
     * @param productId the product ID
     * @param quantity  the quantity to commit
     */
    public void commit(String productId, int quantity) {
        log.info("Committing {} units of product {}", quantity, productId);
        inventory.merge(productId, -quantity, Integer::sum);
        reserved.merge(productId, -quantity, (a, b) -> Math.max(0, a + b));
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
