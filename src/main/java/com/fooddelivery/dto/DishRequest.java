package com.fooddelivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DishRequest {

    @NotBlank(message = "Dish name is required")
    @Size(max = 100, message = "Dish name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 500, message = "Image url must not exceed 500 characters")
    private String imageUrl;

    private Long categoryId;

    @Min(value = 0, message = "Cooking minutes must be zero or positive")
    private Integer cookingMinutes;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be zero or positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimals")
    private BigDecimal price;

    @Valid
    private List<DishIngredientRequest> ingredients;

    @Valid
    private List<DishStepRequest> steps;

    @Data
    public static class DishIngredientRequest {

        @NotBlank(message = "Ingredient name is required")
        @Size(max = 100, message = "Ingredient name must not exceed 100 characters")
        private String name;

        @DecimalMin(value = "0.00", message = "Ingredient amount must be zero or positive")
        @Digits(integer = 8, fraction = 2, message = "Ingredient amount must have at most 2 decimals")
        private BigDecimal amount;

        @Size(max = 20, message = "Ingredient unit must not exceed 20 characters")
        private String unit;

        @Size(max = 100, message = "Ingredient amount text must not exceed 100 characters")
        private String amountText;
    }

    @Data
    public static class DishStepRequest {

        @NotNull(message = "Step number is required")
        @Min(value = 1, message = "Step number must start from 1")
        private Integer stepNo;

        @NotBlank(message = "Step content is required")
        @Size(max = 500, message = "Step content must not exceed 500 characters")
        private String content;

        @Size(max = 500, message = "Step image url must not exceed 500 characters")
        private String imageUrl;
    }
}