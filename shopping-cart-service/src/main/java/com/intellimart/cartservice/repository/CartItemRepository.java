package com.intellimart.cartservice.repository;

import com.intellimart.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Custom method to find a cart item by cart ID and product ID
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
}