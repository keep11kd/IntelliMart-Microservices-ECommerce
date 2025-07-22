package com.intellimart.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO for payment initiation, containing details for frontend checkout")
public class PaymentInitiationResponse {
    @Schema(description = "Our internal Order ID", example = "1")
    private Long orderId;

    @Schema(description = "Razorpay's Order ID, used by frontend for checkout", example = "order_xxxxxxxxxxxxxx")
    private String razorpayOrderId;

    @Schema(description = "Razorpay Key ID, used by frontend for checkout", example = "rzp_test_xxxxxxxxxxxxxx")
    private String razorpayKeyId; // <--- Ensure this field exists

    @Schema(description = "Total amount of the order in the smallest currency unit (e.g., paise for INR)", example = "100000")
    private Integer amountInPaise;

    @Schema(description = "Currency of the payment (e.g., INR)", example = "INR")
    private String currency;

    @Schema(description = "User's name for checkout pre-fill", example = "John Doe")
    private String userName; // Optional, for pre-filling checkout form

    @Schema(description = "User's email for checkout pre-fill", example = "john.doe@example.com")
    private String userEmail; // Optional, for pre-filling checkout form

    @Schema(description = "User's phone number for checkout pre-fill", example = "+919876543210")
    private String userPhone; // Optional, for pre-filling checkout form
}