package com.intellimart.orderservice.service;

import com.intellimart.orderservice.client.ShoppingCartClient;
import com.intellimart.orderservice.client.ProductClient;
import com.intellimart.orderservice.dto.CartItemResponse;
import com.intellimart.orderservice.dto.CartResponse;
import com.intellimart.orderservice.dto.OrderItemRequest;
import com.intellimart.orderservice.dto.OrderItemResponse;
import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.dto.ProductResponse;
import com.intellimart.orderservice.dto.StockDecrementRequest;
import com.intellimart.orderservice.exception.InsufficientStockException;
import com.intellimart.orderservice.exception.ResourceNotFoundException;
import com.intellimart.orderservice.model.Order;
import com.intellimart.orderservice.model.OrderItem;
import com.intellimart.orderservice.model.OrderStatus;
import com.intellimart.orderservice.repository.OrderItemRepository;
import com.intellimart.orderservice.repository.OrderRepository;
import com.intellimart.orderservice.util.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional // Ensure transactional consistency across database operations
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShoppingCartClient shoppingCartClient;
    private final ProductClient productClient;

    /**
     * Places a new order based on a direct OrderRequest.
     * Includes inventory deduction and detailed logging for distributed calls.
     */
    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) throws InsufficientStockException, ResourceNotFoundException {
        log.info("Attempting to place new order for user ID: {}", orderRequest.getUserId());

        Long userId = Long.parseLong(orderRequest.getUserId()); // Ensure consistency with Long userId

        List<OrderItem> orderItems = new java.util.ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            ProductResponse product;
            log.debug("Processing order item for product ID: {} with quantity: {}", itemRequest.getProductId(), itemRequest.getQuantity());

            try {
                // Synchronous call to product-service to get product details and prepare for stock decrement
                log.info("Calling product-service to get details for product ID: {}", itemRequest.getProductId());
                ResponseEntity<ProductResponse> productResponse = productClient.getProductById(itemRequest.getProductId());

                if (productResponse.getStatusCode() != HttpStatus.OK || productResponse.getBody() == null) {
                    log.error("Product not found or unavailable from product-service for ID: {}. Status: {}", itemRequest.getProductId(), productResponse.getStatusCode());
                    throw new ResourceNotFoundException("Product not found or unavailable: " + itemRequest.getProductId());
                }
                product = productResponse.getBody();
                log.info("Received product details for ID: {}. Name: {}, Stock: {}", product.getId(), product.getName(), product.getStock());

                // Price consistency check (optional but good)
                if (product.getPrice().compareTo(itemRequest.getPriceAtPurchase()) != 0) {
                    log.warn("Price mismatch for product {}. Order item price: {}, Actual product price: {}. Using order item price for this order.",
                            itemRequest.getProductId(), itemRequest.getPriceAtPurchase(), product.getPrice());
                }

                // Synchronous call to product-service to decrement stock
                StockDecrementRequest decrementRequest = new StockDecrementRequest(itemRequest.getProductId(), itemRequest.getQuantity());
                log.info("Attempting to decrement stock for product ID: {} by quantity: {}", itemRequest.getProductId(), itemRequest.getQuantity());
                ResponseEntity<Void> decrementResponse = productClient.decrementStock(decrementRequest);

                if (decrementResponse.getStatusCode() != HttpStatus.OK) {
                    if (decrementResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        String errorMessage = "Insufficient stock for product ID: " + itemRequest.getProductId() +
                                              ". Requested: " + itemRequest.getQuantity() + ". Available stock might be less.";
                        log.error(errorMessage);
                        throw new InsufficientStockException(errorMessage);
                    } else {
                        // General error from product-service during stock decrement
                        String errorMessage = String.format("Failed to decrement stock for product ID: %s. Product service returned status: %s. Response body: %s",
                                itemRequest.getProductId(), decrementResponse.getStatusCode(), decrementResponse.getBody());
                        log.error(errorMessage);
                        throw new RuntimeException(errorMessage); // This will trigger transaction rollback
                    }
                }
                log.info("Stock successfully decremented for product ID: {} by {}", itemRequest.getProductId(), itemRequest.getQuantity());

            } catch (ResourceNotFoundException | InsufficientStockException e) {
                // Re-throw specific, known exceptions for @ExceptionHandler to catch
                throw e;
            } catch (Exception e) {
                // Catch any other unexpected errors from Feign client (network, service down, etc.)
                log.error("Critical error during product service interaction for product ID {}: {}", itemRequest.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Failed to process order due to product service issue for product " + itemRequest.getProductId() + ": " + e.getMessage(), e);
            }

            BigDecimal itemTotalPrice = itemRequest.getPriceAtPurchase().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(itemRequest.getPriceAtPurchase())
                    .productName(product.getName())
                    .imageUrl(product.getImageUrl())
                    .build();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .paymentInfo(orderRequest.getPaymentInfo() != null ? orderRequest.getPaymentInfo() : "N/A - Direct Order")
                .shippingAddress(orderRequest.getShippingAddress() != null ? orderRequest.getShippingAddress() : "N/A - Direct Order")
                .orderItems(orderItems)
                .build();

        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(item -> item.setOrder(savedOrder)); // Establish bidirectional link

        log.info("Order placed successfully with ID: {} and orderNumber: {}", savedOrder.getId(), savedOrder.getOrderNumber());
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Creates an order by fetching cart contents from the shopping-cart-service.
     * Includes inventory deduction and detailed logging.
     *
     * @param userIdString The ID of the user whose cart should be converted to an order.
     * @return OrderResponse representing the newly created order.
     * @throws ResourceNotFoundException if the cart is empty or user not found.
     * @throws InsufficientStockException if stock is insufficient for any item.
     */
    @Override
    public OrderResponse createOrderFromCart(String userIdString) throws ResourceNotFoundException, InsufficientStockException {
        log.info("Attempting to create order from cart for user ID: {}", userIdString);

        Long userId = Long.parseLong(userIdString);

        // Synchronous call to shopping-cart-service
        CartResponse cart;
        try {
            log.info("Calling shopping-cart-service to get cart for user ID: {}", userIdString);
            cart = shoppingCartClient.getCartByUserId(userIdString);
            log.info("Received cart for user ID: {}. Items count: {}", userIdString, cart != null ? cart.getItems().size() : 0);
        } catch (Exception e) {
            log.error("Failed to retrieve cart from shopping-cart-service for user ID {}: {}", userIdString, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve shopping cart for user " + userIdString + ": " + e.getMessage(), e);
        }

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Cannot create order: Cart is empty for user ID: {}", userIdString);
            throw new ResourceNotFoundException("Cannot create order: Cart is empty for user ID " + userIdString);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

        for (CartItemResponse cartItem : cart.getItems()) {
            ProductResponse product;
            log.debug("Processing cart item for product ID: {} with quantity: {}", cartItem.getProductId(), cartItem.getQuantity());

            try {
                // Synchronous call to product-service to get product details and decrement stock
                log.info("Calling product-service to get details for product ID: {}", cartItem.getProductId());
                ResponseEntity<ProductResponse> productResponse = productClient.getProductById(cartItem.getProductId());

                if (productResponse.getStatusCode() != HttpStatus.OK || productResponse.getBody() == null) {
                    log.error("Product not found or unavailable from product-service for ID: {}. Status: {}", cartItem.getProductId(), productResponse.getStatusCode());
                    throw new ResourceNotFoundException("Product not found or unavailable from cart: " + cartItem.getProductId());
                }
                product = productResponse.getBody();
                log.info("Received product details for ID: {}. Name: {}, Stock: {}", product.getId(), product.getName(), product.getStock());

                // Synchronous call to product-service to decrement stock
                StockDecrementRequest decrementRequest = new StockDecrementRequest(cartItem.getProductId(), cartItem.getQuantity());
                log.info("Attempting to decrement stock for product ID: {} by quantity: {}", cartItem.getProductId(), cartItem.getQuantity());
                ResponseEntity<Void> decrementResponse = productClient.decrementStock(decrementRequest);

                if (decrementResponse.getStatusCode() != HttpStatus.OK) {
                    if (decrementResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        String errorMessage = "Insufficient stock for product ID: " + cartItem.getProductId() +
                                              ". Requested: " + cartItem.getQuantity() + ". Available stock might be less.";
                        log.error(errorMessage);
                        throw new InsufficientStockException(errorMessage);
                    } else {
                        String errorMessage = String.format("Failed to decrement stock for product ID: %s. Product service returned status: %s. Response body: %s",
                                cartItem.getProductId(), decrementResponse.getStatusCode(), decrementResponse.getBody());
                        log.error(errorMessage);
                        throw new RuntimeException(errorMessage); // This will rollback the transaction
                    }
                }
                log.info("Stock successfully decremented for product ID: {} by {}", cartItem.getProductId(), cartItem.getQuantity());

            } catch (ResourceNotFoundException | InsufficientStockException e) {
                throw e;
            } catch (Exception e) {
                log.error("Critical error during product service interaction for cart item {}: {}", cartItem.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Failed to process order from cart due to product service issue for product " + cartItem.getProductId() + ": " + e.getMessage(), e);
            }

            BigDecimal itemTotalPrice = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(cartItem.getPrice())
                    .productName(product.getName())
                    .imageUrl(product.getImageUrl())
                    .build();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .paymentInfo("Online Payment - Cart")
                .shippingAddress("Default Shipping Address for User " + userIdString)
                .orderItems(orderItems)
                .build();

        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(item -> item.setOrder(savedOrder)); // Ensure bidirectional link

        log.info("Order created from cart successfully with ID: {} for user ID: {}", savedOrder.getId(), userIdString);

        // Asynchronous/Fire-and-forget call to clear the user's cart after order creation
        // This operation is not critical for order creation transaction and can fail independently.
        try {
            log.info("Attempting to clear cart for user ID: {}", userIdString);
            shoppingCartClient.clearCartByUserId(userIdString);
            log.info("Cart cleared successfully for user ID: {}", userIdString);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user ID: {}. This might require manual cleanup or a dedicated retry mechanism. Error: {}", userIdString, e.getMessage());
            // This error doesn't roll back the order, but should be monitored.
        }

        return mapToOrderResponse(savedOrder);
    }


    @Override
    public OrderResponse getOrderById(Long id) throws ResourceNotFoundException {
        log.info("Fetching order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {}", id);
                    return new ResourceNotFoundException("Order not found with ID: " + id);
                });
        log.debug("Found order: {}", order.getOrderNumber());
        return mapToOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders.");
        List<Order> orders = orderRepository.findAll();
        log.info("Found {} total orders.", orders.size());
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        log.info("Fetching orders for user ID: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        log.info("Found {} orders for user ID: {}", orders.size(), userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> searchOrders(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Searching orders with filters - status: {}, startDate: {}, endDate: {}", status, startDate, endDate);

        Specification<Order> spec = OrderSpecifications.combineAnd(
                OrderSpecifications.withStatus(status),
                OrderSpecifications.withCreatedAtBetween(startDate, endDate)
        );

        List<Order> orders = orderRepository.findAll(spec);
        log.info("Found {} orders matching the criteria.", orders.size());
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, String newStatus) throws ResourceNotFoundException, IllegalArgumentException {
        log.info("Updating status for order ID: {} to {}", id, newStatus);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {} for status update.", id);
                    return new ResourceNotFoundException("Order not found with ID: " + id);
                });

        OrderStatus statusEnum;
        try {
            statusEnum = OrderStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status provided for order ID {}: {}. Valid statuses are: {}", id, newStatus,
                    java.util.Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.joining(", ")));
            throw new IllegalArgumentException("Invalid order status: " + newStatus + ". Valid statuses are: " +
                    java.util.Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.joining(", ")));
        }

        if (order.getStatus().equals(statusEnum)) {
            log.info("Order ID: {} status is already {}. No update needed.", id, newStatus);
            return mapToOrderResponse(order); // Return current state if no change
        }

        order.setStatus(statusEnum);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order ID: {} status updated to: {}", updatedOrder.getId(), updatedOrder.getStatus());
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    public void deleteOrder(Long id) throws ResourceNotFoundException {
        log.info("Attempting to delete order with ID: {}", id);
        if (!orderRepository.existsById(id)) {
            log.warn("Order not found with ID: {} for deletion.", id);
            throw new ResourceNotFoundException("Order not found with ID: " + id);
        }
        orderRepository.deleteById(id);
        log.info("Order ID: {} deleted successfully.", id);
    }

    // Helper method to map Order entity to OrderResponse DTO
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems() != null ?
                order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponse)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .paymentInfo(order.getPaymentInfo())
                .shippingAddress(order.getShippingAddress())
                .orderItems(itemResponses)
                .build();
    }

    // Helper method to map OrderItem entity to OrderItemResponse DTO
    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId())
                .quantity(orderItem.getQuantity())
                .priceAtPurchase(orderItem.getPriceAtPurchase())
                .productName(orderItem.getProductName())
                .imageUrl(orderItem.getImageUrl())
                .build();
    }
}