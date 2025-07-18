package com.intellimart.cartservice.service;

import com.intellimart.cartservice.client.ProductServiceClient;
import com.intellimart.cartservice.dto.AddToCartRequest;
import com.intellimart.cartservice.dto.ProductResponse;
import com.intellimart.cartservice.entity.Cart;
import com.intellimart.cartservice.entity.CartItem;
import com.intellimart.cartservice.repository.CartItemRepository;
import com.intellimart.cartservice.repository.CartRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public Cart addItemToCart(String userId, AddToCartRequest request) {
        // ... (existing code for addItemToCart remains the same) ...

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new cart for user: {}", userId);
                    return Cart.builder().userId(userId).build();
                });

        Optional<CartItem> existingCartItemOptional = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingCartItemOptional.isPresent()) {
            CartItem existingItem = existingCartItemOptional.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            log.info("Updated quantity for product {} in cart for user {}", request.getProductId(), userId);
        } else {
            log.info("Adding new product {} to cart for user {}", request.getProductId(), userId);
            ProductResponse productDetails = null;
            try {
                productDetails = productServiceClient.getProductById(request.getProductId());
                if (productDetails == null) {
                    throw new RuntimeException("Product not found with ID: " + request.getProductId());
                }
                log.debug("Fetched product details: {}", productDetails);
            } catch (Exception e) {
                log.error("Error fetching product details for ID {}: {}", request.getProductId(), e.getMessage());
                throw new RuntimeException("Failed to add item to cart: Product details could not be retrieved. " + e.getMessage());
            }

            CartItem newCartItem = CartItem.builder()
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .price(productDetails.getPrice())
                    .productName(productDetails.getName())
                    .imageUrl(productDetails.getImageUrl())
                    .build();

            cart.addCartItem(newCartItem);
        }

        return cartRepository.save(cart);
    }

    // --- NEW METHOD ---
    public Optional<Cart> getCartByUserId(String userId) {
        log.info("Fetching cart for user ID: {}", userId);
        // It's good practice to log or handle if the cart is not found here,
        // or let the Optional handle it in the controller.
        return cartRepository.findByUserId(userId);
    }
}