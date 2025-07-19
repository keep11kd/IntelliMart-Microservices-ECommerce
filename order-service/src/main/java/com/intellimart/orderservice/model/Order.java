package com.intellimart.orderservice.model; // Keeping your package name

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID; // New import for UUID

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL, so "orders" is good
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // Changed to Long, assuming numeric user IDs from auth-service

    @Column(unique = true, nullable = false, updatable = false) // Ensure order number is unique and not updatable
    private String orderNumber; // Unique identifier for the order (e.g., UUID)

    @Enumerated(EnumType.STRING) // Store enum as String in DB
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2) // Precision for total amount
    private BigDecimal totalAmount;

    @Column(length = 255) // Length for payment information
    private String paymentInfo; // Simplified for now, could be a JSON or reference to Payment service

    @Column(length = 500) // Length for shipping address
    private String shippingAddress; // Full shipping address string

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @Column(name = "created_at", updatable = false) // Audit field for creation timestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at") // Audit field for last update timestamp
    private LocalDateTime updatedAt;

    /**
     * Lifecycle callback method to set default values before persisting a new entity.
     * This ensures `createdAt`, `updatedAt`, `status`, and `orderNumber` are initialized automatically.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now(); // Initialize updatedAt as well
        if (this.status == null) {
            this.status = OrderStatus.PENDING; // Default status for a new order
        }
        if (this.orderNumber == null) {
            this.orderNumber = UUID.randomUUID().toString(); // Generate unique order number using UUID
        }
    }

    /**
     * Lifecycle callback method to update `updatedAt` before updating an existing entity.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}