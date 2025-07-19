package com.intellimart.productservice.service;

import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.dto.CategoryResponse;
import com.intellimart.productservice.dto.StockDecrementRequest; // NEW IMPORT
import com.intellimart.productservice.entity.Category;
import com.intellimart.productservice.entity.Product;
import com.intellimart.productservice.exception.InsufficientStockException; // NEW IMPORT
import com.intellimart.productservice.exception.ResourceNotFoundException;
import com.intellimart.productservice.repository.CategoryRepository;
import com.intellimart.productservice.repository.ProductRepository;
import com.intellimart.productservice.service.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService { // Renamed and implements interface

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    @Override // From interface
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest, MultipartFile imageFile) {
        log.info("Creating product: {}", productRequest.getName());

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + productRequest.getCategoryId()));

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = imageStorageService.storeFile(imageFile);
        } else if (productRequest.getImageUrl() != null && !productRequest.getImageUrl().isEmpty()) {
            imageUrl = productRequest.getImageUrl();
        }

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .stock(productRequest.getStock())
                .imageUrl(imageUrl)
                .category(category)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getId());
        return mapToProductResponse(savedProduct);
    }

    @Override // From interface
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override // From interface
    public Page<ProductResponse> getAllProductsPaginated(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching products with page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productRepository.findAll(pageable);
        return products.map(this::mapToProductResponse);
    }

    @Override // From interface
    public Page<ProductResponse> getFilteredProducts(
            String nameQuery, String descriptionQuery, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size, String sortBy, String sortDir) {
        log.info("Filtering products with nameQuery={}, descQuery={}, categoryId={}, minPrice={}, maxPrice={}, page={}, size={}, sortBy={}, sortDir={}",
                nameQuery, descriptionQuery, categoryId, minPrice, maxPrice, page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // NOTE: This assumes you have a `findProductsByCriteria` method in ProductRepository.
        // If not, you'll need to implement it there (e.g., using @Query or Specification).
        Page<Product> products = productRepository.findProductsByCriteria(
                nameQuery, descriptionQuery, categoryId, minPrice, maxPrice, pageable);

        return products.map(this::mapToProductResponse);
    }

    @Override // From interface
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToProductResponse(product);
    }

    @Override // From interface
    @Transactional
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
            String oldImageUrl = existingProduct.getImageUrl();
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    imageStorageService.deleteFile(oldImageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old image {}: {}", oldImageUrl, e.getMessage());
                }
            }
            String newImageUrl = imageStorageService.storeFile(imageFile);
            existingProduct.setImageUrl(newImageUrl);
        } else if (productRequest.getImageUrl() != null) {
            existingProduct.setImageUrl(productRequest.getImageUrl());
        }

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated with ID: {}", updatedProduct.getId());
        return mapToProductResponse(updatedProduct);
    }

    @Override // From interface
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        Product productToDelete = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (productToDelete.getImageUrl() != null && !productToDelete.getImageUrl().isEmpty()) {
            try {
                imageStorageService.deleteFile(productToDelete.getImageUrl());
                log.info("Associated image deleted for product ID: {}", id);
            } catch (Exception e) {
                log.warn("Failed to delete image for product ID {}: {}", id, e.getMessage());
            }
        }
        productRepository.delete(productToDelete);
        log.info("Product deleted with ID: {}", id);
    }

    @Override // From interface
    public List<ProductResponse> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        // NOTE: This assumes you have `findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase` in ProductRepository
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override // From interface
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        log.info("Fetching products by category ID: {}", categoryId);
        // NOTE: This assumes you have `findByCategoryId` in ProductRepository
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    // --- NEW CRITICAL METHODS FOR ORDER-SERVICE INTEGRATION ---
    @Override // From interface
    @Transactional
    public void decrementStock(StockDecrementRequest request) throws ResourceNotFoundException, InsufficientStockException {
        log.info("Attempting to decrement stock for product ID: {} by quantity: {}", request.getProductId(), request.getQuantity());
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for product ID: " + request.getProductId() +
                                                 ". Available: " + product.getStock() + ", Requested: " + request.getQuantity());
        }

        product.setStock(product.getStock() - request.getQuantity());
        product.setUpdatedAt(LocalDateTime.now()); // Update timestamp on stock change
        productRepository.save(product);
        log.info("Stock for product ID: {} decremented to: {}", product.getId(), product.getStock());
    }

    @Override // From interface
    @Transactional
    public void incrementStock(Long productId, Integer quantity) throws ResourceNotFoundException {
        log.info("Attempting to increment stock for product ID: {} by quantity: {}", productId, quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setStock(product.getStock() + quantity);
        product.setUpdatedAt(LocalDateTime.now()); // Update timestamp on stock change
        productRepository.save(product);
        log.info("Stock for product ID: {} incremented to: {}", product.getId(), product.getStock());
    }

    // Helper method to map Product entity to ProductResponse DTO
    private ProductResponse mapToProductResponse(Product product) {
        CategoryResponse categoryResponse = null;
        if (product.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .description(product.getCategory().getDescription())
                    .createdAt(product.getCategory().getCreatedAt()) // Ensure audit fields are mapped
                    .updatedAt(product.getCategory().getUpdatedAt()) // Ensure audit fields are mapped
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