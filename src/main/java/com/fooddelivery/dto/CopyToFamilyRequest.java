package com.fooddelivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CopyToFamilyRequest {

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be zero or positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimals")
    private BigDecimal price;

    @Valid
    private List<IngredientOverride> ingredientOverrides;

    @Data
    public static class IngredientOverride {

        @NotNull(message = "Ingredient sortNo is required")
        @Min(value = 0, message = "Ingredient sortNo must be zero or positive")
        private Integer sortNo;

        @NotNull(message = "Ingredient amount is required")
        @DecimalMin(value = "0.00", message = "Ingredient amount must be zero or positive")
        @Digits(integer = 8, fraction = 2, message = "Ingredient amount must have at most 2 decimals")
        private BigDecimal amount;

        @NotBlank(message = "Ingredient unit is required")
        @Size(max = 20, message = "Ingredient unit must not exceed 20 characters")
        private String unit;
    }
}
