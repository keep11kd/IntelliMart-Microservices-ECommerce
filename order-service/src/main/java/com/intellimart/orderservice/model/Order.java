package com.intellimart.orderservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so use "orders" or similar
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // ID of the user who placed the order (as a String for flexibility)

    @Column(unique = true, nullable = false) // Ensure order number is unique
    private String orderNumber; // Unique identifier for the order (e.g., UUID or sequence)

    @Enumerated(EnumType.STRING) // Store enum as String in DB
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false, precision = 10, scale = 2) // Precision for total amount (e.g., up to 99,999,999.99)
    private BigDecimal totalAmount;

    @Column(length = 255) // Length for payment information
    private String paymentInfo; // Simplified for now, could be a JSON or reference to Payment service

    @Column(length = 500) // Length for shipping address
    private String shippingAddress; // Full shipping address string

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    /**
     * Lifecycle callback method to set default values before persisting a new entity.
     * This ensures `orderDate`, `status`, and `orderNumber` are initialized automatically.
     */
    @PrePersist
    protected void onCreate() {
        if (this.orderDate == null) {
            this.orderDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING; // Default status for a new order
        }
        // Generate a simple order number using System.nanoTime() for uniqueness.
        // In a production system, consider a more robust ID generation service (e.g., UUID.randomUUID()).
        if (this.orderNumber == null) {
            this.orderNumber = "ORD-" + System.nanoTime();
        }
    }
}