package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;
    private String name;
    private String email;
    private String avatarUrl;
    private List<String> dietaryPreferences;
    private List<String> dietaryRestrictions;
    private List<String> allergies;
    private List<String> medicalConditions;
}
