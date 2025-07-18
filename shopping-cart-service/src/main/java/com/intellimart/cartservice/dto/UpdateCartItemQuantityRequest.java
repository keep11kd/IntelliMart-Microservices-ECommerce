package com.intellimart.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemQuantityRequest {
    @NotNull(message = "Quantity cannot be null")
    @Min(value = 0, message = "Quantity must be at least 0. Use 0 to remove the item.")
    private Integer quantity;
}