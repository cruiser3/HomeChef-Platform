package com.fooddelivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AiCalorieRequest {

    @NotBlank(message = "菜名不能为空")
    @Size(max = 100, message = "菜名不能超过 100 字")
    private String name;

    @Valid
    private List<IngredientItem> ingredients;

    @Data
    public static class IngredientItem {

        @NotBlank(message = "食材名称不能为空")
        @Size(max = 100, message = "食材名称不能超过 100 字")
        private String name;

        private BigDecimal amount;

        @Size(max = 20, message = "单位不能超过 20 字")
        private String unit;

        @Size(max = 100, message = "用量说明不能超过 100 字")
        private String amountText;
    }
}
