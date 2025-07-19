package com.intellimart.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDecrementRequest {
    @NotNull(message = "Product ID cannot be null")
    private Long productId; // Changed to Long
    @NotNull(message = "Quantity to decrement cannot be null")
    @Min(value = 1, message = "Quantity to decrement must be at least 1")
    private Integer quantity;
}