package com.intellimart.orderservice.service;

import com.intellimart.orderservice.client.ShoppingCartClient;
import com.intellimart.orderservice.client.ProductClient;
import com.intellimart.orderservice.dto.CartItemResponse;
import com.intellimart.orderservice.dto.CartResponse;
import com.intellimart.orderservice.dto.OrderItemRequest;
import com.intellimart.orderservice.dto.OrderItemResponse;
import com.intellimart.orderservice.dto.OrderPlacedEvent;
import com.intellimart.orderservice.dto.OrderItemEvent;
import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.dto.PaymentInitiationResponse;
import com.intellimart.orderservice.dto.ProductResponse;
import com.intellimart.orderservice.dto.StockDecrementRequest;
import com.intellimart.orderservice.dto.OrderResponseForProductService;
import com.intellimart.orderservice.dto.OrderItemResponseForProductService;
import com.intellimart.orderservice.exception.InsufficientStockException;
import com.intellimart.orderservice.exception.ResourceNotFoundException;
import com.intellimart.orderservice.model.Order;
import com.intellimart.orderservice.model.OrderItem;
import com.intellimart.orderservice.model.OrderStatus;
import com.intellimart.orderservice.repository.OrderItemRepository;
import com.intellimart.orderservice.repository.OrderRepository;
import com.intellimart.orderservice.util.OrderSpecifications;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final RazorpayClient razorpayClient;
    private final RabbitMQMessagePublisher messagePublisher;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.webhook-secret}")
    private String razorpayWebhookSecret;

    /**
     * Places a new order based on a direct OrderRequest.
     * Includes inventory deduction and detailed logging for distributed calls.
     */
    @Override
    public OrderResponse placeOrder(OrderRequest orderRequest) throws InsufficientStockException, ResourceNotFoundException {
        log.info("Attempting to place new order for user ID: {}", orderRequest.getUserId());

        Long userId = Long.parseLong(orderRequest.getUserId());

        List<OrderItem> orderItems = new java.util.ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            ProductResponse product;
            log.debug("Processing order item for product ID: {} with quantity: {}", itemRequest.getProductId(), itemRequest.getQuantity());

            try {
                log.info("Calling product-service to get details for product ID: {}", itemRequest.getProductId());
                // itemRequest.getProductId() is now Long, productClient.getProductById expects Long
                ResponseEntity<ProductResponse> productResponse = productClient.getProductById(itemRequest.getProductId());

                if (productResponse.getStatusCode() != HttpStatus.OK || productResponse.getBody() == null) {
                    log.error("Product not found or unavailable from product-service for ID: {}. Status: {}", itemRequest.getProductId(), productResponse.getStatusCode());
                    throw new ResourceNotFoundException("Product not found or unavailable: " + itemRequest.getProductId());
                }
                product = productResponse.getBody();
                log.info("Received product details for ID: {}. Name: {}, Stock: {}", product.getId(), product.getName(), product.getStock());

                if (product.getPrice().compareTo(itemRequest.getPriceAtPurchase()) != 0) {
                    log.warn("Price mismatch for product {}. Order item price: {}, Actual product price: {}. Using order item price for this order.",
                            itemRequest.getProductId(), itemRequest.getPriceAtPurchase(), product.getPrice());
                }

                // itemRequest.getProductId() is now Long, StockDecrementRequest expects Long
                StockDecrementRequest decrementRequest = new StockDecrementRequest(itemRequest.getProductId(), itemRequest.getQuantity());
                log.info("Attempting to decrement stock for product ID: {} by quantity: {}", itemRequest.getProductId(), itemRequest.getQuantity());
                // productClient.decrementStock expects Long, and itemRequest.getProductId() is Long
                ResponseEntity<Void> decrementResponse = productClient.decrementStock(decrementRequest);

                if (decrementResponse.getStatusCode() != HttpStatus.OK) {
                    if (decrementResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        String errorMessage = "Insufficient stock for product ID: " + itemRequest.getProductId() +
                                              ". Requested: " + itemRequest.getQuantity() + ". Available stock might be less.";
                        log.error(errorMessage);
                        throw new InsufficientStockException(errorMessage);
                    } else {
                        String errorMessage = String.format("Failed to decrement stock for product ID: %s. Product service returned status: %s. Response body: %s",
                                itemRequest.getProductId(), decrementResponse.getStatusCode(), decrementResponse.getBody());
                        log.error(errorMessage);
                        throw new RuntimeException(errorMessage);
                    }
                }
                log.info("Stock successfully decremented for product ID: {} by {}", itemRequest.getProductId(), itemRequest.getQuantity());

            } catch (ResourceNotFoundException | InsufficientStockException e) {
                throw e;
            } catch (Exception e) {
                log.error("Critical error during product service interaction for product ID {}: {}", itemRequest.getProductId(), e.getMessage(), e);
                throw new RuntimeException("Failed to process order due to product service issue for product " + itemRequest.getProductId() + ": " + e.getMessage(), e);
            }

            BigDecimal itemTotalPrice = itemRequest.getPriceAtPurchase().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .productId(itemRequest.getProductId()) // itemRequest.getProductId() is now Long, OrderItem.productId is Long
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
        orderItems.forEach(item -> item.setOrder(savedOrder));

        log.info("Order placed successfully with ID: {} and orderNumber: {}", savedOrder.getId(), savedOrder.getOrderNumber());

        // Publish Order Placed Event immediately after order creation (before payment might be confirmed)
        // This event signifies the order has been recorded in the system.
        publishOrderPlacedEvent(savedOrder);

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

        CartResponse cart;
        try {
            log.info("Calling shopping-cart-service to get cart for user ID: {}", userIdString);
            // shoppingCartClient.getCartByUserId expects String, so userIdString is correct
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
                log.info("Calling product-service to get details for product ID: {}", cartItem.getProductId());
                // cartItem.getProductId() is now Long, productClient.getProductById expects Long
                ResponseEntity<ProductResponse> productResponse = productClient.getProductById(cartItem.getProductId());

                if (productResponse.getStatusCode() != HttpStatus.OK || productResponse.getBody() == null) {
                    log.error("Product not found or unavailable from product-service for ID: {}. Status: {}", cartItem.getProductId(), productResponse.getStatusCode());
                    throw new ResourceNotFoundException("Product not found or unavailable from cart: " + cartItem.getProductId());
                }
                product = productResponse.getBody();
                log.info("Received product details for ID: {}. Name: {}, Stock: {}", product.getId(), product.getName(), product.getStock());

                // cartItem.getProductId() is now Long, StockDecrementRequest expects Long
                StockDecrementRequest decrementRequest = new StockDecrementRequest(cartItem.getProductId(), cartItem.getQuantity());
                log.info("Attempting to decrement stock for product ID: {} by quantity: {}", cartItem.getProductId(), cartItem.getQuantity());
                // productClient.decrementStock expects Long, and cartItem.getProductId() is Long
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
                        throw new RuntimeException(errorMessage);
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
                    .productId(cartItem.getProductId()) // cartItem.getProductId() is now Long, OrderItem.productId is Long
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
        orderItems.forEach(item -> item.setOrder(savedOrder));

        log.info("Order created from cart successfully with ID: {} for user ID: {}", savedOrder.getId(), userIdString);

        try {
            log.info("Attempting to clear cart for user ID: {}", userIdString);
            // shoppingCartClient.clearCartByUserId expects String, so convert Long productId to String
            shoppingCartClient.clearCartByUserId(String.valueOf(userId)); // CONVERTED userId to String
            log.info("Cart cleared successfully for user ID: {}", userIdString);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user ID: {}. This might require manual cleanup or a dedicated retry mechanism. Error: {}", userIdString, e.getMessage());
        }

        // Publish Order Placed Event immediately after order creation (before payment might be confirmed)
        publishOrderPlacedEvent(savedOrder);

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

    /**
     * Retrieves orders that contain a specific product ID.
     * This is intended for use by other services (e.g., Product Service for recommendations).
     *
     * @param productId The ID of the product to search for within order items.
     * @return A list of OrderResponseForProductService objects.
     */
    @Override
    public List<OrderResponseForProductService> findOrdersByProductId(Long productId) {
        log.info("Fetching orders containing product ID: {}", productId);

        // Find all OrderItems that contain the given productId
        // OrderItem.productId is now Long, so findByProductId(Long) is correct
        List<OrderItem> orderItems = orderItemRepository.findByProductId(productId);

        // Get distinct Orders from these OrderItems
        Set<Order> orders = orderItems.stream()
                .map(OrderItem::getOrder)
                .collect(Collectors.toSet());

        log.info("Found {} orders containing product ID: {}", orders.size(), productId);

        // Map the distinct Orders to OrderResponseForProductService DTOs
        return orders.stream()
                .map(this::mapToOrderResponseForProductService)
                .collect(Collectors.toList());
    }


    @Override
    @SuppressWarnings("unchecked") // Suppress warning for varargs parameter
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
            return mapToOrderResponse(order);
        }

        order.setStatus(statusEnum);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order ID: {} status updated to: {}", updatedOrder.getId(), updatedOrder.getStatus());

        // Publish Order Placed Event if status becomes PAID due to webhook
        if (updatedOrder.getStatus() == OrderStatus.PAID) {
             publishOrderPlacedEvent(updatedOrder);
        }

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

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(Long orderId) throws ResourceNotFoundException, RuntimeException {
        log.info("Initiating payment for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with ID: {} for payment initiation.", orderId);
                    return new ResourceNotFoundException("Order not found with ID: " + orderId);
                });

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            log.warn("Payment cannot be initiated for order ID {} with current status: {}", orderId, order.getStatus());
            throw new IllegalArgumentException("Payment cannot be initiated for order with status: " + order.getStatus());
        }

        try {
            long amountInPaise = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", order.getOrderNumber());
            orderRequest.put("payment_capture", 1);

            log.info("Creating Razorpay Order for internal order ID: {} with amount: {} paise", orderId, amountInPaise);
            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            log.info("Successfully created Razorpay Order. ID: {}", (Object) razorpayOrder.get("id"));

            order.setRazorpayOrderId(razorpayOrder.get("id"));
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            orderRepository.save(order);
            log.info("Internal order ID: {} updated with Razorpay Order ID: {}", order.getId(), order.getRazorpayOrderId());

            String userName = "Customer " + order.getUserId();
            String userEmail = "customer" + order.getUserId() + "@example.com";
            String userPhone = "9999999999";

            return PaymentInitiationResponse.builder()
                    .orderId(order.getId())
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .razorpayKeyId(razorpayKeyId)
                    .amountInPaise((int) amountInPaise)
                    .currency("INR")
                    .userName(userName)
                    .userEmail(userEmail)
                    .userPhone(userPhone)
                    .build();

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay Order for order ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay Order: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during payment initiation for order ID {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("An unexpected error occurred during payment initiation: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleRazorpayWebhook(String rawPayload, String razorpaySignature) throws RuntimeException {
        log.info("Received Razorpay Webhook. Raw Payload Length: {}, Signature: {}", rawPayload.length(), razorpaySignature);

        try {
            Utils.verifyWebhookSignature(rawPayload, razorpaySignature, razorpayWebhookSecret);
            log.info("Razorpay Webhook signature verified successfully.");

        } catch (RazorpayException e) {
            log.error("Razorpay Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Webhook signature verification failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred during webhook signature verification: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error during webhook verification: " + e.getMessage(), e);
        }

        JSONObject payloadJson = new JSONObject(rawPayload);
        String event = (String) payloadJson.get("event");
        Map<String, Object> entity = (Map<String, Object>) payloadJson.get("entity");

        if (entity == null) {
            log.error("Webhook payload 'entity' is null for event: {}", (Object) event);
            return;
        }

        String razorpayPaymentId = null;
        String razorpayOrderId = null;

        if ("payment.captured".equals(event) || "payment.failed".equals(event) || "payment.authorized".equals(event)) {
            razorpayPaymentId = (String) entity.get("id");
            razorpayOrderId = (String) entity.get("order_id");
            log.info("Processing payment event: {}. Payment ID: {}, Order ID: {}", (Object) event, (Object) razorpayPaymentId, (Object) razorpayOrderId);
        } else if ("order.paid".equals(event)) {
            razorpayOrderId = (String) entity.get("id");
            List<Map<String, Object>> payments = (List<Map<String, Object>>) entity.get("payments");
            if (payments != null && !payments.isEmpty()) {
                razorpayPaymentId = (String) payments.get(0).get("id");
            }
            log.info("Processing order event: {}. Order ID: {}, Payment ID: {}", (Object) event, (Object) razorpayPaymentId, (Object) razorpayPaymentId);
        } else {
            log.warn("Unhandled Razorpay webhook event type: {}", (Object) event);
            return;
        }

        if (razorpayOrderId == null) {
            log.error("Razorpay Order ID not found in webhook payload for event: {}", (Object) event);
            return;
        }

        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (order == null) {
            log.warn("Internal order not found for Razorpay Order ID: {}. This might be an old or invalid webhook.", (Object) razorpayOrderId);
            return;
        }

        OrderStatus originalStatus = order.getStatus();

        switch (event) {
            case "payment.captured":
            case "order.paid":
                order.setStatus(OrderStatus.PAID);
                order.setRazorpayPaymentId(razorpayPaymentId);
                log.info("Order ID: {} status updated to PAID. Razorpay Payment ID: {}", (Object) order.getId(), (Object) razorpayPaymentId);
                break;
            case "payment.failed":
                order.setStatus(OrderStatus.FAILED);
                order.setRazorpayPaymentId(razorpayPaymentId);
                log.warn("Order ID: {} status updated to FAILED. Razorpay Payment ID: {}", (Object) order.getId(), (Object) razorpayPaymentId);
                break;
            case "payment.authorized":
                order.setStatus(OrderStatus.AUTHORIZED);
                order.setRazorpayPaymentId(razorpayPaymentId);
                log.info("Order ID: {} status updated to AUTHORIZED. Razorpay Payment ID: {}", (Object) order.getId(), (Object) razorpayPaymentId);
                break;
            case "refund.processed":
                order.setStatus(OrderStatus.REFUNDED);
                log.info("Order ID: {} status updated to REFUNDED.", (Object) order.getId());
                break;
            default:
                log.info("Webhook event {} received for Order ID {} but no status update applied.", (Object) event, (Object) order.getId());
                return;
        }

        Order updatedOrder = orderRepository.save(order);

        // Publish Order Placed Event if status becomes PAID due to webhook
        if (originalStatus != OrderStatus.PAID && updatedOrder.getStatus() == OrderStatus.PAID) {
             publishOrderPlacedEvent(updatedOrder);
        }

        log.info("Order ID: {} successfully processed webhook event: {}", (Object) order.getId(), (Object) event);
    }

    /**
     * Helper method to create and publish an OrderPlacedEvent from an Order entity.
     * @param order The Order entity from which to create the event.
     */
    private void publishOrderPlacedEvent(Order order) {
        List<OrderItemEvent> itemEvents = order.getOrderItems().stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId()) // REMOVED Long.valueOf()
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .priceAtPurchase(item.getPriceAtPurchase())
                        .imageUrl(item.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        OrderPlacedEvent orderPlacedEvent = OrderPlacedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .paymentInfo(order.getPaymentInfo())
                .razorpayOrderId(order.getRazorpayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemEvents)
                .build();

        messagePublisher.publishOrderPlacedEvent(orderPlacedEvent);
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
                .razorpayOrderId(order.getRazorpayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .build();
    }

    // Helper method to map OrderItem entity to OrderItemResponse DTO
    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId()) // orderItem.getProductId() is now Long, OrderItemResponse.productId should be Long
                .quantity(orderItem.getQuantity())
                .priceAtPurchase(orderItem.getPriceAtPurchase())
                .productName(orderItem.getProductName())
                .imageUrl(orderItem.getImageUrl())
                .build();
    }

    // NEW Helper method to map Order entity to OrderResponseForProductService DTO
    private OrderResponseForProductService mapToOrderResponseForProductService(Order order) {
        List<OrderItemResponseForProductService> itemResponses = order.getOrderItems() != null ?
                order.getOrderItems().stream()
                        .map(this::mapToOrderItemResponseForProductService)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return OrderResponseForProductService.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name()) // Convert enum to String
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .orderItems(itemResponses)
                .build();
    }

    // NEW Helper method to map OrderItem entity to OrderItemResponseForProductService DTO
    private OrderItemResponseForProductService mapToOrderItemResponseForProductService(OrderItem orderItem) {
        return OrderItemResponseForProductService.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProductId()) // REMOVED Long.valueOf()
                .productName(orderItem.getProductName()) // Include product name
                .quantity(orderItem.getQuantity())
                .priceAtPurchase(orderItem.getPriceAtPurchase())
                .imageUrl(orderItem.getImageUrl()) // Include image URL
                .build();
    }
}
