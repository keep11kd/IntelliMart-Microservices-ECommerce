package com.intellimart.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private String userId;
    private List<CartItemResponse> items;
    // You might also have a total cart amount here, but we'll calculate it in order-service
}