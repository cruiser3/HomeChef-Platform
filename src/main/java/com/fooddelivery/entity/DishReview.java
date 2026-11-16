package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "dish_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_anonymous", nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
}
