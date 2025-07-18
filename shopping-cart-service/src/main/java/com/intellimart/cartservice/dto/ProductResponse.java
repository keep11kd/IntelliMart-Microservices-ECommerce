package com.intellimart.cartservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data // Lombok: Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates a constructor with all fields
public class ProductResponse {
    private Long id;          // The ID of the product
    private String name;      // The name of the product
    private String description; // The description of the product
    private BigDecimal price; // The price of the product
    private String imageUrl;  // The URL of the product's image
    // You can add other fields from your product-service's Product entity
    // if your shopping cart service needs them (e.g., brand, weight, etc.)
}