package com.intellimart.productservice.service;

import com.intellimart.productservice.client.OrderClient; // NEW IMPORT
import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.dto.CategoryResponse;
import com.intellimart.productservice.dto.StockDecrementRequest;
import com.intellimart.productservice.dto.RecommendationResponse; // NEW IMPORT
import com.intellimart.productservice.dto.OrderResponseForProductService; // NEW IMPORT
import com.intellimart.productservice.dto.OrderItemResponseForProductService; // NEW IMPORT
import com.intellimart.productservice.entity.Category;
import com.intellimart.productservice.entity.Product;
import com.intellimart.productservice.exception.InsufficientStockException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;
    private final OrderClient orderClient; // NEW: Inject OrderClient

    @Override
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

    @Override
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all products");
        return productRepository.findAll()
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ProductResponse> getAllProductsPaginated(int page, int size, String sortBy, String sortDir) {
        log.info("Fetching products with page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir);
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> products = productRepository.findAll(pageable);
        return products.map(this::mapToProductResponse);
    }

    @Override
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

    @Override
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapToProductResponse(product);
    }

    @Override
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

    @Override
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

    @Override
    public List<ProductResponse> searchProducts(String query) {
        log.info("Searching products with query: {}", query);
        // NOTE: This assumes you have `findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase` in ProductRepository
        List<Product> products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        log.info("Fetching products by category ID: {}", categoryId);
        // NOTE: This assumes you have `findByCategoryId` in ProductRepository
        List<Product> products = productRepository.findByCategoryId(categoryId);
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
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
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Stock for product ID: {} decremented to: {}", product.getId(), product.getStock());
    }

    @Override
    @Transactional
    public void incrementStock(Long productId, Integer quantity) throws ResourceNotFoundException {
        log.info("Attempting to increment stock for product ID: {} by quantity: {}", productId, quantity);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setStock(product.getStock() + quantity);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Stock for product ID: {} incremented to: {}", product.getId(), product.getStock());
    }

    /**
     * Retrieves a list of recommended products based on various rules.
     *
     * @param productId The ID of the product for which to get recommendations.
     * @return A list of RecommendationResponse objects.
     */
    @Override
    public List<RecommendationResponse> getProductRecommendations(Long productId) {
        log.info("Generating recommendations for product ID: {}", productId);
        List<RecommendationResponse> recommendations = new ArrayList<>();
        Product targetProduct;

        try {
            targetProduct = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target product not found for recommendations: " + productId));
        } catch (ResourceNotFoundException e) { // Changed to ResourceNotFoundException
            log.error("Failed to find target product for recommendations: {}", e.getMessage());
            return Collections.emptyList(); // Return empty list if target product not found
        }

        // 1. "Same Category" Recommendations
        log.debug("Finding same category recommendations for product ID: {}", productId);
        // Use targetProduct.getCategory() as the parameter for findByCategoryAndIdNot
        List<Product> sameCategoryProducts = productRepository.findByCategoryAndIdNot(targetProduct.getCategory(), productId);
        sameCategoryProducts.stream()
                .map(product -> mapToRecommendationResponse(product, "Same Category"))
                .forEach(recommendations::add);
        log.debug("Found {} same category recommendations.", sameCategoryProducts.size());


        // 2. "Customers who bought this also bought..." Recommendations
        log.debug("Finding 'also bought' recommendations for product ID: {}", productId);
        try {
            // Ensure the OrderClient returns a List of OrderResponseForProductService
            List<OrderResponseForProductService> ordersContainingProduct = orderClient.getOrdersByProductId(productId);
            log.debug("Found {} orders containing product ID: {}", ordersContainingProduct.size(), productId);

            Map<Long, Long> otherProductCounts = ordersContainingProduct.stream()
                    .flatMap(order -> order.getOrderItems().stream()) // Get all order items from these orders
                    .filter(item -> !item.getProductId().equals(productId)) // Exclude the target product itself
                    .collect(Collectors.groupingBy(OrderItemResponseForProductService::getProductId, Collectors.counting())); // Count occurrences of other products

            // Get the top N "also bought" product IDs
            List<Long> topAlsoBoughtProductIds = otherProductCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // Sort by count (most bought together)
                    .limit(5) // Limit to top 5 recommendations
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            log.debug("Top 'also bought' product IDs: {}", topAlsoBoughtProductIds);

            List<Product> alsoBoughtProducts = productRepository.findAllById(topAlsoBoughtProductIds);
            alsoBoughtProducts.stream()
                    .filter(p -> !recommendations.stream().anyMatch(r -> r.getId().equals(p.getId()))) // Avoid duplicates with same category
                    .map(product -> mapToRecommendationResponse(product, "Customers Also Bought"))
                    .forEach(recommendations::add);
            log.debug("Found {} 'also bought' recommendations.", alsoBoughtProducts.size());

        } catch (Exception e) {
            log.error("Error fetching 'also bought' recommendations from order-service for product ID {}: {}", productId, e.getMessage(), e);
            // Graceful degradation: continue without "also bought" recommendations if order-service is down
        }

        // 3. "Recently Viewed" (Placeholder for client-side or more complex logic)
        log.debug("Skipping 'Recently Viewed' recommendations (client-side or dedicated service logic needed).");

        // Filter out the target product itself from the recommendations and ensure distinctness
        List<RecommendationResponse> finalRecommendations = recommendations.stream()
                .filter(rec -> !rec.getId().equals(productId))
                .distinct() // Remove any remaining duplicates
                .collect(Collectors.toList());

        log.info("Generated {} total recommendations for product ID: {}", finalRecommendations.size(), productId);
        return finalRecommendations;
    }


    // Helper method to map Product entity to ProductResponse DTO
    private ProductResponse mapToProductResponse(Product product) {
        CategoryResponse categoryResponse = null;
        if (product.getCategory() != null) {
            categoryResponse = CategoryResponse.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .description(product.getCategory().getDescription())
                    .createdAt(product.getCategory().getCreatedAt())
                    .updatedAt(product.getCategory().getUpdatedAt())
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

    // NEW Helper method to map Product entity to RecommendationResponse DTO
    private RecommendationResponse mapToRecommendationResponse(Product product, String reason) {
        return RecommendationResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null) // Get category ID from Category object
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null) // Get category name from Category object
                .recommendationReason(reason)
                .build();
    }
}