package com.intellimart.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    @NotBlank(message = "Product ID cannot be empty")
    private String productId;

    @NotNull(message = "Quantity cannot be null")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotNull(message = "Price at purchase cannot be null")
    @Min(value = 0, message = "Price at purchase must be non-negative")
    private BigDecimal priceAtPurchase;

    // Added for direct order creation, consistent with OrderItem entity
    private String productName;
    private String imageUrl;
}