package com.intellimart.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartViewResponse {
    private String userId;
    private List<CartItemViewResponse> items;
    private BigDecimal totalPrice;
    private Integer totalItems;
    private Integer totalQuantity;
}