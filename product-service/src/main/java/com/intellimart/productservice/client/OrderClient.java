package com.intellimart.productservice.client;

import com.intellimart.productservice.dto.OrderResponseForProductService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// CORRECTED: Removed 'url' attribute. Feign will now use Eureka for discovery.
@FeignClient(name = "order-service")
public interface OrderClient {

    /**
     * Fetches orders that contain a specific product ID.
     * This endpoint needs to be implemented in order-service.
     *
     * @param productId The ID of the product to search for in orders.
     * @return A list of orders containing the specified product.
     */
    @GetMapping("/api/orders/searchByProductId") // This endpoint needs to be created in OrderService
    List<OrderResponseForProductService> getOrdersByProductId(@RequestParam ("productId") Long productId);
}
