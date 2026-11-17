package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 广场菜品评论实体。
 * <p>
 * 结构对齐家庭菜品评论 {@link DishReview}，差异在于广场是全局空间、
 * 无家庭隔离，因此没有 {@code familyId}，而是通过 {@code dishId} 关联
 * 到 {@link SystemDish}。{@code parentId} 支持对评论的回复。
 */
@Entity
@Table(name = "system_dish_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDishReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
