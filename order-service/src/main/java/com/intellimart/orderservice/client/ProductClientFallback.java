package com.intellimart.orderservice.client;

import com.intellimart.orderservice.dto.ProductResponse;
import com.intellimart.orderservice.dto.StockDecrementRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Fallback implementation for ProductClient.
 * This class provides default responses when the product-service is unavailable
 * or experiences an error. It receives the cause of the failure.
 */
@Slf4j
public class ProductClientFallback implements ProductClient {

    private final Throwable cause; // Field to store the cause of the fallback

    public ProductClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public ResponseEntity<ProductResponse> getProductById(Long productId) {
        log.error("Fallback activated: Could not retrieve product {} details. Cause: {}", productId, cause.getMessage());
        // Return a default/empty product response, or throw a specific exception if product details are critical
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
    }

    @Override
    public ResponseEntity<Void> decrementStock(StockDecrementRequest request) {
        log.error("Fallback activated: Could not decrement stock for product {}. Quantity: {}. Cause: {}",
                request.getProductId(), request.getQuantity(), cause.getMessage());
        // When stock deduction fails due to service unavailability, it's a critical error for order creation.
        // We'll return an HTTP 503 Service Unavailable to indicate the external service issue.
        // The calling service (OrderService) should handle this by rolling back the order.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
}