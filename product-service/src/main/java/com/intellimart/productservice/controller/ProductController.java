package com.intellimart.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // !!! NEW IMPORT: For converting JSON string part to DTO
import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // !!! NEW IMPORT: For security annotations
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // !!! NEW IMPORT: For handling file uploads
import jakarta.validation.Valid;

import java.io.IOException; // !!! NEW IMPORT: For ObjectMapper exceptions
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper; // !!! NEW: Inject ObjectMapper for JSON string deserialization

    // Create a new product (ADMIN only) - now accepts file upload
    // !!! MODIFIED: Consumes multipart/form-data and accepts ProductRequest as JSON string and MultipartFile
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')") // !!! NEW: Secure this endpoint for ADMIN role
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") String productJson, // Product data as a JSON string part
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException { // Optional image file part
        
        // Convert the product JSON string part to ProductRequest DTO
        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
        
        // Call the service with both DTO and file
        ProductResponse newProduct = productService.createProduct(productRequest, imageFile);
        return new ResponseEntity<>(newProduct, HttpStatus.CREATED);
    }

    // Get all products (Public access) - NO CHANGE
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    // Get all products paginated (Public access) - NO CHANGE
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductResponse>> getAllProductsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Page<ProductResponse> products = productService.getAllProductsPaginated(page, size, sortBy, sortDir);
        return ResponseEntity.ok(products);
    }

    // Get product by ID (Public access) - NO CHANGE
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // Update an existing product (ADMIN only) - now accepts file upload
    // !!! MODIFIED: Consumes multipart/form-data and accepts ProductRequest as JSON string and MultipartFile
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('ADMIN')") // !!! NEW: Secure this endpoint for ADMIN role
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson, // Product data as a JSON string part
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException { // Optional image file part
        
        // Convert the product JSON string part to ProductRequest DTO
        ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
        
        // Call the service with both DTO and file
        ProductResponse updatedProduct = productService.updateProduct(id, productRequest, imageFile);
        return ResponseEntity.ok(updatedProduct);
    }

    // Delete a product by ID (ADMIN only)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Indicate 204 No Content for successful deletion
    @PreAuthorize("hasRole('ADMIN')") // !!! NEW: Secure this endpoint for ADMIN role
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Search products (Public access) - NO CHANGE
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String query) {
        List<ProductResponse> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    // Get products by category ID (Public access) - NO CHANGE
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategoryId(@PathVariable Long categoryId) {
        List<ProductResponse> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }

    // Note: You might want to add a global exception handler or specific handlers
    // for exceptions like ResourceNotFoundException, IOException, etc.,
    // if not already done in a @ControllerAdvice class.
}