package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 菜谱结构化结果（草稿）。仅用于回填前端表单，不直接落库。
 * amount 与 unit 语义与 {@link DishRequest.DishIngredientRequest} 对齐；
 * 模糊量由 AI 约定为 amount=0 + unit=原文（如「少许」）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecipeParseResponse {

    private String name;

    private List<Ingredient> ingredients;

    private List<String> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ingredient {
        private String name;
        private BigDecimal amount;
        private String unit;
    }
}
