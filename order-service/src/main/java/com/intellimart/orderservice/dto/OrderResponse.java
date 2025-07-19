package com.intellimart.orderservice.dto;

import com.intellimart.orderservice.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema; // <--- NEW IMPORT
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO for order details")
public class OrderResponse {
    @Schema(description = "Unique ID of the order", example = "1")
    private Long id;
    @Schema(description = "ID of the user who placed the order", example = "101")
    private Long userId;
    @Schema(description = "Unique human-readable order number (UUID)", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String orderNumber;
    @Schema(description = "Current status of the order", example = "PENDING", allowableValues = {"PENDING", "SHIPPED", "DELIVERED", "CANCELLED"})
    private OrderStatus status;
    @Schema(description = "Total monetary amount of the order", example = "199.99")
    private BigDecimal totalAmount;
    @Schema(description = "Payment details for the order", example = "Credit Card (Visa ending 1234)")
    private String paymentInfo;
    @Schema(description = "Shipping address for the order", example = "123 Main St, Anytown, USA")
    private String shippingAddress;
    @Schema(description = "List of individual items in the order")
    private List<OrderItemResponse> orderItems;
    @Schema(description = "Timestamp when the order was created", example = "2023-10-26T10:00:00")
    private LocalDateTime createdAt;
    @Schema(description = "Timestamp when the order was last updated", example = "2023-10-26T10:30:00")
    private LocalDateTime updatedAt;
}