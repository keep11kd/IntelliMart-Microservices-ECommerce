package com.intellimart.cartservice.repository;

import com.intellimart.cartservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Custom method to find a cart by userId
    Optional<Cart> findByUserId(String userId);
}