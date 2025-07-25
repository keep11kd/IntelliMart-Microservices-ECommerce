package com.intellimart.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimart.productservice.dto.ErrorResponse;
import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.dto.RecommendationResponse; // NEW IMPORT: For recommendation endpoint
import com.intellimart.productservice.dto.StockDecrementRequest;
import com.intellimart.productservice.exception.InsufficientStockException;
import com.intellimart.productservice.exception.ResourceNotFoundException;
import com.intellimart.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products in IntelliMart")
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "Create a new product",
        description = "Creates a new product with details and an optional image. Requires ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can create products"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - issue with image storage or unexpected error")
        }
    )
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product data in JSON format", required = true)
            @RequestPart("product") @Valid String productJson,
            @Parameter(description = "Optional image file for the product")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        log.info("Received request to create product with JSON: {} and image file: {}", productJson, (imageFile != null ? imageFile.getOriginalFilename() : "No file"));
        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
        try {
            ProductResponse newProduct = productService.createProduct(productRequest, imageFile);
            log.info("Product created with ID: {}", newProduct.getId());
            return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to create product: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error creating product: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Get all products",
        description = "Retrieves a list of all products. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class, type = "array")))
        }
    )
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("Received request to get all products.");
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @Operation(
        summary = "Get all products with pagination",
        description = "Retrieves a paginated list of all products. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class, type = "array", name = "ProductResponsePage")))
        }
    )
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductResponse>> getAllProductsPaginated(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., 'name', 'price', 'id')", example = "id") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction ('asc' or 'desc')", example = "asc") @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Received request to get all products paginated: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        Page<ProductResponse> products = productService.getAllProductsPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }

    @Operation(
        summary = "Get filtered products with pagination",
        description = "Retrieves products based on optional filters (category, price range, search query) with pagination and sorting. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered and paginated list",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class, type = "array", name = "ProductResponsePage")))
        }
    )
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponse>> getFilteredProducts(
            @Parameter(description = "Optional: ID of the category to filter by", example = "1")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Optional: Minimum price (inclusive)", example = "50.00")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Optional: Maximum price (inclusive)", example = "500.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Optional: Search term for product name or description", example = "laptop")
            @RequestParam(required = false) String query,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., 'name', 'price', 'id')", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction ('asc' or 'desc')", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Received request to get filtered products: categoryId={}, minPrice={}, maxPrice={}, query={}, page={}, size={}, sortBy={}, sortDir={}",
                categoryId, minPrice, maxPrice, query, page, size, sortBy, sortDir);
        Page<ProductResponse> products = productService.getFilteredProducts(
                query, query, categoryId, minPrice, maxPrice,
                page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }

    @Operation(
        summary = "Get product by ID",
        description = "Retrieves a single product by its ID. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Product found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "ID of the product to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Received request to get product by ID: {}", id);
        try {
            ProductResponse product = productService.getProductById(id);
            return ResponseEntity.ok(product);
        } catch (ResourceNotFoundException e) {
            log.warn("Product not found for ID: {}. {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(
        summary = "Update an existing product",
        description = "Updates an existing product's details and/or image. Requires ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can update products"),
            @ApiResponse(responseCode = "404", description = "Product or Category not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - issue with image storage or unexpected error")
        }
    )
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "ID of the product to update", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated product data in JSON format", required = true)
            @RequestPart("product") @Valid String productJson,
            @Parameter(description = "Optional new image file for the product")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        log.info("Received request to update product ID: {}", id);
        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
        try {
            ProductResponse updatedProduct = productService.updateProduct(id, productRequest, imageFile);
            log.info("Product ID: {} updated successfully.", updatedProduct.getId());
            return ResponseEntity.ok(updatedProduct);
        } catch (ResourceNotFoundException e) {
            log.warn("Product or Category not found for ID: {}. {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error updating product ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Delete a product",
        description = "Deletes a product by its ID. Requires ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can delete products"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID of the product to delete", required = true, example = "1")
            @PathVariable Long id) {
        log.info("Received request to delete product ID: {}", id);
        try {
            productService.deleteProduct(id);
            log.info("Product ID: {} deleted successfully.", id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Product not found for deletion: {}. {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting product ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Decrement product stock",
        description = "Decrements the stock of a product by a specified quantity. Primarily used by Order Service. Requires INTERNAL or ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Stock decremented successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or insufficient stock",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires INTERNAL or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @PostMapping("/decrement-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    public ResponseEntity<Void> decrementStock(
            @Parameter(description = "Product ID and quantity to decrement", required = true)
            @RequestBody @Valid StockDecrementRequest request) {
        log.info("Received request to decrement stock for product ID: {} by quantity: {}", request.getProductId(), request.getQuantity());
        try {
            productService.decrementStock(request);
            log.info("Stock decremented successfully for product ID: {}", request.getProductId());
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Stock decrement failed: Product not found for ID {}. {}", request.getProductId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (InsufficientStockException e) {
            log.warn("Stock decrement failed: Insufficient stock for product ID {}. {}", request.getProductId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Error decrementing stock for product ID {}: {}", request.getProductId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Increment product stock",
        description = "Increments the stock of a product by a specified quantity. Requires INTERNAL or ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Stock incremented successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g., quantity less than 1)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires INTERNAL or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @PostMapping("/increment-stock/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    public ResponseEntity<Void> incrementStock(
            @Parameter(description = "ID of the product to increment stock for", required = true, example = "1")
            @PathVariable Long productId,
            @Parameter(description = "Quantity to increment by (must be at least 1)", required = true, example = "5")
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
        log.info("Received request to increment stock for product ID: {} by quantity: {}", productId, quantity);
        try {
            productService.incrementStock(productId, quantity);
            log.info("Stock incremented successfully for product ID: {}", productId);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Stock increment failed: Product not found for ID {}. {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error incrementing stock for product ID {}: {}", productId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
            summary = "Get product recommendations",
            description = "Retrieves a list of recommended products based on the given product. Accessible to all roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved recommendations",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RecommendationResponse.class, type = "array"))),
                    @ApiResponse(responseCode = "404", description = "Product not found with the given ID (for which recommendations are requested)",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - unexpected issue during recommendation generation or communication with order service",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/{productId}/recommendations")
    public ResponseEntity<List<RecommendationResponse>> getProductRecommendations(
            @Parameter(description = "ID of the product for which to get recommendations", required = true, example = "1")
            @PathVariable Long productId) {
        log.info("Received request for recommendations for product ID: {}", productId);
        List<RecommendationResponse> recommendations = productService.getProductRecommendations(productId);
        log.info("Generated {} recommendations for product ID: {}", recommendations.size(), productId);
        return ResponseEntity.ok(recommendations);
    }

    // Existing search endpoint (can be covered by /filter)
    @Operation(
        summary = "Search products by name or description (legacy)",
        description = "Retrieves a list of products matching the query in name or description. Publicly accessible. Consider using /api/products/filter for more options.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class, type = "array")))
        }
    )
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Search term for product name or description", required = true, example = "phone")
            @RequestParam String query) {
        log.info("Received request to search products with query: {}", query);
        List<ProductResponse> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    // Existing category-specific endpoint (can be covered by /filter)
    @Operation(
        summary = "Get products by category ID (legacy)",
        description = "Retrieves a list of products belonging to a specific category. Publicly accessible. Consider using /api/products/filter for more options.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products by category",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class, type = "array"))),
            @ApiResponse(responseCode = "404", description = "Category not found (if category doesn't exist)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(
            @Parameter(description = "ID of the category", required = true, example = "1")
            @PathVariable Long categoryId) {
        log.info("Received request to get products by category ID: {}", categoryId);
        List<ProductResponse> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }
}