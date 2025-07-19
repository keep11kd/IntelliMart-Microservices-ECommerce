package com.intellimart.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String id; // Assuming product ID is String
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock; // Current stock level
    private String imageUrl;
    // Add any other fields you need from the product service
}