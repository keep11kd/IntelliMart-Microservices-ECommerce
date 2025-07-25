package com.intellimart.orderservice.dto;

import com.intellimart.orderservice.model.OrderStatus; // <--- NEW IMPORT
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // <--- NEW IMPORT

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent {
    private Long orderId;
    private Long userId;
    private String orderNumber;
    private OrderStatus status; // <--- ADDED: Current status of the order
    private BigDecimal totalAmount;
    private String shippingAddress; // <--- ADDED: Full shipping address
    private String paymentInfo;   // <--- ADDED: General payment info string
    private String razorpayOrderId; // <--- ADDED: Razorpay's order ID
    private String razorpayPaymentId; // <--- ADDED: Razorpay's payment ID (if available)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // <--- ADDED: Last updated timestamp

    private List<OrderItemEvent> items; // <--- ADDED: Detailed list of items in the order

    // Optional: Add customer contact details if a separate user service isn't always queried
    // private String customerEmail;
    // private String customerPhone;
}