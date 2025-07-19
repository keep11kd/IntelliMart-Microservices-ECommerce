package com.intellimart.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    @NotBlank(message = "User ID cannot be empty")
    private String userId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid // This ensures validation is applied to each item in the list
    private List<OrderItemRequest> orderItems;

    // Optional fields for direct order creation, consistent with Order entity
    private String paymentInfo;
    private String shippingAddress;
}