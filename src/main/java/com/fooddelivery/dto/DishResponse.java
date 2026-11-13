package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishResponse {
    private Long id;
    private Long familyId;
    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Integer cookingMinutes;
    private BigDecimal price;
    private Long createdBy;
    private String createdByName;
    private boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<IngredientItem> ingredients;
    private List<StepItem> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IngredientItem {
        private String name;
        private BigDecimal amount;
        private String unit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StepItem {
        private int stepNo;
        private String content;
        private String imageUrl;
    }
}