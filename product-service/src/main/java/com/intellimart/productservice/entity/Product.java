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
@Builder // !!! NEW: Add this annotation - This is good, ensures you can use Product.builder()
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Correct for auto-incrementing Long ID
    private Long id; // ID is now Long

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;
    @Column(length = 1024) // URL for product image
    private String imageUrl; // Field for image URL

    // --- Relationship to Category ---
    @ManyToOne(fetch = FetchType.LAZY) // Correct for Many products to one Category
    @JoinColumn(name = "category_id", nullable = false) // Foreign key column
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