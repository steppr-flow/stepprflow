package io.stepprflow.sample.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Mock notification service for demonstration.
 * In a real application, this would send emails, SMS, push notifications, etc.
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * Send order confirmation to customer.
     *
     * @param email   the customer email
     * @param orderId the order ID
     * @param amount  the total amount
     */
    public void sendOrderConfirmation(String email, String orderId, BigDecimal amount) {
        log.info("Sending order confirmation to {}", email);

        // Simulate email sending
        simulateDelay(100);

        log.info("Email sent to {} for order {} (total: {})", email, orderId, amount);
    }

    /**
     * Send order failed notification to customer.
     *
     * @param email   the customer email
     * @param orderId the order ID
     * @param reason  the failure reason
     */
    public void sendOrderFailed(String email, String orderId, String reason) {
        log.info("Sending order failure notification to {}", email);

        simulateDelay(100);

        log.info("Failure notification sent to {} for order {}: {}", email, orderId, reason);
    }

    /**
     * Send shipping notification to customer.
     *
     * @param email          the customer email
     * @param orderId        the order ID
     * @param trackingNumber the tracking number
     */
    public void sendShippingNotification(String email, String orderId, String trackingNumber) {
        log.info("Sending shipping notification to {}", email);

        simulateDelay(100);

        log.info("Shipping notification sent to {} for order {} (tracking: {})",
            email, orderId, trackingNumber);
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
