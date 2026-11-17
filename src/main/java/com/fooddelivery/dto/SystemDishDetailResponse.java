package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDishDetailResponse {
    private Long id;
    private String name;
    private String description;
    private String coverImageUrl;
    private Long categoryId;
    private String categoryName;
    private Long cuisineId;
    private String cuisineName;
    private Integer cookingMinutes;
    private Integer difficulty;
    private Integer calories;
    private String sourceType;
    private String sourceUrl;
    private String originalPath;
    private String sourceCommit;
    private String license;
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
        private String amountText;
        private boolean optional;
        private int sortNo;
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
