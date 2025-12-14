package io.stepprflow.sample.service;

import io.stepprflow.sample.exception.PaymentDeclinedException;
import io.stepprflow.sample.model.OrderPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock payment service for demonstration.
 * In a real application, this would integrate with a payment gateway (Stripe, PayPal, etc.).
 */
@Slf4j
@Service
public class PaymentService {

    /**
     * Charge a payment method.
     *
     * @param paymentInfo the payment information
     * @param amount      the amount to charge
     * @return the transaction ID
     */
    public String charge(OrderPayload.PaymentInfo paymentInfo, BigDecimal amount) {
        log.info("Processing payment of {} for card ending in {}",
            amount, paymentInfo.getCardLast4());

        // Simulate payment processing time
        simulateDelay(500);

        // Simulate occasional failures (10% chance)
        if (Math.random() < 0.1) {
            throw new PaymentDeclinedException("Insufficient funds", amount);
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Payment successful. Transaction ID: {}", transactionId);

        return transactionId;
    }

    /**
     * Refund a transaction.
     *
     * @param transactionId the transaction ID to refund
     * @param amount        the amount to refund
     * @return the refund ID
     */
    public String refund(String transactionId, BigDecimal amount) {
        log.info("Processing refund of {} for transaction {}", amount, transactionId);

        simulateDelay(300);

        String refundId = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Refund successful. Refund ID: {}", refundId);

        return refundId;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
