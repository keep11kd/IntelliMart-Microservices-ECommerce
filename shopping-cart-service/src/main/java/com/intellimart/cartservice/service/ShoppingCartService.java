package com.intellimart.cartservice.service;

import com.intellimart.cartservice.client.ProductServiceClient;
import com.intellimart.cartservice.dto.AddToCartRequest;
import com.intellimart.cartservice.dto.CartItemViewResponse;
import com.intellimart.cartservice.dto.CartViewResponse;
import com.intellimart.cartservice.dto.ProductResponse;
import com.intellimart.cartservice.entity.Cart;
import com.intellimart.cartservice.entity.CartItem;
import com.intellimart.cartservice.exception.ProductServiceCommunicationException; // <--- NEW IMPORT
import com.intellimart.cartservice.exception.ResourceNotFoundException;     // <--- NEW IMPORT
import com.intellimart.cartservice.repository.CartItemRepository;
import com.intellimart.cartservice.repository.CartRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingCartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductServiceClient productServiceClient;

    @Transactional
    public Cart addItemToCart(String userId, AddToCartRequest request) {
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
            ProductResponse productDetails;
            try {
                productDetails = productServiceClient.getProductById(request.getProductId());
                if (productDetails == null) {
                    throw new ProductServiceCommunicationException("Product not found with ID: " + request.getProductId() + " in product-service.");
                }
                log.debug("Fetched product details: {}", productDetails);
            } catch (Exception e) {
                log.error("Error fetching product details for ID {}: {}", request.getProductId(), e.getMessage());
                throw new ProductServiceCommunicationException("Failed to retrieve product details for ID: " + request.getProductId(), e);
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

    @Transactional
    public Cart updateCartItemQuantity(String userId, Long productId, Integer newQuantity) {
        log.info("Updating quantity for product {} to {} in cart for user {}", productId, newQuantity, userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        Optional<CartItem> existingCartItemOptional = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingCartItemOptional.isEmpty()) {
            throw new ResourceNotFoundException("Product " + productId + " not found in cart for user: " + userId);
        }

        CartItem existingItem = existingCartItemOptional.get();

        if (newQuantity <= 0) {
            log.info("Removing product {} from cart for user {} as quantity is {}", productId, userId, newQuantity);
            cart.removeCartItem(existingItem);
        } else {
            existingItem.setQuantity(newQuantity);
            log.info("Product {} quantity updated to {} for user {}", productId, newQuantity, userId);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void removeCartItem(String userId, Long productId) {
        log.info("Attempting to remove product {} from cart for user {}", productId, userId);
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));

        Optional<CartItem> itemToRemoveOptional = cart.getCartItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (itemToRemoveOptional.isEmpty()) {
            throw new ResourceNotFoundException("Product " + productId + " not found in cart for user: " + userId);
        }

        CartItem itemToRemove = itemToRemoveOptional.get();
        cart.removeCartItem(itemToRemove);

        cartRepository.save(cart);
        log.info("Product {} successfully removed from cart for user {}", productId, userId);
    }

    public CartViewResponse getComprehensiveCartView(String userId) {
        log.info("Fetching comprehensive cart view for user ID: {}", userId);
        Optional<Cart> cartOptional = cartRepository.findByUserId(userId);

        if (cartOptional.isEmpty()) {
            log.info("Cart not found for user ID: {}. Returning empty cart view.", userId);
            return CartViewResponse.builder()
                    .userId(userId)
                    .items(Collections.emptyList())
                    .totalPrice(BigDecimal.ZERO)
                    .totalItems(0)
                    .totalQuantity(0)
                    .build();
        }

        Cart cart = cartOptional.get();
        BigDecimal totalCartPrice = BigDecimal.ZERO;
        int totalCartQuantity = 0;

        List<CartItemViewResponse> cartItemViews = cart.getCartItems().stream()
            .map(cartItem -> {
                ProductResponse productDetails = null;
                try {
                    productDetails = productServiceClient.getProductById(cartItem.getProductId());
                    if (productDetails == null) {
                        // If product service returns null for a product that should exist
                        log.warn("Product ID {} not found in Product Service during cart view. Using denormalized data.", cartItem.getProductId());
                        throw new ProductServiceCommunicationException("Product " + cartItem.getProductId() + " not found in Product Service.");
                    }
                } catch (Exception e) { // Catch FeignClientException or any other comm error
                    log.warn("Could not fetch latest details for product ID {}. Using denormalized data. Error: {}",
                              cartItem.getProductId(), e.getMessage());
                    // Fallback: Use denormalized data if product-service is unavailable or product not found
                    productDetails = ProductResponse.builder()
                            .id(cartItem.getProductId())
                            .name(cartItem.getProductName() + " (Info Unavailable)") // Indicate problem
                            .price(cartItem.getPrice()) // Use denormalized price
                            .imageUrl(cartItem.getImageUrl())
                            .build();
                }

                BigDecimal currentProductPrice = (productDetails != null && productDetails.getPrice() != null)
                                                ? productDetails.getPrice()
                                                : (cartItem.getPrice() != null ? cartItem.getPrice() : BigDecimal.ZERO);

                BigDecimal lineItemTotal = currentProductPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

					/*
					 * // Accumulate total cart price totalCartPrice.add(lineItemTotal); // Use
					 * .add() to update BigDecimal totalCartQuantity += cartItem.getQuantity();
					 */
                return CartItemViewResponse.builder()
                        .productId(cartItem.getProductId())
                        .quantity(cartItem.getQuantity())
                        .productName(productDetails != null ? productDetails.getName() : cartItem.getProductName())
                        .currentPrice(currentProductPrice)
                        .imageUrl(productDetails != null ? productDetails.getImageUrl() : cartItem.getImageUrl())
                        .lineItemTotal(lineItemTotal)
                        .build();
            })
            .collect(Collectors.toList());

        // Need to ensure totalCartPrice is updated outside the stream if the lambda is side-effecting totalCartPrice directly.
        // A more functional approach would be to collect into a DTO and then sum.
        // For simplicity and to match the current pattern, we'll re-sum or make totalCartPrice final with a new accumulator.
        // Let's refine the total price calculation to be robust after the stream.
        BigDecimal finalTotalPrice = cartItemViews.stream()
                                        .map(CartItemViewResponse::getLineItemTotal)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        int finalTotalQuantity = cartItemViews.stream()
                                        .mapToInt(CartItemViewResponse::getQuantity)
                                        .sum();

        return CartViewResponse.builder()
                .userId(userId)
                .items(cartItemViews)
                .totalPrice(finalTotalPrice)
                .totalItems(cartItemViews.size())
                .totalQuantity(finalTotalQuantity)
                .build();
    }
}