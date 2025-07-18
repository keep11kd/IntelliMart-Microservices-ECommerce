package com.intellimart.cartservice.client;

import com.intellimart.cartservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service") // 'product-service' is the name registered in Eureka
public interface ProductServiceClient {

    @GetMapping("/api/products/{productId}") // Endpoint in product-service to get product by ID
    ProductResponse getProductById(@PathVariable("productId") Long productId);
}