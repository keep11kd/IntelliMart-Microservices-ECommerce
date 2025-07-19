package com.intellimart.productservice.service;

import com.intellimart.productservice.dto.ProductRequest;
import com.intellimart.productservice.dto.ProductResponse;
import com.intellimart.productservice.dto.StockDecrementRequest; // NEW IMPORT
import com.intellimart.productservice.exception.InsufficientStockException; // NEW IMPORT
import com.intellimart.productservice.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    // CRUD Operations with Image handling
    ProductResponse createProduct(ProductRequest productRequest, MultipartFile imageFile) throws ResourceNotFoundException;
    ProductResponse getProductById(Long id) throws ResourceNotFoundException;
    List<ProductResponse> getAllProducts();
    ProductResponse updateProduct(Long id, ProductRequest productRequest, MultipartFile imageFile) throws ResourceNotFoundException;
    void deleteProduct(Long id) throws ResourceNotFoundException;

    // Pagination and Filtering
    Page<ProductResponse> getAllProductsPaginated(int page, int size, String sortBy, String sortDir);
    Page<ProductResponse> getFilteredProducts(String nameQuery, String descriptionQuery, Long categoryId,
                                            BigDecimal minPrice, BigDecimal maxPrice,
                                            int page, int size, String sortBy, String sortDir);

    // Search by query (e.g., name or description)
    List<ProductResponse> searchProducts(String query);

    // Filter by Category ID
    List<ProductResponse> getProductsByCategoryId(Long categoryId);

    // --- CRITICAL METHODS FOR ORDER-SERVICE INTEGRATION ---
    void decrementStock(StockDecrementRequest request) throws ResourceNotFoundException, InsufficientStockException;
    void incrementStock(Long productId, Integer quantity) throws ResourceNotFoundException;
}