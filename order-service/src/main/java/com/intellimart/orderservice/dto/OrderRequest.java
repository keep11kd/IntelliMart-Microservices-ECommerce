package com.intellimart.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema; // <--- NEW IMPORT
import jakarta.validation.Valid;
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
@Schema(description = "Request DTO for creating or placing an order")
public class OrderRequest {
    @NotNull(message = "User ID cannot be null")
    @Schema(description = "The ID of the user placing the order (as a String)", example = "101")
    private String userId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid // Ensures validation is applied to each item in the list
    @Schema(description = "List of items included in the order")
    private List<OrderItemRequest> orderItems;

    @Schema(description = "Payment information for the order (e.g., 'Credit Card', 'PayPal')", example = "Credit Card (Visa ending 1234)")
    private String paymentInfo;

    @Schema(description = "Shipping address for the order", example = "123 Main St, Anytown, USA")
    private String shippingAddress;
}