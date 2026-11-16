package com.fooddelivery.controller;

import com.fooddelivery.dto.UploadResponse;
import com.fooddelivery.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @PostMapping
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size must not exceed 5MB");
        }

        String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Only jpg, jpeg, png, webp, gif images are allowed");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        return ResponseEntity.ok(new UploadResponse("/uploads/" + filename));
    }

    private String resolveExtension(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }
}
