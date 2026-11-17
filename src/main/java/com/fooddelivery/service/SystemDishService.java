package com.fooddelivery.service;

import com.fooddelivery.dto.CopyToFamilyRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.dto.SystemDishCategoryResponse;
import com.fooddelivery.dto.SystemDishDetailResponse;
import com.fooddelivery.dto.SystemDishSummaryResponse;

import java.util.List;

public interface SystemDishService {

    List<SystemDishCategoryResponse> listCategories();

    List<SystemDishSummaryResponse> list(String tab, Long categoryId, Long cuisineId, Integer caloriesMin,
            Integer caloriesMax, String keyword);

    SystemDishDetailResponse getDetail(Long id);

    SystemDishDetailResponse setCuisine(Long id, Long cuisineId);

    DishResponse copyToFamily(Long familyId, Long id, CopyToFamilyRequest request);

    SystemDishDetailResponse publishToSquare(Long familyId, Long dishId, Long categoryId);

    SystemDishDetailResponse setAvailability(Long id, boolean available);

    void withdrawPublish(Long id);
}
