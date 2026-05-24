package com.huit.da_java.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {
    private final String productImageDirectory;

    public WebResourceConfig(@Value("${app.upload.product-images-dir:uploads/products}") String productImageDirectory) {
        this.productImageDirectory = productImageDirectory;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(productImageDirectory).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(location);
    }
}
