package com.fooddelivery.service;

import com.fooddelivery.dto.CopyToFamilyRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.dto.SystemDishCategoryResponse;
import com.fooddelivery.dto.SystemDishDetailResponse;
import com.fooddelivery.dto.SystemDishSummaryResponse;

import java.util.List;

public interface SystemDishService {

    List<SystemDishCategoryResponse> listCategories();

    List<SystemDishSummaryResponse> list(Long categoryId, String keyword);

    SystemDishDetailResponse getDetail(Long id);

    DishResponse copyToFamily(Long familyId, Long id, CopyToFamilyRequest request);
}
