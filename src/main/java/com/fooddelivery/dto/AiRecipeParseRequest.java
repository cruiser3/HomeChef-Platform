package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiRecipeParseRequest {

    @NotBlank(message = "Recipe text is required")
    @Size(max = 5000, message = "Recipe text must not exceed 5000 characters")
    private String text;
}
