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
public class OrderItemResponseForProductService {
    private Long id; // ID of the order item itself
    private Long productId;
    private String productName; // Include product name for clarity
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private String imageUrl; // Include image URL for clarity
}