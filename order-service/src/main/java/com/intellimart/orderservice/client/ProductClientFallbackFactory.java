package com.intellimart.orderservice.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * FallbackFactory for ProductClient.
 * This factory creates instances of ProductClientFallback,
 * providing the Throwable that caused the fallback to the fallback instance.
 */
@Component // Must be a Spring component
@Slf4j
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        log.error("Fallback factory triggered for ProductClient. Cause: {}", cause.getMessage(), cause);
        return new ProductClientFallback(cause);
    }
}