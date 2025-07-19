package com.intellimart.orderservice.dto;

import com.intellimart.orderservice.model.OrderStatus; // Corrected import
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String userId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String paymentInfo;
    private String shippingAddress;
    private List<OrderItemResponse> orderItems;
}