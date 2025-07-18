package com.intellimart.productservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${product.images.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This maps requests to /images/** to the local file system directory
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/"); // IMPORTANT: The "file:" prefix and trailing slash are crucial
    }
}