package io.stepprflow.sample.workflow;

import io.stepprflow.core.model.ErrorInfo;
import io.stepprflow.core.model.WorkflowMessage;
import io.stepprflow.core.model.WorkflowStatus;
import io.stepprflow.sample.model.OrderPayload;
import io.stepprflow.sample.service.InventoryService;
import io.stepprflow.sample.service.NotificationService;
import io.stepprflow.sample.service.PaymentService;
import io.stepprflow.sample.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderWorkflow Tests")
class OrderWorkflowTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ShippingService shippingService;

    @Mock
    private NotificationService notificationService;

    private OrderWorkflow workflow;

    @BeforeEach
    void setUp() {
        workflow = new OrderWorkflow(inventoryService, paymentService, shippingService, notificationService);
    }

    @Nested
    @DisplayName("Step 1: Validate Order")
    class ValidateOrderTests {

        @Test
        @DisplayName("Should validate order with all required fields")
        void shouldValidateCompleteOrder() {
            // Given
            OrderPayload payload = createValidPayload();

            // When & Then - no exception
            workflow.validateOrder(payload);
        }

        @Test
        @DisplayName("Should reject order with no items")
        void shouldRejectOrderWithNoItems() {
            // Given
            OrderPayload payload = createValidPayload();
            payload.setItems(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> workflow.validateOrder(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("Should reject order with null items")
        void shouldRejectOrderWithNullItems() {
            // Given
            OrderPayload payload = createValidPayload();
            payload.setItems(null);

            // When & Then
            assertThatThrownBy(() -> workflow.validateOrder(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("Should reject order with zero amount")
        void shouldRejectOrderWithZeroAmount() {
            // Given
            OrderPayload payload = createValidPayload();
            payload.setTotalAmount(BigDecimal.ZERO);

            // When & Then
            assertThatThrownBy(() -> workflow.validateOrder(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Should reject order with negative amount")
        void shouldRejectOrderWithNegativeAmount() {
            // Given
            OrderPayload payload = createValidPayload();
            payload.setTotalAmount(new BigDecimal("-10.00"));

            // When & Then
            assertThatThrownBy(() -> workflow.validateOrder(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Should reject order without shipping address")
        void shouldRejectOrderWithoutShippingAddress() {
            // Given
            OrderPayload payload = createValidPayload();
            payload.setShippingAddress(null);

            // When & Then
            assertThatThrownBy(() -> workflow.validateOrder(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Shipping address");
        }
    }

    @Nested
    @DisplayName("Step 2: Reserve Inventory")
    class ReserveInventoryTests {

        @Test
        @DisplayName("Should reserve inventory for all items")
        void shouldReserveInventoryForAllItems() {
            // Given
            OrderPayload payload = createValidPayload();
            when(inventoryService.reserve(anyString(), anyInt())).thenReturn(true);

            // When
            workflow.reserveInventory(payload);

            // Then
            verify(inventoryService).reserve("PROD-001", 2);
        }

        @Test
        @DisplayName("Should fail when inventory is insufficient")
        void shouldFailWhenInventoryInsufficient() {
            // Given
            OrderPayload payload = createValidPayload();
            when(inventoryService.reserve(anyString(), anyInt())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> workflow.reserveInventory(payload))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient inventory");
        }
    }

    @Nested
    @DisplayName("Step 3: Process Payment")
    class ProcessPaymentTests {

        @Test
        @DisplayName("Should process payment and set transaction ID")
        void shouldProcessPaymentSuccessfully() {
            // Given
            OrderPayload payload = createValidPayload();
            when(paymentService.charge(any(), any())).thenReturn("TXN-12345");

            // When
            workflow.processPayment(payload);

            // Then
            verify(paymentService).charge(payload.getPaymentInfo(), payload.getTotalAmount());
            assert payload.getPaymentInfo().getTransactionId().equals("TXN-12345");
        }

        @Test
        @DisplayName("Should propagate payment failure")
        void shouldPropagatePaymentFailure() {
            // Given
            OrderPayload payload = createValidPayload();
            when(paymentService.charge(any(), any())).thenThrow(new RuntimeException("Payment declined"));

            // When & Then
            assertThatThrownBy(() -> workflow.processPayment(payload))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Payment declined");
        }
    }

    @Nested
    @DisplayName("Step 4: Create Shipment")
    class CreateShipmentTests {

        @Test
        @DisplayName("Should create shipment successfully")
        void shouldCreateShipmentSuccessfully() {
            // Given
            OrderPayload payload = createValidPayload();
            when(shippingService.createShipment(anyString(), any(), any())).thenReturn("TRACK-ABC123");

            // When
            workflow.createShipment(payload);

            // Then
            verify(shippingService).createShipment(
                    payload.getOrderId(),
                    payload.getItems(),
                    payload.getShippingAddress()
            );
        }
    }

    @Nested
    @DisplayName("Step 5: Send Confirmation")
    class SendConfirmationTests {

        @Test
        @DisplayName("Should send confirmation notification")
        void shouldSendConfirmationNotification() {
            // Given
            OrderPayload payload = createValidPayload();

            // When
            workflow.sendConfirmation(payload);

            // Then
            verify(notificationService).sendOrderConfirmation(
                    payload.getCustomerEmail(),
                    payload.getOrderId(),
                    payload.getTotalAmount()
            );
        }
    }

    @Nested
    @DisplayName("Workflow Callbacks")
    class CallbackTests {

        @Test
        @DisplayName("Should handle successful workflow completion")
        void shouldHandleSuccessCallback() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId(UUID.randomUUID().toString())
                    .topic("order-workflow")
                    .currentStep(5)
                    .totalSteps(5)
                    .status(WorkflowStatus.COMPLETED)
                    .build();

            // When & Then - should not throw
            workflow.onWorkflowComplete(message);
        }

        @Test
        @DisplayName("Should handle workflow failure")
        void shouldHandleFailureCallback() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId(UUID.randomUUID().toString())
                    .topic("order-workflow")
                    .currentStep(3)
                    .totalSteps(5)
                    .status(WorkflowStatus.FAILED)
                    .errorInfo(ErrorInfo.builder()
                            .message("Payment declined")
                            .exceptionType("RuntimeException")
                            .build())
                    .build();

            // When & Then - should not throw
            workflow.onWorkflowFailed(message);
        }

        @Test
        @DisplayName("Should handle failure callback without error info")
        void shouldHandleFailureWithoutErrorInfo() {
            // Given
            WorkflowMessage message = WorkflowMessage.builder()
                    .executionId(UUID.randomUUID().toString())
                    .topic("order-workflow")
                    .currentStep(2)
                    .totalSteps(5)
                    .status(WorkflowStatus.FAILED)
                    .build();

            // When & Then - should not throw
            workflow.onWorkflowFailed(message);
        }
    }

    private OrderPayload createValidPayload() {
        return OrderPayload.builder()
                .orderId("ORD-001")
                .customerId("CUST-001")
                .customerEmail("customer@example.com")
                .items(List.of(
                        OrderPayload.OrderItem.builder()
                                .productId("PROD-001")
                                .productName("Test Product")
                                .quantity(2)
                                .price(new BigDecimal("25.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("50.00"))
                .paymentInfo(OrderPayload.PaymentInfo.builder()
                        .cardLast4("4242")
                        .cardType("VISA")
                        .build())
                .shippingAddress(OrderPayload.ShippingAddress.builder()
                        .street("123 Main St")
                        .city("New York")
                        .state("NY")
                        .zipCode("10001")
                        .country("USA")
                        .build())
                .build();
    }
}