package com.intellimart.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseForProductService {
    private Long id;
    private Long userId;
    private String orderNumber;
    private List<OrderItemResponseForProductService> orderItems; // List of simplified order items
    // Add other fields if necessary for future logic, but keep it minimal for now
}