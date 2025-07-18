package com.intellimart.cartservice.dto; // Make sure this package declaration is correct

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemViewResponse {
    private Long productId;
    private Integer quantity;
    private String productName;
    private BigDecimal currentPrice;
    private String imageUrl;
    private BigDecimal lineItemTotal;
}