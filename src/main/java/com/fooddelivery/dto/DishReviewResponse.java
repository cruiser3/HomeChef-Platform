package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishReviewResponse {

    private Long id;
    private Long dishId;
    private Long parentId;
    private boolean anonymous;
    private String userName;
    private String avatarUrl;
    private boolean mine;
    private Integer rating;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<DishReviewResponse> replies = new ArrayList<>();
}
