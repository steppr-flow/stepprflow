package io.stepprflow.sample.workflow;

import io.stepprflow.core.annotation.OnFailure;
import io.stepprflow.core.annotation.OnSuccess;
import io.stepprflow.core.annotation.Step;
import io.stepprflow.core.annotation.Timeout;
import io.stepprflow.core.annotation.Topic;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.service.StepprFlow;
import io.stepprflow.sample.exception.InsufficientInventoryException;
import io.stepprflow.sample.model.OrderPayload;
import io.stepprflow.sample.service.InventoryService;
import io.stepprflow.sample.service.NotificationService;
import io.stepprflow.sample.service.PaymentService;
import io.stepprflow.sample.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Order processing workflow demonstrating Steppr Flow capabilities with RabbitMQ.
 * <p>
 * This workflow processes an order through 5 steps:
 * 1. Validate order data
 * 2. Reserve inventory
 * 3. Process payment
 * 4. Create shipment
 * 5. Send confirmation
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Topic(value = "order-workflow", description = "Processes customer orders via RabbitMQ")
public class OrderWorkflow implements StepprFlow {

    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final NotificationService notificationService;

    @Step(id = 1, label = "Validate Order", description = "Validates order data and business rules")
    public void validateOrder(OrderPayload payload) {
        log.info("[Step 1] Validating order: {}", payload.getOrderId());

        if (payload.getItems() == null || payload.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        if (payload.getTotalAmount() == null || payload.getTotalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }

        if (payload.getShippingAddress() == null) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        log.info("[Step 1] Order {} validated successfully", payload.getOrderId());
    }

    @Step(id = 2, label = "Reserve Inventory", description = "Reserves products in inventory")
    public void reserveInventory(OrderPayload payload) {
        log.info("[Step 2] Reserving inventory for order: {}", payload.getOrderId());

        for (OrderPayload.OrderItem item : payload.getItems()) {
            boolean reserved = inventoryService.reserve(item.getProductId(), item.getQuantity());

            if (!reserved) {
                throw new InsufficientInventoryException(item.getProductId(), item.getQuantity());
            }

            log.info("[Step 2] Reserved {} units of product {}", item.getQuantity(), item.getProductId());
        }

        log.info("[Step 2] Inventory reserved for order {}", payload.getOrderId());
    }

    @Step(id = 3, label = "Process Payment", description = "Charges customer payment method")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    public void processPayment(OrderPayload payload) {
        log.info("[Step 3] Processing payment for order: {} (amount: {})",
            payload.getOrderId(), payload.getTotalAmount());

        String transactionId = paymentService.charge(
            payload.getPaymentInfo(),
            payload.getTotalAmount()
        );

        // Update payload with transaction ID
        payload.getPaymentInfo().setTransactionId(transactionId);

        log.info("[Step 3] Payment processed for order {} (transaction: {})",
            payload.getOrderId(), transactionId);
    }

    @Step(id = 4, label = "Create Shipment", description = "Creates shipping label and schedules pickup")
    public void createShipment(OrderPayload payload) {
        log.info("[Step 4] Creating shipment for order: {}", payload.getOrderId());

        String trackingNumber = shippingService.createShipment(
            payload.getOrderId(),
            payload.getItems(),
            payload.getShippingAddress()
        );

        log.info("[Step 4] Shipment created for order {} (tracking: {})",
            payload.getOrderId(), trackingNumber);
    }

    @Step(id = 5, label = "Send Confirmation", description = "Sends order confirmation to customer")
    public void sendConfirmation(OrderPayload payload) {
        log.info("[Step 5] Sending confirmation for order: {}", payload.getOrderId());

        notificationService.sendOrderConfirmation(
            payload.getCustomerEmail(),
            payload.getOrderId(),
            payload.getTotalAmount()
        );

        log.info("[Step 5] Confirmation sent for order {}", payload.getOrderId());
    }

    @OnSuccess
    public void onWorkflowComplete(WorkflowMessage message) {
        log.info("=== ORDER WORKFLOW COMPLETED (RabbitMQ) ===");
        log.info("Execution ID: {}", message.getExecutionId());
        log.info("Total steps: {}", message.getTotalSteps());
        log.info("============================================");
    }

    @OnFailure
    public void onWorkflowFailed(WorkflowMessage message) {
        log.error("=== ORDER WORKFLOW FAILED (RabbitMQ) ===");
        log.error("Execution ID: {}", message.getExecutionId());
        log.error("Failed at step: {}", message.getCurrentStep());

        if (message.getErrorInfo() != null) {
            log.error("Error: {}", message.getErrorInfo().getMessage());
        }

        log.error("=========================================");

        // Here you could implement compensation logic:
        // - Release reserved inventory
        // - Refund payment if charged
        // - Cancel shipment if created
    }
}
