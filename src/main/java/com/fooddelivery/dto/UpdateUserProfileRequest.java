package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must be at most 50 characters")
    private String name;

    @Size(max = 500, message = "Avatar url must be at most 500 characters")
    private String avatarUrl;

    @Size(max = 5, message = "At most 5 dietary preferences are allowed")
    private List<@Size(max = 32, message = "Dietary preference must be at most 32 characters") String> dietaryPreferences;

    @Size(max = 5, message = "At most 5 dietary restrictions are allowed")
    private List<@Size(max = 32, message = "Dietary restriction must be at most 32 characters") String> dietaryRestrictions;

    @Size(max = 8, message = "At most 8 allergies are allowed")
    private List<@Size(max = 32, message = "Allergy must be at most 32 characters") String> allergies;

    @Size(max = 8, message = "At most 8 medical conditions are allowed")
    private List<@Size(max = 32, message = "Medical condition must be at most 32 characters") String> medicalConditions;
}
