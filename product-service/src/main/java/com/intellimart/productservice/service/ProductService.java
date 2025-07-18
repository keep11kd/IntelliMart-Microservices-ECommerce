package com.intellimart.productservice.service;

import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.dto.CategoryResponse;
import com.intellimart.productservice.entity.Category;
import com.intellimart.productservice.entity.Product;
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
import org.springframework.transaction.annotation.Transactional; // Import for @Transactional
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal; // !!! NEW IMPORT: For price filtering

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    @Transactional // Added @Transactional for create, update, delete operations
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

    /**
     * NEW METHOD: Retrieves products based on various filtering criteria, with pagination and sorting.
     * This method uses the flexible findProductsByCriteria from the repository.
     * @param nameQuery Optional: Part of product name to search.
     * @param descriptionQuery Optional: Part of product description to search.
     * @param categoryId Optional: ID of the category to filter by.
     * @param minPrice Optional: Minimum price in the range.
     * @param maxPrice Optional: Maximum price in the range.
     * @param page Page number.
     * @param size Number of items per page.
     * @param sortBy Field to sort by.
     * @param sortDir Sort direction (asc/desc).
     * @return A paginated list of ProductResponse DTOs.
     */
    public Page<ProductResponse> getFilteredProducts(
            String nameQuery, String descriptionQuery, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            int page, int size, String sortBy, String sortDir) {
        log.info("Filtering products with nameQuery={}, descQuery={}, categoryId={}, minPrice={}, maxPrice={}, page={}, size={}, sortBy={}, sortDir={}",
                nameQuery, descriptionQuery, categoryId, minPrice, maxPrice, page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Call the repository method that handles multiple optional criteria
        Page<Product> products = productRepository.findProductsByCriteria(
                nameQuery, descriptionQuery, categoryId, minPrice, maxPrice, pageable);

        return products.map(this::mapToProductResponse);
    }


    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToProductResponse(product);
    }

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
            // Optional: Delete old image if it exists before storing new one to save space
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
        } else if (productRequest.getImageUrl() != null) { // Allow explicit null to clear image, or new external URL
            existingProduct.setImageUrl(productRequest.getImageUrl());
        }
        // If imageFile is null/empty AND productRequest.getImageUrl() is also null,
        // the existing imageUrl will remain unchanged (unless explicitly set to null by request.getImageUrl() == null).

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated with ID: {}", updatedProduct.getId());
        return mapToProductResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        Product productToDelete = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Optional: Delete associated image file from storage when product is deleted
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

    // Keeping these for backward compatibility or specific use cases,
    // though getFilteredProducts can cover most search/category needs.
    public List<ProductResponse> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        // Note: This calls the repository method that combines name and description search
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        log.info("Fetching products by category ID: {}", categoryId);
        // Note: This calls the repository method that finds by category ID
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