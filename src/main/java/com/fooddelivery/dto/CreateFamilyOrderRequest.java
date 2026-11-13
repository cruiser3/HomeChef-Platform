package com.fooddelivery.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFamilyOrderRequest {

    @Size(max = 20, message = "Meal period must be at most 20 characters")
    private String mealPeriod;

    @Size(max = 500, message = "Remark must be at most 500 characters")
    private String remark;
}