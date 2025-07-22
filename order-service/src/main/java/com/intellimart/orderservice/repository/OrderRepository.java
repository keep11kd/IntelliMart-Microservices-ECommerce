package com.intellimart.orderservice.repository;

import com.intellimart.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // <--- NEW IMPORT
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> { // <--- EXTEND JpaSpecificationExecutor
    List<Order> findByUserId(Long userId);
    
    // Find order by orderNumber (if you implement a unique order number generation)
    Optional<Order> findByOrderNumber(String orderNumber);
    
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId); // <--- NEW METHOD
}