package com.intellimart.orderservice.service;

import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.exception.InsufficientStockException; // Ensure this is imported if you're throwing it
import com.intellimart.orderservice.exception.ResourceNotFoundException;
// import com.intellimart.orderservice.exception.OrderProcessingException; // Only if declared in interface

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest orderRequest) throws InsufficientStockException, ResourceNotFoundException;
    OrderResponse createOrderFromCart(String userIdString) throws ResourceNotFoundException, InsufficientStockException;
    OrderResponse getOrderById(Long id) throws ResourceNotFoundException;
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByUserId(Long userId); // <--- CHANGED FROM String TO Long
    OrderResponse updateOrderStatus(Long id, String newStatus) throws ResourceNotFoundException;
    void deleteOrder(Long id) throws ResourceNotFoundException;
}