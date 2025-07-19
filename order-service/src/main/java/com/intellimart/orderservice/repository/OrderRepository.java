package com.intellimart.orderservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.intellimart.orderservice.model.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find orders by userId
    List<Order> findByUserId(String userId);

    // Find order by orderNumber (if you implement a unique order number generation)
    Optional<Order> findByOrderNumber(String orderNumber);
}