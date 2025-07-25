package com.intellimart.orderservice.service;

import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.dto.OrderResponseForProductService;
import com.intellimart.orderservice.dto.PaymentInitiationResponse;
import com.intellimart.orderservice.exception.InsufficientStockException;
import com.intellimart.orderservice.exception.ResourceNotFoundException;
import com.intellimart.orderservice.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
// Removed: import java.util.Map; // No longer needed for webhook payload in interface

public interface OrderService {
    OrderResponse placeOrder(OrderRequest orderRequest) throws InsufficientStockException, ResourceNotFoundException;
    OrderResponse createOrderFromCart(String userIdString) throws ResourceNotFoundException, InsufficientStockException;
    OrderResponse getOrderById(Long id) throws ResourceNotFoundException;
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByUserId(Long userId);
    OrderResponse updateOrderStatus(Long id, String newStatus) throws ResourceNotFoundException, IllegalArgumentException; // Added IllegalArgumentException back
    void deleteOrder(Long id) throws ResourceNotFoundException;
    
    // Method for Admin Filters
    List<OrderResponse> searchOrders(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    // Method for Payment Initiation
    PaymentInitiationResponse initiatePayment(Long orderId) throws ResourceNotFoundException, RuntimeException;

    // --- FIX HERE: Updated method signature to match OrderServiceImpl ---
    void handleRazorpayWebhook(String rawPayload, String razorpaySignature) throws RuntimeException;
    // --- END FIX ---
    
 // --- FIX HERE: Changed return type to List<OrderResponseForProductService> ---
    List<OrderResponseForProductService> findOrdersByProductId(Long productId);
    // --- END FIX ---

}