package com.intellimart.cartservice.controller;

import com.intellimart.cartservice.dto.AddToCartRequest;
import com.intellimart.cartservice.entity.Cart;
import com.intellimart.cartservice.service.ShoppingCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addItemToCart(
            @PathVariable String userId,
            @Valid @RequestBody AddToCartRequest request) {
        try {
            Cart updatedCart = shoppingCartService.addItemToCart(userId, request);
            return new ResponseEntity<>(updatedCart, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable String userId) {
        // --- MODIFIED LINE ---
        return shoppingCartService.getCartByUserId(userId) // Call the new method on the service
                .map(cart -> new ResponseEntity<>(cart, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}