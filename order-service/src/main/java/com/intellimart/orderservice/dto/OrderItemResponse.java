package com.intellimart.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private String productId;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private String productName; // Added
    private String imageUrl;    // Added
}