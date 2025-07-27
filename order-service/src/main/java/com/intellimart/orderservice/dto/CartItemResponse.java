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
public class CartItemResponse {
	private Long id;
    private Long productId; // THIS MUST BE LONG
    private String productName; // Assuming product name is String
    private Integer quantity;
    private BigDecimal price;
    private String imageUrl; // Assuming image URL is String
    private Long cartId; // Assuming cartId is Long
}