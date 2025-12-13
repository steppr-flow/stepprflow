package io.stepprflow.sample.service;

import io.stepprflow.sample.model.OrderPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Mock shipping service for demonstration.
 * In a real application, this would integrate with shipping carriers (FedEx, UPS, etc.).
 */
@Slf4j
@Service
public class ShippingService {

    /**
     * Create a shipment for an order.
     *
     * @param orderId         the order ID
     * @param items           the items to ship
     * @param shippingAddress the destination address
     * @return the tracking number
     */
    public String createShipment(String orderId,
                                  List<OrderPayload.OrderItem> items,
                                  OrderPayload.ShippingAddress shippingAddress) {

        log.info("Creating shipment for order {} to {}, {}",
            orderId, shippingAddress.getCity(), shippingAddress.getCountry());

        // Simulate shipment creation time
        simulateDelay(300);

        int totalItems = items.stream().mapToInt(OrderPayload.OrderItem::getQuantity).sum();

        String trackingNumber = "TRACK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        log.info("Shipment created. Tracking: {} ({} items)", trackingNumber, totalItems);

        return trackingNumber;
    }

    /**
     * Cancel a shipment.
     *
     * @param trackingNumber the tracking number to cancel
     */
    public void cancelShipment(String trackingNumber) {
        log.info("Cancelling shipment: {}", trackingNumber);
        simulateDelay(200);
        log.info("Shipment {} cancelled", trackingNumber);
    }

    /**
     * Get shipment status.
     *
     * @param trackingNumber the tracking number
     * @return the shipment status
     */
    public String getStatus(String trackingNumber) {
        return "IN_TRANSIT";
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
