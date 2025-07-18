package com.intellimart.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.io.IOException;
import java.math.BigDecimal; // !!! NEW IMPORT: For price filtering
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    // Create a new product (ADMIN only) - now accepts file upload
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);

        ProductResponse newProduct = productService.createProduct(productRequest, imageFile);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }

    // Get all products (Public access)
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // Get all products paginated (Public access)
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductResponse>> getAllProductsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<ProductResponse> products = productService.getAllProductsPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }

    /**
     * NEW ENDPOINT: Retrieves products with optional filtering by category, price range,
     * search query (name/description), along with pagination and sorting.
     * All parameters are optional. This endpoint is publicly accessible.
     *
     * @param categoryId Optional: ID of the category to filter by.
     * @param minPrice Optional: Minimum price (inclusive).
     * @param maxPrice Optional: Maximum price (inclusive).
     * @param query Optional: Search term for product name or description.
     * @param page Page number (default 0).
     * @param size Number of items per page (default 10).
     * @param sortBy Field to sort by (default "id").
     * @param sortDir Sort direction (default "asc").
     * @return A paginated list of ProductResponse DTOs.
     */
    @GetMapping("/filter") // A new dedicated endpoint for comprehensive filtering
    public ResponseEntity<Page<ProductResponse>> getFilteredProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String query, // General search query for name/description
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        // Pass the 'query' parameter for both nameQuery and descriptionQuery to the service
        Page<ProductResponse> products = productService.getFilteredProducts(
                query, query, categoryId, minPrice, maxPrice,
                page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }


    // Get product by ID (Public access)
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // Update an existing product (ADMIN only) - now accepts file upload
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {

        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);

        ProductResponse updatedProduct = productService.updateProduct(id, productRequest, imageFile);
        return ResponseEntity.ok(updatedProduct);
    }

    // Delete a product by ID (ADMIN only)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Search products (Public access) - Existing endpoint, can be partially superseded by /filter
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String query) {
        List<ProductResponse> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    // Get products by category ID (Public access) - Existing endpoint, can be partially superseded by /filter
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(@PathVariable Long categoryId) {
        List<ProductResponse> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }
}