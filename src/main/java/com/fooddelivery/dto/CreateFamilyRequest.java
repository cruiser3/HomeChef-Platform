package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateFamilyRequest {

    @NotBlank(message = "Family name is required")
    @Size(max = 50, message = "Family name must not exceed 50 characters")
    private String name;
}