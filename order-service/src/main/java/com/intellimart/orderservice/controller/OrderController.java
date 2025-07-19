package com.intellimart.orderservice.controller;

import com.intellimart.orderservice.dto.ErrorResponse;
import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.exception.InsufficientStockException;
import com.intellimart.orderservice.exception.ResourceNotFoundException;
import com.intellimart.orderservice.model.OrderStatus;
import com.intellimart.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest; // <--- NEW IMPORT
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing customer orders in IntelliMart")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final HttpServletRequest request; // <--- INJECT HttpServletRequest

    // --- Exception Handlers ---
    // These methods will catch specific exceptions thrown by other methods in this controller
    // and return a standardized ErrorResponse.

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Sets HTTP 404 status
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .apiPath(request.getRequestURI()) // <--- Using injected request for accurate path
                .errorCode(HttpStatus.NOT_FOUND)
                .errorMessage(ex.getMessage())
                .errorTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Sets HTTP 400 status
    public ResponseEntity<ErrorResponse> handleInsufficientStockException(InsufficientStockException ex) {
        log.error("Insufficient stock: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .apiPath(request.getRequestURI()) // <--- Using injected request for accurate path
                .errorCode(HttpStatus.BAD_REQUEST)
                .errorMessage(ex.getMessage())
                .errorTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Sets HTTP 400 status
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .apiPath(request.getRequestURI()) // <--- Using injected request for accurate path
                .errorCode(HttpStatus.BAD_REQUEST)
                .errorMessage(ex.getMessage())
                .errorTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Sets HTTP 500 status
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .apiPath(request.getRequestURI()) // <--- Using injected request for accurate path
                .errorCode(HttpStatus.INTERNAL_SERVER_ERROR)
                .errorMessage("An unexpected internal server error occurred: " + ex.getMessage())
                .errorTime(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Removed `getCurrentRequestPath()` helper method as HttpServletRequest is now injected


    @Operation(
            summary = "Place a new order",
            description = "Creates a new order with specified products and quantities. Requires CUSTOMER or ADMIN role.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody( // <--- ENHANCED SWAGGER
                    description = "Order details including user ID and list of order items",
                    required = true,
                    content = @Content(schema = @Schema(implementation = OrderRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order placed successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input (e.g., validation errors), insufficient stock, or invalid request",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), // <--- MORE SPECIFIC DESCRIPTION
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER or ADMIN role"),
                    @ApiResponse(responseCode = "404", description = "Product not found referenced in order items",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - unexpected issue during order processing or communication with other services",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // <--- ADDED CONTENT SCHEMA
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody @Valid OrderRequest orderRequest)
            throws InsufficientStockException, ResourceNotFoundException {
        log.info("Received request to place order for userId: {}", orderRequest.getUserId());
        OrderResponse newOrder = orderService.placeOrder(orderRequest);
        log.info("Order placed successfully with orderNumber: {}", newOrder.getOrderNumber());
        return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Get order by ID (Admin/Internal only)",
            description = "Retrieves a single order by its ID. Requires ADMIN or INTERNAL role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or INTERNAL role"),
                    @ApiResponse(responseCode = "404", description = "Order not found with the given ID", // <--- MORE SPECIFIC DESCRIPTION
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "ID of the order to retrieve", required = true, example = "1")
            @PathVariable Long id) throws ResourceNotFoundException {
        log.info("Received request to get order by ID: {}", id);
        OrderResponse order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @Operation(
            summary = "Get current user's order history",
            description = "Retrieves a list of all orders placed by the currently authenticated user. Requires CUSTOMER or ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved order history",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class, type = "array"))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires CUSTOMER or ADMIN role"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - failed to retrieve user ID or orders",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // <--- ADDED CONTENT SCHEMA
            }
    )
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<OrderResponse>> getMyOrderHistory() {
        log.info("Received request to get current user's order history.");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // <--- REFINED ERROR HANDLING FOR CONSISTENCY WITH @ExceptionHandler
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Attempt to access /api/orders/me by unauthenticated/anonymous user. This should ideally be caught by @PreAuthorize.");
            throw new IllegalArgumentException("Authentication required or principal not found for user history.");
        }

        Long userId = null;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Priority 1: "userId" claim
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
                    // Let userId remain null to trigger the IllegalArgumentException below
                }
            }

            // Priority 2: "id" claim if "userId" not found/invalid
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

            // Priority 3: "sub" (subject) claim if others not found/invalid (assuming 'sub' is user ID)
            if (userId == null && jwt.getSubject() != null) {
                try {
                    userId = Long.parseLong(jwt.getSubject());
                } catch (NumberFormatException e) {
                    log.warn("JWT 'sub' claim is not a valid Long userId for subject: {}", jwt.getSubject());
                }
            }

        } else {
             log.error("Unsupported or unexpected authentication principal type: {}. Cannot extract userId.",
                       authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
             throw new RuntimeException("Unsupported authentication principal type for user ID extraction.");
        }

        if (userId == null) {
            log.error("Could not determine user ID from authentication context for /me endpoint.");
            // Throw an exception that will be caught by one of the handlers
            throw new IllegalArgumentException("User ID could not be determined from authentication context. Ensure JWT contains 'userId', 'id', or numeric 'sub' claim.");
        }

        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        log.info("Retrieved {} orders for userId: {}", orders.size(), userId);
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Get all orders with optional filters (Admin only)",
            description = "Retrieves a list of all orders, optionally filtered by status and creation date range. Requires ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved orders",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class, type = "array"))),
                    @ApiResponse(responseCode = "400", description = "Invalid status or date format provided", // <--- MORE SPECIFIC DESCRIPTION
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - unexpected issue",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // <--- ADDED CONTENT SCHEMA
            }
    )
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersAdmin(
            @Parameter(description = "Filter by order status (e.g., PENDING, SHIPPED, DELIVERED, CANCELLED)", example = "DELIVERED",
                       schema = @Schema(type = "string", allowableValues = {"PENDING", "SHIPPED", "DELIVERED", "CANCELLED"})) // <--- ENHANCED SWAGGER
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by creation date (start of range, inclusive). Format: YYYY-MM-DDTHH:MM:SS", example = "2023-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter by creation date (end of range, inclusive). Format: YYYY-MM-DDTHH:MM:SS", example = "2023-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Admin request to get all orders with filters - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);

        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            orderStatus = OrderStatus.valueOf(status.toUpperCase()); // Throws IllegalArgumentException if invalid, caught by handler
        }

        List<OrderResponse> orders = orderService.searchOrders(orderStatus, startDate, endDate);
        log.info("Admin retrieved {} orders.", orders.size());
        return ResponseEntity.ok(orders);
    }

    @Operation(
            summary = "Update order status (Admin only)",
            description = "Updates the status of a specific order by its ID. Requires ADMIN role. Valid statuses: PENDING, SHIPPED, DELIVERED, CANCELLED.",
            parameters = {
                    @Parameter(description = "ID of the order to update", required = true, example = "1"),
                    @Parameter(description = "New status for the order (e.g., SHIPPED, DELIVERED, CANCELLED)", required = true, example = "SHIPPED",
                               schema = @Schema(type = "string", allowableValues = {"PENDING", "SHIPPED", "DELIVERED", "CANCELLED"})) // <--- ENHANCED SWAGGER
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid new status provided",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN role"),
                    @ApiResponse(responseCode = "404", description = "Order not found",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - unexpected issue",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // <--- ADDED CONTENT SCHEMA
            }
    )
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String newStatus) throws ResourceNotFoundException, IllegalArgumentException {

        log.info("Admin request to update status for order ID: {} to {}", id, newStatus);
        OrderResponse updatedOrder = orderService.updateOrderStatus(id, newStatus);
        log.info("Order ID: {} status successfully updated to {}", id, updatedOrder.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }
}