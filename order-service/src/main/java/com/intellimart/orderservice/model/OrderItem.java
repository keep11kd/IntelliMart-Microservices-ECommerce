package com.intellimart.orderservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Link to the parent Order

    @Column(nullable = false)
    private Long productId; // ID of the product from product-service (changed to String for consistency with userId)

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase; // Price of the product at the time of order creation

    // Optional: Denormalized product name and image URL for reporting/display purposes
    @Column(length = 255)
    private String productName;
    @Column(length = 1024) // URLs can be long
    private String imageUrl;
}