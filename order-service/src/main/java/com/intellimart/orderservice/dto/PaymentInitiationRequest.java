package com.intellimart.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for initiating a payment for an existing order")
public class PaymentInitiationRequest {
    @NotNull(message = "Order ID cannot be null")
    @Schema(description = "The ID of the order for which payment is to be initiated", example = "1")
    private Long orderId;
}