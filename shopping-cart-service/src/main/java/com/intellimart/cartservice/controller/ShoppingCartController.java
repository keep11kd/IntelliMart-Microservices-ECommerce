package com.intellimart.cartservice.controller;

import com.intellimart.cartservice.dto.AddToCartRequest;
import com.intellimart.cartservice.dto.CartViewResponse;
import com.intellimart.cartservice.dto.ErrorResponse; // <--- NEW IMPORT for Swagger @ApiResponse
import com.intellimart.cartservice.dto.UpdateCartItemQuantityRequest;
import com.intellimart.cartservice.entity.Cart; // Still needed if addItemToCart returns Cart directly
import com.intellimart.cartservice.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;      // <--- NEW IMPORT
import io.swagger.v3.oas.annotations.Parameter;      // <--- NEW IMPORT
import io.swagger.v3.oas.annotations.media.Content;  // <--- NEW IMPORT
import io.swagger.v3.oas.annotations.media.Schema;   // <--- NEW IMPORT
import io.swagger.v3.oas.annotations.responses.ApiResponse; // <--- NEW IMPORT
import io.swagger.v3.oas.annotations.tags.Tag;       // <--- NEW IMPORT
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Shopping Cart Management", description = "APIs for managing user shopping carts in IntelliMart") // <--- New Tag
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @Operation(
        summary = "Add item to cart or update quantity",
        description = "Adds a new product to the specified user's cart or increments its quantity if already present. " +
                      "Product details (name, price, image) are denormalized at the time of addition.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Item added/quantity updated successfully",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or quantity",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Product service unavailable or product not found during addition",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addItemToCart(
            @Parameter(description = "ID of the user whose cart is being modified", required = true, example = "user123")
            @PathVariable String userId,
            @Parameter(description = "Details of the product to add/update", required = true)
            @Valid @RequestBody AddToCartRequest request) {
        Cart updatedCart = shoppingCartService.addItemToCart(userId, request);
        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    @Operation(
        summary = "Get user's comprehensive cart view",
        description = "Retrieves the user's shopping cart with up-to-date product details (name, current price, image) " +
                      "fetched from the product-service, along with calculated total price and quantities.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully (even if empty)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = CartViewResponse.class)))
            // No 404 for cart not found as service now returns an empty CartViewResponse
        }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<CartViewResponse> getCartByUserId(
            @Parameter(description = "ID of the user whose cart is to be retrieved", required = true, example = "user123")
            @PathVariable String userId) {
        CartViewResponse cartView = shoppingCartService.getComprehensiveCartView(userId);
        return new ResponseEntity<>(cartView, HttpStatus.OK);
    }

    @Operation(
        summary = "Update quantity of an item in cart",
        description = "Updates the quantity of a specific product in the user's cart. " +
                      "Setting quantity to 0 or less will remove the item from the cart.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully (or item removed)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Cart.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or quantity",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cart or product item not found in cart",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<Cart> updateCartItemQuantity(
            @Parameter(description = "ID of the user", required = true, example = "user123")
            @PathVariable String userId,
            @Parameter(description = "ID of the product to update", required = true, example = "1")
            @PathVariable Long productId,
            @Parameter(description = "New quantity for the item", required = true)
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        Cart updatedCart = shoppingCartService.updateCartItemQuantity(userId, productId, request.getQuantity());
        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    @Operation(
        summary = "Remove item from cart",
        description = "Completely removes a specific product from the user's cart.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Item removed successfully (No Content)"),
            @ApiResponse(responseCode = "404", description = "Cart or product item not found in cart",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @DeleteMapping("/{userId}/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Ensures 204 is returned on success
    public ResponseEntity<Void> removeCartItem(
            @Parameter(description = "ID of the user", required = true, example = "user123")
            @PathVariable String userId,
            @Parameter(description = "ID of the product to remove", required = true, example = "1")
            @PathVariable Long productId) {
        shoppingCartService.removeCartItem(userId, productId);
        return ResponseEntity.noContent().build();
    }
}