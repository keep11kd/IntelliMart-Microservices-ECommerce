package com.intellimart.orderservice.controller;

import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.exception.InsufficientStockException; // Import new exception
import com.intellimart.orderservice.exception.ResourceNotFoundException;
import com.intellimart.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j // For logging
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody @Valid OrderRequest orderRequest) {
        log.info("Received request to place an order (direct): {}", orderRequest);
        try {
            OrderResponse orderResponse = orderService.placeOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (InsufficientStockException e) {
            log.warn("Failed to place direct order due to insufficient stock: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return 400 Bad Request
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to place direct order due to product not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // Return 404 Not Found
        } catch (Exception e) {
            log.error("An unexpected error occurred while placing direct order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint for Day 23: Create an order from a user's shopping cart.
     * Now handles InsufficientStockException.
     * @param userId The ID of the user whose cart will be converted to an order.
     * @return The created OrderResponse.
     */
    @PostMapping("/from-cart/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OrderResponse> createOrderFromCart(@PathVariable String userId) {
        log.info("Received request to create order from cart for user ID: {}", userId);
        try {
            OrderResponse orderResponse = orderService.createOrderFromCart(userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to create order from cart for user ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Cart empty or product not found
        } catch (InsufficientStockException e) {
            log.warn("Failed to create order from cart due to insufficient stock: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return 400 Bad Request
        } catch (Exception e) {
            log.error("An unexpected error occurred while creating order from cart for user ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        try {
            OrderResponse orderResponse = orderService.getOrderById(id);
            log.info("Found order with ID: {}", id);
            return ResponseEntity.ok(orderResponse);
        } catch (ResourceNotFoundException e) {
            log.warn("Order not found for ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders.");
        return orderService.getAllOrders();
    }

    @GetMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getOrdersByUserId(@PathVariable String userId) {
        log.info("Fetching orders for user ID: {}", userId);
        return orderService.getOrdersByUserId(userId);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            OrderResponse updatedOrder = orderService.updateOrderStatus(id, status);
            log.info("Successfully updated order ID: {} status to {}", id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (ResourceNotFoundException e) {
            log.warn("Order not found for ID: {}. Cannot update status.", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid status provided for order ID {}: {}", id, status);
            return ResponseEntity.badRequest().body(null); // Or return a custom error response
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            log.info("Order ID: {} deleted successfully.", id);
        } catch (ResourceNotFoundException e) {
            log.warn("Order not found for ID: {}. Cannot delete.", id);
            // Spring will automatically return 404 due to @ResponseStatus(HttpStatus.NOT_FOUND) on ResourceNotFoundException
            throw e;
        }
    }
}