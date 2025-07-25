package com.intellimart.productservice.client;

import com.intellimart.productservice.dto.OrderResponseForProductService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "order-service", url = "${application.config.order-service.url}") // IMPORTANT: Configure URL in application.properties
public interface OrderClient {

    /**
     * Fetches orders that contain a specific product ID.
     * This endpoint needs to be implemented in order-service.
     *
     * @param productId The ID of the product to search for in orders.
     * @return A list of orders containing the specified product.
     */
    @GetMapping("/api/orders/searchByProductId") // This endpoint needs to be created in OrderService
    List<OrderResponseForProductService> getOrdersByProductId(@RequestParam Long productId);
}