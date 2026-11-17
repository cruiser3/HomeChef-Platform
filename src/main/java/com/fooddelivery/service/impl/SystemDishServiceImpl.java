package com.fooddelivery.service.impl;

import com.fooddelivery.dto.CopyToFamilyRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.dto.SystemDishCategoryResponse;
import com.fooddelivery.dto.SystemDishDetailResponse;
import com.fooddelivery.dto.SystemDishSummaryResponse;
import com.fooddelivery.entity.Cuisine;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.DishCategory;
import com.fooddelivery.entity.DishIngredient;
import com.fooddelivery.entity.DishStep;
import com.fooddelivery.entity.SystemDish;
import com.fooddelivery.entity.SystemDishCategory;
import com.fooddelivery.entity.SystemDishIngredient;
import com.fooddelivery.entity.SystemDishStep;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CuisineRepository;
import com.fooddelivery.repository.DishIngredientRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.DishStepRepository;
import com.fooddelivery.repository.SystemDishCategoryRepository;
import com.fooddelivery.repository.SystemDishIngredientRepository;
import com.fooddelivery.repository.SystemDishRepository;
import com.fooddelivery.repository.SystemDishStepRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.FamilyDishCategoryResolver;
import com.fooddelivery.service.SystemDishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemDishServiceImpl implements SystemDishService {

    private final SystemDishRepository systemDishRepository;
    private final CuisineRepository cuisineRepository;
    private final SystemDishIngredientRepository systemDishIngredientRepository;
    private final SystemDishStepRepository systemDishStepRepository;
    private final SystemDishCategoryRepository systemDishCategoryRepository;
    private final FamilyDishCategoryResolver familyDishCategoryResolver;
    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final DishStepRepository dishStepRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<SystemDishCategoryResponse> listCategories() {
        return systemDishCategoryRepository.findByAvailableTrueOrderBySortNoAsc().stream()
                .map(category -> SystemDishCategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .sortNo(category.getSortNo())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemDishSummaryResponse> list(Long categoryId, Long cuisineId, Integer caloriesMin,
            Integer caloriesMax, String keyword) {
        List<SystemDish> dishes;
        if (keyword != null && !keyword.isBlank()) {
            dishes = systemDishRepository.findByNameContainingIgnoreCaseAndAvailableTrueOrderByIdDesc(keyword.trim());
            if (categoryId != null) {
                dishes = dishes.stream().filter(d -> categoryId.equals(d.getCategoryId())).toList();
            }
        } else if (categoryId != null) {
            dishes = systemDishRepository.findByCategoryIdAndAvailableTrueOrderByIdDesc(categoryId);
        } else {
            dishes = systemDishRepository.findByAvailableTrueOrderByIdDesc();
        }

        if (cuisineId != null) {
            if (isUncategorized(cuisineId)) {
                dishes = dishes.stream().filter(d -> d.getCuisineId() == null).toList();
            } else {
                dishes = dishes.stream().filter(d -> cuisineId.equals(d.getCuisineId())).toList();
            }
        }
        // 卡路里区间过滤：左闭右开 [caloriesMin, caloriesMax)。
        if (caloriesMin != null) {
            dishes = dishes.stream().filter(d -> d.getCalories() != null && d.getCalories() >= caloriesMin).toList();
        }
        if (caloriesMax != null) {
            dishes = dishes.stream().filter(d -> d.getCalories() != null && d.getCalories() < caloriesMax).toList();
        }

        Map<Long, String> categoryNames = categoryNamesOf(dishes);
        Map<Long, String> cuisineNames = cuisineNamesOf(dishes);
        return dishes.stream()
                .map(dish -> toSummaryResponse(dish, categoryNames, cuisineNames))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemDishDetailResponse getDetail(Long id) {
        return toDetailResponse(findAvailableSystemDish(id));
    }

    @Override
    public SystemDishDetailResponse setCuisine(Long id, Long cuisineId) {
        if (cuisineId != null && !cuisineRepository.existsById(cuisineId)) {
            throw new BadRequestException("Cuisine does not exist: " + cuisineId);
        }
        SystemDish dish = findAvailableSystemDish(id);
        dish.setCuisineId(cuisineId);
        return toDetailResponse(systemDishRepository.save(dish));
    }

    @Override
    public DishResponse copyToFamily(Long familyId, Long id, CopyToFamilyRequest request) {
        currentUserService.requireMembership(familyId);
        SystemDish systemDish = findAvailableSystemDish(id);
        Long creatorId = currentUserService.getCurrentUserId();

        List<SystemDishIngredient> systemIngredients =
                systemDishIngredientRepository.findByDishIdOrderBySortNoAsc(id);
        List<SystemDishStep> systemSteps =
                systemDishStepRepository.findByDishIdOrderByStepNoAsc(id);

        DishCategory familyCategory = mapCategory(familyId, systemDish.getCategoryId());

        Dish dish = Dish.builder()
                .familyId(familyId)
                .name(systemDish.getName())
                .description(systemDish.getDescription())
                .imageUrl(systemDish.getCoverImageUrl())
                .categoryId(familyCategory == null ? null : familyCategory.getId())
                .cuisineId(systemDish.getCuisineId())
                .cookingMinutes(systemDish.getCookingMinutes())
                .calories(systemDish.getCalories())
                .price(request.getPrice())
                .createdBy(creatorId)
                .available(true)
                .build();
        Dish saved = dishRepository.save(dish);

        List<DishIngredient> resolvedIngredients =
                resolveIngredients(systemIngredients, request.getIngredientOverrides());
        for (int i = 0; i < resolvedIngredients.size(); i++) {
            DishIngredient resolved = resolvedIngredients.get(i);
            dishIngredientRepository.save(DishIngredient.builder()
                    .dishId(saved.getId())
                    .name(resolved.getName())
                    .amount(resolved.getAmount())
                    .unit(resolved.getUnit())
                    .amountText(resolved.getAmountText())
                    .sortNo(i)
                    .build());
        }

        for (SystemDishStep step : systemSteps) {
            dishStepRepository.save(DishStep.builder()
                    .dishId(saved.getId())
                    .stepNo(step.getStepNo())
                    .content(step.getContent())
                    .imageUrl(step.getImageUrl())
                    .build());
        }

        return buildDishResponse(saved, familyCategory, resolvedIngredients, systemSteps,
                currentUserService.getCurrentUser().getName());
    }

    private SystemDish findAvailableSystemDish(Long id) {
        return systemDishRepository.findByIdAndAvailableTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("System dish not found with id: " + id));
    }

    private DishCategory mapCategory(Long familyId, Long systemCategoryId) {
        if (systemCategoryId == null) {
            return null;
        }
        SystemDishCategory systemCategory = systemDishCategoryRepository.findById(systemCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "System dish category not found with id: " + systemCategoryId));
        return familyDishCategoryResolver.getOrCreate(familyId, systemCategory.getName());
    }

    private List<DishIngredient> resolveIngredients(
            List<SystemDishIngredient> systemIngredients,
            List<CopyToFamilyRequest.IngredientOverride> overrides) {
        Map<Integer, CopyToFamilyRequest.IngredientOverride> overrideBySortNo = new HashMap<>();
        if (overrides != null) {
            for (CopyToFamilyRequest.IngredientOverride override : overrides) {
                if (override != null) {
                    overrideBySortNo.put(override.getSortNo(), override);
                }
            }
        }

        List<DishIngredient> result = new ArrayList<>();
        for (SystemDishIngredient ingredient : systemIngredients) {
            BigDecimal amount = ingredient.getAmount();
            String unit = ingredient.getUnit();
            String amountText = ingredient.getAmountText();
            if (amount == null || unit == null || unit.isBlank()) {
                CopyToFamilyRequest.IngredientOverride override = overrideBySortNo.get(ingredient.getSortNo());
                if (override != null && override.getAmount() != null
                        && override.getUnit() != null && !override.getUnit().isBlank()) {
                    amount = override.getAmount();
                    unit = override.getUnit();
                    amountText = null;
                }
                // 无 override（或 override 不完整）时保留原文：amount/unit 留空，amountText 原样保留
            }
            result.add(DishIngredient.builder()
                    .name(ingredient.getName())
                    .amount(amount)
                    .unit(unit == null ? null : unit.trim())
                    .amountText(amountText)
                    .sortNo(ingredient.getSortNo())
                    .build());
        }
        return result;
    }

    private Map<Long, String> categoryNamesOf(List<SystemDish> dishes) {
        List<Long> categoryIds = dishes.stream()
                .map(SystemDish::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return systemDishCategoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(SystemDishCategory::getId, SystemDishCategory::getName));
    }

    private boolean isUncategorized(Long cuisineId) {
        if (cuisineId == null) {
            return false;
        }
        return cuisineRepository.findById(cuisineId)
                .map(c -> c.getSortNo() == Cuisine.UNCATEGORIZED_SORT_NO)
                .orElse(false);
    }

    private String cuisineNameOf(Long cuisineId) {
        if (cuisineId == null) {
            return null;
        }
        return cuisineRepository.findById(cuisineId).map(Cuisine::getName).orElse(null);
    }

    private Map<Long, String> cuisineNamesOf(List<SystemDish> dishes) {
        List<Long> cuisineIds = dishes.stream()
                .map(SystemDish::getCuisineId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (cuisineIds.isEmpty()) {
            return Map.of();
        }
        return cuisineRepository.findAllById(cuisineIds).stream()
                .collect(Collectors.toMap(Cuisine::getId, Cuisine::getName));
    }

    private SystemDishSummaryResponse toSummaryResponse(SystemDish dish, Map<Long, String> categoryNames,
            Map<Long, String> cuisineNames) {
        return SystemDishSummaryResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .description(dish.getDescription())
                .coverImageUrl(dish.getCoverImageUrl())
                .categoryId(dish.getCategoryId())
                .categoryName(dish.getCategoryId() == null ? null : categoryNames.get(dish.getCategoryId()))
                .cuisineId(dish.getCuisineId())
                .cuisineName(dish.getCuisineId() == null ? null : cuisineNames.get(dish.getCuisineId()))
                .cookingMinutes(dish.getCookingMinutes())
                .difficulty(dish.getDifficulty())
                .calories(dish.getCalories())
                .build();
    }

    private SystemDishDetailResponse toDetailResponse(SystemDish dish) {
        String categoryName = dish.getCategoryId() == null ? null
                : systemDishCategoryRepository.findById(dish.getCategoryId())
                        .map(SystemDishCategory::getName)
                        .orElse(null);
        return SystemDishDetailResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .description(dish.getDescription())
                .coverImageUrl(dish.getCoverImageUrl())
                .categoryId(dish.getCategoryId())
                .categoryName(categoryName)
                .cuisineId(dish.getCuisineId())
                .cuisineName(cuisineNameOf(dish.getCuisineId()))
                .cookingMinutes(dish.getCookingMinutes())
                .difficulty(dish.getDifficulty())
                .calories(dish.getCalories())
                .sourceType(dish.getSourceType())
                .sourceUrl(dish.getSourceUrl())
                .originalPath(dish.getOriginalPath())
                .sourceCommit(dish.getSourceCommit())
                .license(dish.getLicense())
                .ingredients(systemDishIngredientRepository.findByDishIdOrderBySortNoAsc(dish.getId()).stream()
                        .map(item -> SystemDishDetailResponse.IngredientItem.builder()
                                .name(item.getName())
                                .amount(item.getAmount())
                                .unit(item.getUnit())
                                .amountText(item.getAmountText())
                                .optional(item.isOptional())
                                .sortNo(item.getSortNo())
                                .build())
                        .toList())
                .steps(systemDishStepRepository.findByDishIdOrderByStepNoAsc(dish.getId()).stream()
                        .map(step -> SystemDishDetailResponse.StepItem.builder()
                                .stepNo(step.getStepNo())
                                .content(step.getContent())
                                .imageUrl(step.getImageUrl())
                                .build())
                        .toList())
                .build();
    }

    private DishResponse buildDishResponse(Dish saved, DishCategory familyCategory,
            List<DishIngredient> resolvedIngredients, List<SystemDishStep> systemSteps, String creatorName) {
        return DishResponse.builder()
                .id(saved.getId())
                .familyId(saved.getFamilyId())
                .name(saved.getName())
                .description(saved.getDescription())
                .imageUrl(saved.getImageUrl())
                .categoryId(familyCategory == null ? null : familyCategory.getId())
                .categoryName(familyCategory == null ? null : familyCategory.getName())
                .cuisineId(saved.getCuisineId())
                .cuisineName(cuisineNameOf(saved.getCuisineId()))
                .cookingMinutes(saved.getCookingMinutes())
                .calories(saved.getCalories())
                .price(saved.getPrice())
                .createdBy(saved.getCreatedBy())
                .createdByName(creatorName)
                .available(saved.isAvailable())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .ingredients(resolvedIngredients.stream()
                        .map(item -> DishResponse.IngredientItem.builder()
                                .name(item.getName())
                                .amount(item.getAmount())
                                .unit(item.getUnit())
                                .amountText(item.getAmountText())
                                .build())
                        .toList())
                .steps(systemSteps.stream()
                        .map(step -> DishResponse.StepItem.builder()
                                .stepNo(step.getStepNo())
                                .content(step.getContent())
                                .imageUrl(step.getImageUrl())
                                .build())
                        .toList())
                .build();
    }
}
