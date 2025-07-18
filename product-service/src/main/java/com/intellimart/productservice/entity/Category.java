package com.intellimart.productservice.entity;

import jakarta.persistence.*; // For JPA annotations
import lombok.AllArgsConstructor; // Lombok: Generates all-args constructor
import lombok.Data;             // Lombok: Generates getters, setters, toString, equals, hashCode
import lombok.NoArgsConstructor;  // Lombok: Generates no-args constructor
import java.time.LocalDateTime;   // For auditing timestamps
import lombok.Builder;
@Entity // Marks this class as a JPA entity
@Table(name = "categories") // Specifies the table name in the database
@Data // Lombok: Generates boilerplate code (getters, setters, etc.)
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates an all-argument constructor
@Builder // !!! NEW: Add this annotation
public class Category {
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments primary key for MySQL/PostgreSQL/H2
    private Long id;

    @Column(nullable = false, unique = true) // Column constraints: not null and unique
    private String name;

    private String description;

    @Column(name = "created_at", updatable = false) // Audit field: set on creation, not updatable
    private LocalDateTime createdAt;

    @Column(name = "updated_at") // Audit field: updated on modification
    private LocalDateTime updatedAt;

    // --- Lifecycle Callbacks (Optional, but good practice for auditing) ---
    @PrePersist // Executed before the entity is first persisted
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate // Executed before the entity is updated
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}