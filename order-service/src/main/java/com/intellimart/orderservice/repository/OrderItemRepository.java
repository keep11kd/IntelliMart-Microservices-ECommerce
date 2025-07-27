package com.intellimart.orderservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // Import for @Transactional

import com.intellimart.orderservice.model.Order;
import com.intellimart.orderservice.model.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// --- DTO Projection Interfaces (for optimized data fetching) ---
// These interfaces define a subset of fields from OrderItem, useful when you don't need the full entity.
// Spring Data JPA can automatically map query results to these.
interface OrderItemSummary {
    Long getId();
    Long getProductId();
    String getProductName();
    Integer getQuantity();
    BigDecimal getPriceAtPurchase();
    // Example of accessing a field from the associated Order entity
    String getOrder_OrderNumber(); // Note: This uses the underscore convention for nested properties
    LocalDateTime getOrder_CreatedAt(); // CORRECTED: Changed to match Order entity's 'createdAt' field
}

interface ProductSalesStats {
    Long getProductId();
    Long getTotalQuantitySold(); // Renamed for clarity in return
    BigDecimal getTotalRevenue();
}

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Finds all order items associated with a specific Order entity.
     * Useful when you already have the Order object loaded.
     * @param order The Order entity to find items for.
     * @return A list of OrderItem entities belonging to the given order.
     */
    List<OrderItem> findByOrder(Order order);

    /**
     * Finds all order items associated with a specific order ID.
     * This is generally more common in service layers where you pass IDs.
     * @param orderId The ID of the Order to find items for.
     * @return A list of OrderItem entities belonging to the given order ID.
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Finds all order items that contain a specific product ID across all orders.
     * Useful for analyzing which orders included a particular product.
     * @param productId The ID of the product.
     * @return A list of OrderItem entities containing the specified product.
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Finds a specific order item within a given order by its order ID and product ID.
     * This is highly efficient for checking if a particular product is in a particular order.
     * @param orderId The ID of the Order.
     * @param productId The ID of the product.
     * @return An Optional containing the OrderItem if found, otherwise empty.
     */
    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    /**
     * Finds all order items for a specific user ID by traversing the relationship to the Order.
     * This is a convenient derived query using the '_' convention for nested properties.
     * @param userId The ID of the user who placed the order.
     * @return A list of OrderItem entities associated with the given user.
     */
    List<OrderItem> findByOrder_UserId(Long userId);

    /**
     * Finds order items where the quantity is greater than a specified value.
     * @param quantity The minimum quantity threshold.
     * @return A list of OrderItem entities with quantity greater than the given value.
     */
    List<OrderItem> findByQuantityGreaterThan(Integer quantity);

    /**
     * Finds order items where the price at purchase falls within a specified range.
     * @param minPrice The minimum price (inclusive).
     * @param maxPrice The maximum price (inclusive).
     * @return A list of OrderItem entities within the specified price range.
     */
    List<OrderItem> findByPriceAtPurchaseBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Finds order items that were part of orders placed within a specific date range.
     * Useful for time-based reporting.
     * @param startDate The start date of the order (inclusive).
     * @param endDate The end date of the order (inclusive).
     * @return A list of OrderItem entities from orders placed within the date range.
     */
    List<OrderItem> findByOrder_CreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate); // CORRECTED: Changed to match Order entity's 'createdAt' field

    /**
     * Finds order items associated with orders of a specific status.
     * @param orderStatus The status of the parent order (e.g., OrderStatus.DELIVERED).
     * @return A list of OrderItem entities from orders with the given status.
     */
    List<OrderItem> findByOrder_Status(com.intellimart.orderservice.model.OrderStatus orderStatus);

    /**
     * Finds all order items for a given order ID, with pagination and sorting support.
     * This is crucial for large result sets to avoid fetching all data at once.
     * @param orderId The ID of the Order.
     * @param pageable Pagination and sorting information.
     * @return A Page of OrderItem entities.
     */
    Page<OrderItem> findByOrderId(Long orderId, Pageable pageable);


    // --- Custom Queries using @Query Annotation for Advanced Scenarios ---

    /**
     * Calculates the total quantity sold for a specific product across all orders.
     * This is an aggregation query using JPQL. COALESCE ensures 0 is returned if no items are found.
     * @param productId The ID of the product.
     * @return The total quantity of the product sold, or 0 if not found.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer findTotalQuantitySoldByProductId(@Param("productId") Long productId);

    /**
     * Calculates the total revenue generated by a specific product across all orders.
     * @param productId The ID of the product.
     * @return The total revenue for the product, or BigDecimal.ZERO if not found.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity * oi.priceAtPurchase), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    BigDecimal findTotalRevenueByProductId(@Param("productId") Long productId);

    /**
     * Retrieves a simplified view of OrderItem details using a DTO projection (interface-based).
     * This is highly recommended for performance when you don't need the full entity graph.
     * @param orderId The ID of the order.
     * @return A list of OrderItemSummary projections for the given order.
     */
    @Query("SELECT oi.id as id, oi.productId as productId, oi.productName as productName, " +
           "oi.quantity as quantity, oi.priceAtPurchase as priceAtPurchase, " +
           "o.orderNumber as order_OrderNumber, o.createdAt as order_CreatedAt " + // CORRECTED: Changed to match Order entity's 'createdAt' field
           "FROM OrderItem oi JOIN oi.order o WHERE o.id = :orderId")
    List<OrderItemSummary> findOrderItemSummariesByOrderId(@Param("orderId") Long orderId);

    /**
     * Finds the most frequently purchased products based on total quantity sold.
     * This returns a list of projections with product ID, total quantity sold, and total revenue.
     * @param limit The maximum number of top products to return.
     * @return A list of ProductSalesStats projections.
     */
    @Query("SELECT oi.productId as productId, SUM(oi.quantity) as totalQuantitySold, " +
           "SUM(oi.quantity * oi.priceAtPurchase) as totalRevenue " +
           "FROM OrderItem oi " +
           "GROUP BY oi.productId " +
           "ORDER BY totalQuantitySold DESC")
    List<ProductSalesStats> findTopSellingProducts(@Param("limit") int limit, Pageable pageable);
    // Note: Using Pageable for LIMIT/OFFSET functionality in JPQL is generally preferred over native LIMIT.

    // --- Bulk Update/Delete Operations ---

    /**
     * Updates the 'priceAtPurchase' for all items of a specific product ID.
     * Use with caution as this affects historical order data.
     * Requires @Modifying and @Transactional.
     * @param productId The ID of the product.
     * @param newPrice The new price to set.
     * @return The number of entities updated.
     */
    @Modifying
    @Transactional // Always add @Transactional for modifying queries
    @Query("UPDATE OrderItem oi SET oi.priceAtPurchase = :newPrice WHERE oi.productId = :productId")
    int updatePriceAtPurchaseByProductId(@Param("productId") Long productId, @Param("newPrice") BigDecimal newPrice);

    /**
     * Deletes all order items associated with a specific order.
     * Use with caution. Consider orphanRemoval=true on the @OneToMany relationship if cascade is set for deletion.
     * @param order The Order entity whose items are to be deleted.
     */
    @Modifying
    @Transactional
    void deleteByOrder(Order order);

    /**
     * Deletes all order items associated with a specific order ID.
     * More practical for direct deletion using an ID.
     * @param orderId The ID of the order whose items are to be deleted.
     * @return The number of items deleted.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OrderItem oi WHERE oi.order.id = :orderId")
    int deleteByOrderId(@Param("orderId") Long orderId);
}
