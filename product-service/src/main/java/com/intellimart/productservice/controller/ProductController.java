package com.intellimart.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimart.productservice.dto.ErrorResponse; // !!! NEW IMPORT: For Swagger error response schema
import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation; // !!! NEW IMPORT
import io.swagger.v3.oas.annotations.Parameter; // !!! NEW IMPORT
import io.swagger.v3.oas.annotations.media.Content; // !!! NEW IMPORT
import io.swagger.v3.oas.annotations.media.Schema; // !!! NEW IMPORT
import io.swagger.v3.oas.annotations.responses.ApiResponse; // !!! NEW IMPORT
import io.swagger.v3.oas.annotations.tags.Tag; // !!! NEW IMPORT
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products in IntelliMart") // !!! NEW: Controller-level tag
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "Create a new product",
        description = "Creates a new product with details and an optional image. Requires ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing category",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), // !!! MODIFIED: Referencing ErrorResponse
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can create products")
        }
    )
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product data in JSON format", required = true)
            @RequestPart("product") String productJson,
            @Parameter(description = "Optional image file for the product")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
        ProductResponse newProduct = productService.createProduct(productRequest, imageFile);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }

    @Operation(
        summary = "Get all products",
        description = "Retrieves a list of all products. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
        }
    )
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @Operation(
        summary = "Get all products with pagination",
        description = "Retrieves a paginated list of all products. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))) // Page is a generic type, Schema doesn't directly support Page<ProductResponse> well, but it's okay for basic
        }
    )
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductResponse>> getAllProductsPaginated(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field to sort by (e.g., 'name', 'price', 'id')", example = "id") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction ('asc' or 'desc')", example = "asc") @RequestParam(defaultValue = "asc") String sortDir) {
        Page<ProductResponse> products = productService.getAllProductsPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }

    @Operation(
        summary = "Get filtered products with pagination",
        description = "Retrieves products based on optional filters (category, price range, search query) with pagination and sorting. Publicly accessible.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered and paginated list",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
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
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // !!! MODIFIED: Referencing ErrorResponse
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "ID of the product to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Operation(
        summary = "Update an existing product",
        description = "Updates an existing product's details and/or image. Requires ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or missing category",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))), // !!! MODIFIED: Referencing ErrorResponse
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can update products"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // !!! MODIFIED: Referencing ErrorResponse
        }
    )
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "ID of the product to update", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated product data in JSON format", required = true)
            @RequestPart("product") String productJson,
            @Parameter(description = "Optional new image file for the product")
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
        ProductResponse updatedProduct = productService.updateProduct(id, productRequest, imageFile);
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(
        summary = "Delete a product",
        description = "Deletes a product by its ID. Requires ADMIN role.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only ADMIN can delete products"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // !!! MODIFIED: Referencing ErrorResponse
        }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID of the product to delete", required = true, example = "1")
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Existing search endpoint (can be covered by /filter)
    @Operation(
        summary = "Search products by name or description (legacy)",
        description = "Retrieves a list of products matching the query in name or description. Publicly accessible. Consider using /api/products/filter for more options.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved search results",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class)))
        }
    )
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @Parameter(description = "Search term for product name or description", required = true, example = "phone")
            @RequestParam String query) {
        List<ProductResponse> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    // Existing category-specific endpoint (can be covered by /filter)
    @Operation(
        summary = "Get products by category ID (legacy)",
        description = "Retrieves a list of products belonging to a specific category. Publicly accessible. Consider using /api/products/filter for more options.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products by category",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Category not found (if category doesn't exist)",
                         content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))) // !!! MODIFIED: Referencing ErrorResponse
        }
    )
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(
            @Parameter(description = "ID of the category", required = true, example = "1")
            @PathVariable Long categoryId) {
        List<ProductResponse> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }
}