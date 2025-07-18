package com.intellimart.productservice.repository;

import com.intellimart.productservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query; // !!! NEW IMPORT: For custom JPQL queries
import org.springframework.data.repository.query.Param; // !!! NEW IMPORT: For named parameters in @Query

import java.math.BigDecimal; // !!! NEW IMPORT: For price filtering
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- Existing Methods (keeping them as they might be used elsewhere) ---
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByDescriptionContainingIgnoreCase(String description);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndCategoryId(String name, String description, Long categoryId);

    // --- Existing Pagination and Sorting Methods ---
    Page<Product> findAll(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String searchTerm, String searchTerm2, Pageable pageable);

    // --- NEW: Filtering by Price Range (using Spring Data JPA method names) ---

    /**
     * Finds products within a specified price range, with pagination and sorting.
     * @param minPrice The minimum price (inclusive).
     * @param maxPrice The maximum price (inclusive).
     * @param pageable Pagination and sorting information.
     * @return A Page of products matching the criteria.
     */
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Finds products by category ID and within a specified price range, with pagination and sorting.
     * @param categoryId The ID of the category.
     * @param minPrice The minimum price (inclusive).
     * @param maxPrice The maximum price (inclusive).
     * @param pageable Pagination and sorting information.
     * @return A Page of products matching the criteria.
     */
    Page<Product> findByCategoryIdAndPriceBetween(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // --- NEW: Flexible Filtering with @Query (Recommended for combining optional filters) ---

    /**
     * Finds products based on multiple optional criteria: name/description search, category, and price range.
     * This method uses a JPQL query to handle null parameters for optional filtering.
     *
     * @param nameQuery Optional: Part of product name to search (case-insensitive).
     * @param descriptionQuery Optional: Part of product description to search (case-insensitive).
     * @param categoryId Optional: ID of the category.
     * @param minPrice Optional: Minimum price.
     * @param maxPrice Optional: Maximum price.
     * @param pageable Pagination and sorting information.
     * @return A Page of products matching the provided criteria.
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:nameQuery IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :nameQuery, '%'))) AND " +
           "(:descriptionQuery IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :descriptionQuery, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findProductsByCriteria(
            @Param("nameQuery") String nameQuery,
            @Param("descriptionQuery") String descriptionQuery,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}