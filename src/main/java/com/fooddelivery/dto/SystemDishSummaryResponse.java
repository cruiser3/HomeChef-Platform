package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDishSummaryResponse {
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
    private Long sourceFamilyId;
    private Long publishedBy;
    private String familyName;
    private String publisherName;
    private String publisherAvatarUrl;
    private Double averageRating;
    private Integer reviewCount;
}
