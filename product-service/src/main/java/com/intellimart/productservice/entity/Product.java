package com.intellimart.productservice.entity;

import jakarta.persistence.*; // For JPA annotations
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity // Marks this class as a JPA entity
@Table(name = "products") // Specifies the table name
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // !!! NEW: Add this annotation
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    private String imageUrl; // Field for image URL (added in Day 10, but good to have now)

    // --- Relationship to Category ---
    @ManyToOne(fetch = FetchType.LAZY) // Many products can belong to one category. LAZY loading for performance.
    @JoinColumn(name = "category_id", nullable = false) // Foreign key column in 'products' table
    private Category category; // Reference to the Category entity

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- Lifecycle Callbacks ---
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}