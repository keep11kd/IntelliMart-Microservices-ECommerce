package com.intellimart.orderservice.controller;

import com.intellimart.orderservice.dto.ErrorResponse;
import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.exception.InsufficientStockException; // Added for specific exception handling
import com.intellimart.orderservice.exception.ResourceNotFoundException;
import com.intellimart.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime; // Required if ErrorResponse uses LocalDateTime
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing customer orders in IntelliMart")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Place a new order",
            description = "Creates a new order with specified products and quantities. Requires CUSTOMER or ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order placed successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input, insufficient stock, or order processing error",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER or ADMIN role"),
                    @ApiResponse(responseCode = "404", description = "Product not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - unexpected issue")
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody @Valid OrderRequest orderRequest) {
        log.info("Received request to place order for userId: {}", orderRequest.getUserId());
        try {
            OrderResponse newOrder = orderService.placeOrder(orderRequest);
            log.info("Order placed successfully with orderNumber: {}", newOrder.getOrderNumber());
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            log.warn("Order placement failed: Product not found. {}", e.getMessage());
            // It's good practice to return an ErrorResponse here if your API contract specifies it.
            // For now, returning null with a status.
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (InsufficientStockException e) { // Catching specific stock exception
            log.warn("Order placement failed due to insufficient stock: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Error placing order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Get order by ID (Admin/Internal only)",
            description = "Retrieves a single order by its ID. Requires ADMIN or INTERNAL role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or INTERNAL role"),
                    @ApiResponse(responseCode = "404", description = "Order not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "ID of the order to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Received request to get order by ID: {}", id);
        try {
            OrderResponse order = orderService.getOrderById(id);
            return ResponseEntity.ok(order);
        } catch (ResourceNotFoundException e) {
            log.warn("Order not found for ID: {}. {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
            summary = "Get current user's order history",
            description = "Retrieves a list of all orders placed by the currently authenticated user. Requires CUSTOMER or ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved order history",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class, type = "array"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER or ADMIN role"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - failed to retrieve user ID or orders")
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<OrderResponse>> getMyOrderHistory() {
        log.info("Received request to get current user's order history.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Attempt to access /api/orders/me by unauthenticated/anonymous user. This should ideally be caught by @PreAuthorize.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = null;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Option 1: Try to get 'userId' claim directly as Long/Integer, then parse String
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Long) {
                userId = (Long) userIdClaim;
            } else if (userIdClaim instanceof Integer) {
                userId = ((Integer) userIdClaim).longValue();
            } else if (userIdClaim instanceof String) {
                try {
                    userId = Long.parseLong((String) userIdClaim);
                } catch (NumberFormatException e) {
                    log.warn("JWT 'userId' claim is a String but not a valid Long: {}", userIdClaim);
                }
            }

            // Option 2: If 'userId' not found or not parseable, try 'id' claim
            if (userId == null) {
                Object idClaim = jwt.getClaim("id");
                if (idClaim instanceof Long) {
                    userId = (Long) idClaim;
                } else if (idClaim instanceof Integer) {
                    userId = ((Integer) idClaim).longValue();
                } else if (idClaim instanceof String) {
                    try {
                        userId = Long.parseLong((String) idClaim);
                    } catch (NumberFormatException e) {
                        log.warn("JWT 'id' claim is a String but not a valid Long: {}", idClaim);
                    }
                }
            }

            // Option 3: If still null, try 'sub' (subject) claim as a last resort
            // IMPORTANT: 'sub' often contains the username (String). Only parse to Long if you are
            // absolutely sure your 'sub' claim *is* the numeric user ID.
            if (userId == null && jwt.getSubject() != null) {
                try {
                    userId = Long.parseLong(jwt.getSubject());
                } catch (NumberFormatException e) {
                    log.warn("JWT 'sub' claim is not a valid Long userId for subject: {}", jwt.getSubject());
                    // If 'sub' is a username, you might need to query a user service to get the ID.
                    // For example: userId = userService.getUserIdByUsername(jwt.getSubject());
                }
            }

        } else {
             // Handle other principal types if necessary (e.g., custom UserDetails)
             log.error("Unsupported or unexpected authentication principal type: {}. Cannot extract userId.",
                       authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (userId == null) {
            log.error("Could not determine user ID from authentication context for /me endpoint. Access denied.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            // FIX: Now correctly calls getOrdersByUserId with a Long argument
            List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
            log.info("Retrieved {} orders for userId: {}", orders.size(), userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error retrieving order history for userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}