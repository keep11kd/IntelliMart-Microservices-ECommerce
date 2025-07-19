package com.intellimart.orderservice.client;

import com.intellimart.orderservice.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign Client for interacting with the shopping-cart-service.
 * The 'name' attribute must match the 'spring.application.name' of the shopping-cart-service
 * as registered in Eureka.
 */
@FeignClient(name = "shopping-cart-service")
public interface ShoppingCartClient {

    /**
     * Retrieves the cart contents for a given user.
     * Assumes the shopping-cart-service has an endpoint like GET /api/cart/{userId}
     *
     * @param userId The ID of the user whose cart to retrieve.
     * @return CartResponse containing the user's cart details.
     */
    @GetMapping("/api/cart/{userId}")
    CartResponse getCartByUserId(@PathVariable("userId") String userId);

    /**
     * Clears the cart for a given user after an order has been placed.
     * Assumes the shopping-cart-service has an endpoint like POST /api/cart/{userId}/clear
     *
     * @param userId The ID of the user whose cart to clear.
     */
    @PostMapping("/api/cart/{userId}/clear")
    void clearCartByUserId(@PathVariable("userId") String userId);

    // You might add more methods here later, e.g., to validate inventory
    // @GetMapping("/api/cart/{userId}/validate-inventory")
    // Boolean validateCartInventory(@PathVariable("userId") String userId);
}