package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "system_dishes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_system_dishes_source",
                columnNames = {"source_type", "original_path"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "cuisine_id")
    private Long cuisineId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "cooking_minutes")
    private Integer cookingMinutes;

    private Integer difficulty;

    private Integer calories;

    @Column(name = "source_type", nullable = false, length = 30)
    @Builder.Default
    private String sourceType = "HOWTOCOOK";

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "original_path", length = 500)
    private String originalPath;

    @Column(name = "source_commit", length = 64)
    private String sourceCommit;

    @Column(length = 50)
    private String license;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
}
