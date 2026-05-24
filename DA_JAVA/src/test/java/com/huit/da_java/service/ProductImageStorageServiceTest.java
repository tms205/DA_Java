package com.huit.da_java.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageStorageServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAnUploadedProductImage() throws Exception {
        ProductImageStorageService storageService = new ProductImageStorageService(temporaryDirectory.toString());
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "latte.png",
                "image/png",
                new byte[] {1, 2, 3});

        String imageUrl = storageService.store(imageFile);

        assertTrue(imageUrl.startsWith("/uploads/products/"));
        assertTrue(imageUrl.endsWith(".png"));
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        assertTrue(Files.exists(temporaryDirectory.resolve(filename)));
    }

    @Test
    void rejectsFilesThatAreNotSupportedImages() {
        ProductImageStorageService storageService = new ProductImageStorageService(temporaryDirectory.toString());
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "notes.txt",
                "text/plain",
                new byte[] {1, 2, 3});

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> storageService.store(imageFile));

        assertEquals("Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WEBP.", error.getMessage());
    }
}
