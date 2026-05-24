package com.huit.da_java.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductImageStorageService {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp");

    private final Path uploadDirectory;

    public ProductImageStorageService(@Value("${app.upload.product-images-dir:uploads/products}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile imageFile) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh sản phẩm không được lớn hơn 5 MB.");
        }

        String extension = getExtension(imageFile.getOriginalFilename());
        String expectedContentType = CONTENT_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equalsIgnoreCase(imageFile.getContentType())) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG, PNG, GIF hoặc WEBP.");
        }

        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Tên tệp ảnh không hợp lệ.");
        }
        try (var content = imageFile.getInputStream()) {
            Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/products/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotPosition = filename.lastIndexOf('.');
        return dotPosition < 0 ? "" : filename.substring(dotPosition + 1).toLowerCase(Locale.ROOT);
    }
}
