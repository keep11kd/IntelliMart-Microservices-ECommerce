package com.intellimart.orderservice.client;

import com.intellimart.orderservice.dto.ProductResponse;
import com.intellimart.orderservice.dto.StockDecrementRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity; // For more detailed response handling

/**
 * Feign Client for interacting with the product-service.
 * The 'name' attribute must match the 'spring.application.name' of the product-service
 * as registered in Eureka.
 * Uses a FallbackFactory for resilience.
 */
@FeignClient(name = "product-service", fallbackFactory = ProductClientFallbackFactory.class)
public interface ProductClient {

    /**
     * Retrieves product details by ID.
     * Assumes product-service has an endpoint like GET /api/products/{id}
     *
     * @param productId The ID of the product.
     * @return ProductResponse containing product details.
     */
    @GetMapping("/api/products/{productId}")
    ResponseEntity<ProductResponse> getProductById(@PathVariable("productId") Long productId);

    /**
     * Decrements the stock for a given product.
     * Assumes product-service has an endpoint like POST /api/products/decrement-stock
     * This endpoint should return HTTP 200 OK on success, or an appropriate error status
     * (e.g., 400 Bad Request if stock is insufficient).
     *
     * @param request StockDecrementRequest containing productId and quantity.
     * @return ResponseEntity indicating success or failure.
     */
    @PostMapping("/api/products/decrement-stock")
    ResponseEntity<Void> decrementStock(@RequestBody StockDecrementRequest request);
}