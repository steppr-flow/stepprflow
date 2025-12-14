package io.stepprflow.sample.controller;

import io.stepprflow.core.service.WorkflowStarter;
import io.stepprflow.sample.controller.dto.CreateOrderRequest;
import io.stepprflow.sample.controller.dto.OrderResponse;
import io.stepprflow.sample.model.OrderPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final WorkflowStarter workflowStarter;

    /**
     * Create a new order and start the order processing workflow.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received order request from customer: {}", request.getCustomerId());

        // Generate order ID
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Convert request to workflow payload
        OrderPayload payload = buildPayload(orderId, request);

        // Start the workflow
        String executionId = workflowStarter.start("order-workflow", payload);

        log.info("Order workflow started. Order ID: {}, Execution ID: {}", orderId, executionId);

        return ResponseEntity.accepted().body(
            OrderResponse.builder()
                .orderId(orderId)
                .executionId(executionId)
                .status("PROCESSING")
                .message("Order received and processing started")
                .build()
        );
    }

    /**
     * Resume a failed or paused workflow.
     */
    @PostMapping("/{executionId}/resume")
    public ResponseEntity<OrderResponse> resumeOrder(
            @PathVariable String executionId,
            @RequestParam(required = false) Integer fromStep) {

        log.info("Resuming order workflow: {} from step: {}", executionId, fromStep);

        workflowStarter.resume(executionId, fromStep);

        return ResponseEntity.accepted().body(
            OrderResponse.builder()
                .executionId(executionId)
                .status("RESUMING")
                .message("Workflow resume initiated")
                .build()
        );
    }

    /**
     * Cancel an order workflow.
     */
    @DeleteMapping("/{executionId}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String executionId) {
        log.info("Cancelling order workflow: {}", executionId);

        workflowStarter.cancel(executionId);

        return ResponseEntity.ok(
            OrderResponse.builder()
                .executionId(executionId)
                .status("CANCELLED")
                .message("Order cancellation initiated")
                .build()
        );
    }

    private OrderPayload buildPayload(String orderId, CreateOrderRequest request) {
        // Calculate total
        BigDecimal total = request.getItems().stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderPayload.builder()
            .orderId(orderId)
            .customerId(request.getCustomerId())
            .customerEmail(request.getCustomerEmail())
            .items(request.getItems().stream()
                .map(item -> OrderPayload.OrderItem.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build())
                .collect(Collectors.toList()))
            .totalAmount(total)
            .paymentInfo(OrderPayload.PaymentInfo.builder()
                .cardLast4(request.getPayment().getCardLast4())
                .cardType(request.getPayment().getCardType())
                .build())
            .shippingAddress(OrderPayload.ShippingAddress.builder()
                .street(request.getShipping().getStreet())
                .city(request.getShipping().getCity())
                .state(request.getShipping().getState())
                .zipCode(request.getShipping().getZipCode())
                .country(request.getShipping().getCountry())
                .build())
            .status("PENDING")
            .build();
    }
}
