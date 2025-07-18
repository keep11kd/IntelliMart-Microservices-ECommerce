package com.intellimart.productservice.service;

import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.dto.CategoryResponse; // Assuming CategoryResponse is in this package
import com.intellimart.productservice.entity.Category; // User's provided entity package
import com.intellimart.productservice.entity.Product;   // User's provided entity package
import com.intellimart.productservice.exception.ResourceNotFoundException; // User's provided exception
import com.intellimart.productservice.repository.CategoryRepository;
import com.intellimart.productservice.repository.ProductRepository;
import com.intellimart.productservice.service.storage.ImageStorageService; // !!! NEW IMPORT for Image Storage Service
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile; // !!! NEW IMPORT for MultipartFile

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok: Generates constructor with final fields (ProductRepository, CategoryRepository, ImageStorageService)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService; // !!! NEW: Inject ImageStorageService

    // !!! MODIFIED: createProduct now accepts MultipartFile
    public ProductResponse createProduct(ProductRequest productRequest, MultipartFile imageFile) {
        log.info("Creating product: {}", productRequest.getName());

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + productRequest.getCategoryId()));

        String imageUrl = null;
        // Handle image file upload if provided
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = imageStorageService.storeFile(imageFile);
        } else if (productRequest.getImageUrl() != null && !productRequest.getImageUrl().isEmpty()) {
            // If no file, but imageUrl is provided in request (e.g., for external URLs), use it
            imageUrl = productRequest.getImageUrl();
        }

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .stock(productRequest.getStock())
                .imageUrl(imageUrl) // Set the potentially stored image URL
                .category(category)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());
        return mapToProductResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public Page<ProductResponse> getAllProductsPaginated(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching products with page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productRepository.findAll(pageable);
        return products.map(this::mapToProductResponse);
    }

    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToProductResponse(product);
    }

    // !!! MODIFIED: updateProduct now accepts MultipartFile
    public ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile imageFile) {
        log.info("Updating product with ID: {}", id);
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + productRequest.getCategoryId()));

        existingProduct.setName(productRequest.getName());
        existingProduct.setDescription(productRequest.getDescription());
        existingProduct.setPrice(productRequest.getPrice());
        existingProduct.setStock(productRequest.getStock());
        existingProduct.setCategory(category);
        existingProduct.setUpdatedAt(LocalDateTime.now());

        // Handle image file update
        if (imageFile != null && !imageFile.isEmpty()) {
            // Optional: Delete old image if it exists before storing new one to save space
            // String oldImageUrl = existingProduct.getImageUrl();
            // if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            //     try {
            //         imageStorageService.deleteFile(oldImageUrl); // Pass the URL, service will clean it
            //     } catch (Exception e) {
            //         log.warn("Failed to delete old image {}: {}", oldImageUrl, e.getMessage());
            //     }
            // }
            String newImageUrl = imageStorageService.storeFile(imageFile);
            existingProduct.setImageUrl(newImageUrl);
        } else if (productRequest.getImageUrl() != null) { // Allow explicit null to clear image, or new external URL
            existingProduct.setImageUrl(productRequest.getImageUrl());
        }
        // If imageFile is null/empty AND productRequest.getImageUrl() is also null,
        // the existing imageUrl will remain unchanged (unless explicitly set to null by request.getImageUrl() == null).

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated with ID: {}", updatedProduct.getId());
        return mapToProductResponse(updatedProduct);
    }

    // !!! MODIFIED: deleteProduct - optionally delete image when product is removed
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        Product productToDelete = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Optional: Delete associated image file from storage when product is deleted
        if (productToDelete.getImageUrl() != null && !productToDelete.getImageUrl().isEmpty()) {
            try {
                imageStorageService.deleteFile(productToDelete.getImageUrl()); // Pass the URL, service handles cleaning
                log.info("Associated image deleted for product ID: {}", id);
            } catch (Exception e) {
                log.warn("Failed to delete image for product ID {}: {}", id, e.getMessage());
                // Consider if deletion should fail if image delete fails. For now, it just logs warning.
            }
        }
        productRepository.delete(productToDelete); // Using delete(entity) is often better for cascade behavior
        log.info("Product deleted with ID: {}", id);
    }

    public List<ProductResponse> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        log.info("Fetching products by category ID: {}", categoryId);
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse mapToProductResponse(Product product) {
        CategoryResponse categoryResponse = null;
        if (product.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .description(product.getCategory().getDescription())
                    .build();
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .category(categoryResponse)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}