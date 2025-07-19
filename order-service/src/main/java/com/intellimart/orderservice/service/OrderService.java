package com.intellimart.orderservice.service;

import com.intellimart.orderservice.dto.OrderRequest;
import com.intellimart.orderservice.dto.OrderResponse;
import com.intellimart.orderservice.exception.InsufficientStockException; // Import new exception
import com.intellimart.orderservice.exception.ResourceNotFoundException;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest orderRequest) throws InsufficientStockException; // Add exception
    OrderResponse createOrderFromCart(String userId) throws ResourceNotFoundException, InsufficientStockException; // Add exception
    OrderResponse getOrderById(Long id) throws ResourceNotFoundException;
    List<OrderResponse> getAllOrders();
    List<OrderResponse> getOrdersByUserId(String userId);
    OrderResponse updateOrderStatus(Long id, String newStatus) throws ResourceNotFoundException;
    void deleteOrder(Long id) throws ResourceNotFoundException;
}