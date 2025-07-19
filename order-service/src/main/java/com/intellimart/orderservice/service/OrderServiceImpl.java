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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Make sure this is imported if used
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShoppingCartClient shoppingCartClient;
    private final ProductClient productClient;

    /**
     * Places a new order based on a direct OrderRequest (e.g., from an admin panel or direct API call).
     * This method now includes inventory deduction.
     */
    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) throws InsufficientStockException, ResourceNotFoundException {
        log.info("Placing new order (direct request) for user ID: {}", orderRequest.getUserId());

        // Convert userId from String to Long for internal use
        Long userId = Long.parseLong(orderRequest.getUserId());

        // 1. Validate and Decrement Stock for each item
        List<OrderItem> orderItems = new java.util.ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            ProductResponse product;

            try {
                // Call product-service to get product details and decrement stock
                ResponseEntity<ProductResponse> productResponse = productClient.getProductById(itemRequest.getProductId());

                if (productResponse.getStatusCode() != HttpStatus.OK || productResponse.getBody() == null) {
                    log.error("Product not found or unavailable: {}", itemRequest.getProductId());
                    throw new ResourceNotFoundException("Product not found or unavailable: " + itemRequest.getProductId());
                }
                product = productResponse.getBody();

                // Check if product price matches priceAtPurchase (optional, but good for consistency)
                if (product.getPrice().compareTo(itemRequest.getPriceAtPurchase()) != 0) {
                    log.warn("Price mismatch for product {}. Cart price: {}, Actual price: {}",
                            itemRequest.getProductId(), itemRequest.getPriceAtPurchase(), product.getPrice());
                }

                // Attempt to decrement stock
                StockDecrementRequest decrementRequest = new StockDecrementRequest(itemRequest.getProductId(), itemRequest.getQuantity());
                ResponseEntity<Void> decrementResponse = productClient.decrementStock(decrementRequest);

                if (decrementResponse.getStatusCode() != HttpStatus.OK) {
                    // If product-service returns 400 BAD_REQUEST for insufficient stock
                    if (decrementResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        String errorMessage = "Insufficient stock for product ID: " + itemRequest.getProductId() +
                                              ". Requested: " + itemRequest.getQuantity() + ", Available: " + product.getStock();
                        log.error(errorMessage);
                        throw new InsufficientStockException(errorMessage);
                    } else {
                        // Other errors from product-service (e.g., 500, 503 from fallback)
                        String errorMessage = "Failed to decrement stock for product ID: " + itemRequest.getProductId() +
                                              ". Product service returned status: " + decrementResponse.getStatusCode();
                        log.error(errorMessage);
                        throw new RuntimeException(errorMessage); // This will rollback the transaction
                    }
                }
            } catch (ResourceNotFoundException | InsufficientStockException e) {
                throw e; // Re-throw specific, known exceptions
            } catch (Exception e) {
                log.error("Error communicating with product service for product ID {}: {}", itemRequest.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Failed to process order due to product service issue for product " + itemRequest.getProductId(), e); // Catch generic errors
            }

            // Stock successfully decremented, now build OrderItem
            BigDecimal itemTotalPrice = itemRequest.getPriceAtPurchase().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(itemRequest.getPriceAtPurchase())
                    .productName(product.getName()) // Enrich with actual product name
                    .imageUrl(product.getImageUrl()) // Enrich with actual image URL
                    .build();
            orderItems.add(orderItem);
        }

        // 2. Create and Save Order
        Order order = Order.builder()
                .userId(userId) // Use the converted Long userId
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .paymentInfo(orderRequest.getPaymentInfo() != null ? orderRequest.getPaymentInfo() : "N/A - Direct Order")
                .shippingAddress(orderRequest.getShippingAddress() != null ? orderRequest.getShippingAddress() : "N/A - Direct Order")
                .orderItems(orderItems)
                .build();

        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(item -> item.setOrder(savedOrder)); // Ensure bidirectional link is established

        log.info("Order placed successfully with ID: {}", savedOrder.getId());
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Creates an order by fetching cart contents from the shopping-cart-service.
     * This method now includes inventory deduction.
     *
     * @param userIdString The ID of the user whose cart should be converted to an order (String type for API input).
     * @return OrderResponse representing the newly created order.
     * @throws ResourceNotFoundException if the cart is empty or user not found.
     * @throws InsufficientStockException if stock is insufficient for any item.
     */
    @Override
    public OrderResponse createOrderFromCart(String userIdString) throws ResourceNotFoundException, InsufficientStockException {
        log.info("Attempting to create order from cart for user ID: {}", userIdString);

        // Convert userId from String to Long for internal use and consistency
        Long userId = Long.parseLong(userIdString);

        // 1. Fetch cart contents from shopping-cart-service
        CartResponse cart = shoppingCartClient.getCartByUserId(userIdString);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            log.warn("Cart is empty (or could not be retrieved) for user ID: {}", userIdString);
            throw new ResourceNotFoundException("Cannot create order: Cart is empty for user ID " + userIdString);
        }

        // 2. Validate and Decrement Stock for each item in the cart
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();

        for (CartItemResponse cartItem : cart.getItems()) {
            ProductResponse product;

            try {
                // Call product-service to get product details (for enrichment) and decrement stock
                ResponseEntity<ProductResponse> productResponse = productClient.getProductById(cartItem.getProductId());

                if (productResponse.getStatusCode() != HttpStatus.OK || productResponse.getBody() == null) {
                    log.error("Product not found or unavailable from cart: {}", cartItem.getProductId());
                    throw new ResourceNotFoundException("Product not found or unavailable from cart: " + cartItem.getProductId());
                }
                product = productResponse.getBody();

                // Attempt to decrement stock
                StockDecrementRequest decrementRequest = new StockDecrementRequest(cartItem.getProductId(), cartItem.getQuantity());
                ResponseEntity<Void> decrementResponse = productClient.decrementStock(decrementRequest);

                if (decrementResponse.getStatusCode() != HttpStatus.OK) {
                    if (decrementResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        String errorMessage = "Insufficient stock for product ID: " + cartItem.getProductId() +
                                              ". Requested: " + cartItem.getQuantity() + ", Available: " + product.getStock();
                        log.error(errorMessage);
                        throw new InsufficientStockException(errorMessage);
                    } else {
                        String errorMessage = "Failed to decrement stock for product ID: " + cartItem.getProductId() +
                                              ". Product service returned status: " + decrementResponse.getStatusCode();
                        log.error(errorMessage);
                        throw new RuntimeException(errorMessage);
                    }
                }
            } catch (ResourceNotFoundException | InsufficientStockException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error communicating with product service for cart item {}: {}", cartItem.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Failed to process order from cart due to product service issue for product " + cartItem.getProductId(), e);
            }

            // Stock successfully decremented, now build OrderItem
            BigDecimal itemTotalPrice = cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(cartItem.getPrice())
                    .productName(product.getName()) // Enrich with actual product name
                    .imageUrl(product.getImageUrl()) // Enrich with actual image URL
                    .build();
            orderItems.add(orderItem);
        }

        // 3. Create and Save Order
        Order order = Order.builder()
                .userId(userId) // Use the converted Long userId
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .paymentInfo("Online Payment - Cart")
                .shippingAddress("Default Shipping Address for User " + userIdString) // Keep string for address if preferred
                .orderItems(orderItems)
                .build();

        Order savedOrder = orderRepository.save(order);
        orderItems.forEach(item -> item.setOrder(savedOrder)); // Ensure bidirectional link

        log.info("Order created from cart successfully with ID: {} for user ID: {}", savedOrder.getId(), userIdString);

        // 4. Clear the user's cart after order creation
        try {
            shoppingCartClient.clearCartByUserId(userIdString);
            log.info("Cart cleared successfully for user ID: {}", userIdString);
        } catch (Exception e) {
            log.error("Failed to clear cart for user ID: {}. Error: {}", userIdString, e.getMessage());
            // This error doesn't roll back the order, but should be monitored.
        }

        return mapToOrderResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(Long id) throws ResourceNotFoundException {
        log.info("Fetching order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return mapToOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders.");
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) { // <--- FIXED: Changed from String to Long
        log.info("Fetching orders for user ID: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse updateOrderStatus(Long id, String newStatus) throws ResourceNotFoundException {
        log.info("Updating status for order ID: {} to {}", id, newStatus);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));

        try {
            order.setStatus(OrderStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + newStatus + ". Valid statuses are: " +
                    java.util.Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.joining(", ")));
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order ID: {} status updated to: {}", updatedOrder.getId(), updatedOrder.getStatus());
        return mapToOrderResponse(updatedOrder);
    }

    @Override
    public void deleteOrder(Long id) throws ResourceNotFoundException {
        log.info("Attempting to delete order with ID: {}", id);
        if (!orderRepository.existsById(id)) {
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