package com.fooddelivery.service;

import com.fooddelivery.dto.DishCategoryRequest;
import com.fooddelivery.dto.DishCategoryResponse;
import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.dto.DishResponse;

import java.util.List;

public interface DishService {

    DishResponse create(Long familyId, DishRequest request);

    List<DishResponse> list(Long familyId, Long categoryId, String keyword, Boolean available);

    DishResponse getDetail(Long familyId, Long dishId);

    DishResponse update(Long familyId, Long dishId, DishRequest request);

    DishResponse changeAvailability(Long familyId, Long dishId, boolean available);

    void delete(Long familyId, Long dishId);

    List<DishCategoryResponse> listCategories(Long familyId);

    DishCategoryResponse createCategory(Long familyId, DishCategoryRequest request);

    DishCategoryResponse updateCategory(Long familyId, Long categoryId, DishCategoryRequest request);

    void deleteCategory(Long familyId, Long categoryId);
}