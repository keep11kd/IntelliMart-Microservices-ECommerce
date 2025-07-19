package com.intellimart.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDecrementRequest {
    @NotBlank(message = "Product ID cannot be empty")
    private String productId;

    @NotNull(message = "Quantity to decrement cannot be null")
    @Min(value = 1, message = "Quantity to decrement must be at least 1")
    private Integer quantity;
}