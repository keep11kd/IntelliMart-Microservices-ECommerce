// Example: OrderResponse.java
package com.intellimart.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Make sure this is imported
import java.util.List;

import com.intellimart.orderservice.model.OrderStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;        // Matches Order.userId
    private String orderNumber;
    private OrderStatus status; // Assuming OrderStatus enum is visible here
    private BigDecimal totalAmount;
    private String paymentInfo;
    private String shippingAddress;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime createdAt; // New field to reflect order creation time
    private LocalDateTime updatedAt; // New field to reflect last update time

    // You can remove orderDate if it was present
    // private LocalDateTime orderDate; // <--- REMOVE THIS if it's there
}