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
import com.fooddelivery.entity.Family;
import com.fooddelivery.entity.SystemDish;
import com.fooddelivery.entity.SystemDishCategory;
import com.fooddelivery.entity.SystemDishIngredient;
import com.fooddelivery.entity.SystemDishReview;
import com.fooddelivery.entity.SystemDishStep;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CuisineRepository;
import com.fooddelivery.repository.DishIngredientRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.DishStepRepository;
import com.fooddelivery.repository.FamilyRepository;
import com.fooddelivery.repository.SystemDishCategoryRepository;
import com.fooddelivery.repository.SystemDishIngredientRepository;
import com.fooddelivery.repository.SystemDishRepository;
import com.fooddelivery.repository.SystemDishReviewRepository;
import com.fooddelivery.repository.SystemDishStepRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.FamilyDishCategoryResolver;
import com.fooddelivery.service.SystemDishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final SystemDishReviewRepository systemDishReviewRepository;
    private final SystemDishCategoryRepository systemDishCategoryRepository;
    private final FamilyDishCategoryResolver familyDishCategoryResolver;
    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final DishStepRepository dishStepRepository;
    private final CurrentUserService currentUserService;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

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
    public List<SystemDishSummaryResponse> list(String tab, Long categoryId, Long cuisineId, Integer caloriesMin,
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

        // 「最新」Tab：仅用户发布的家庭菜品快照。
        if ("latest".equals(tab)) {
            dishes = dishes.stream()
                    .filter(d -> SystemDish.SOURCE_TYPE_FAMILY.equals(d.getSourceType()))
                    .toList();
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
        Map<Long, String> familyNames = familyNamesOf(dishes);
        Map<Long, User> publishers = publishersOf(dishes);
        Map<Long, double[]> ratingStats = ratingStatsOf(dishes);
        return dishes.stream()
                .map(dish -> {
                    SystemDishSummaryResponse response =
                            toSummaryResponse(dish, categoryNames, cuisineNames, familyNames, publishers);
                    applyRating(response, ratingStats.get(dish.getId()));
                    return response;
                })
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

    private SystemDish findSystemDish(Long id) {
        return systemDishRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System dish not found with id: " + id));
    }

    @Override
    public SystemDishDetailResponse publishToSquare(Long familyId, Long dishId, Long categoryId) {
        currentUserService.requireMembership(familyId);
        Dish dish = dishRepository.findByIdAndFamilyId(dishId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + dishId));
        if (categoryId == null || !systemDishCategoryRepository.existsById(categoryId)) {
            throw new BadRequestException("A valid system category is required to publish");
        }

        Long userId = currentUserService.getCurrentUserId();
        // 唯一标识：同一家庭同一菜品只保留一条广场副本，重复发布即覆盖更新。
        String originalPath = "family-" + familyId + "-dish-" + dishId;
        SystemDish existing = systemDishRepository
                .findBySourceTypeAndOriginalPath(SystemDish.SOURCE_TYPE_FAMILY, originalPath)
                .orElse(null);

        List<DishIngredient> ingredients = dishIngredientRepository.findByDishIdOrderBySortNoAsc(dishId);
        List<DishStep> steps = dishStepRepository.findByDishIdOrderByStepNoAsc(dishId);

        SystemDish saved;
        if (existing == null) {
            saved = systemDishRepository.save(SystemDish.builder()
                    .categoryId(categoryId)
                    .cuisineId(dish.getCuisineId())
                    .name(dish.getName())
                    .description(dish.getDescription())
                    .coverImageUrl(dish.getImageUrl())
                    .cookingMinutes(dish.getCookingMinutes())
                    .difficulty(null)
                    .calories(dish.getCalories())
                    .sourceType(SystemDish.SOURCE_TYPE_FAMILY)
                    .sourceUrl(null)
                    .originalPath(originalPath)
                    .sourceCommit(null)
                    .license(null)
                    .sourceFamilyId(familyId)
                    .publishedBy(userId)
                    .available(true)
                    .build());
        } else {
            existing.setCategoryId(categoryId);
            existing.setCuisineId(dish.getCuisineId());
            existing.setName(dish.getName());
            existing.setDescription(dish.getDescription());
            existing.setCoverImageUrl(dish.getImageUrl());
            existing.setCookingMinutes(dish.getCookingMinutes());
            existing.setCalories(dish.getCalories());
            existing.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            saved = systemDishRepository.save(existing);
            systemDishIngredientRepository.deleteByDishId(saved.getId());
            systemDishStepRepository.deleteByDishId(saved.getId());
        }

        for (int i = 0; i < ingredients.size(); i++) {
            DishIngredient ingredient = ingredients.get(i);
            systemDishIngredientRepository.save(SystemDishIngredient.builder()
                    .dishId(saved.getId())
                    .name(ingredient.getName())
                    .amount(ingredient.getAmount())
                    .unit(ingredient.getUnit())
                    .amountText(ingredient.getAmountText())
                    .optional(false)
                    .sortNo(i)
                    .build());
        }

        for (DishStep step : steps) {
            systemDishStepRepository.save(SystemDishStep.builder()
                    .dishId(saved.getId())
                    .stepNo(step.getStepNo())
                    .content(step.getContent())
                    .imageUrl(step.getImageUrl())
                    .build());
        }

        return toDetailResponse(saved);
    }

    @Override
    public SystemDishDetailResponse setAvailability(Long id, boolean available) {
        SystemDish dish = findSystemDish(id);
        requirePublishManagePermission(dish);
        dish.setAvailable(available);
        dish.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return toDetailResponse(systemDishRepository.save(dish));
    }

    @Override
    public void withdrawPublish(Long id) {
        SystemDish dish = findSystemDish(id);
        requirePublishManagePermission(dish);
        systemDishIngredientRepository.deleteByDishId(id);
        systemDishStepRepository.deleteByDishId(id);
        systemDishRepository.delete(dish);
    }

    /**
     * 校验当前用户是否有权管理（下架/撤回）某个发布到广场的家庭菜品：
     * 仅发布者本人、系统管理员，或来源家庭的管理员。
     */
    private void requirePublishManagePermission(SystemDish dish) {
        if (!SystemDish.SOURCE_TYPE_FAMILY.equals(dish.getSourceType())) {
            throw new ForbiddenException("Only published family dishes can be managed");
        }
        User current = currentUserService.getCurrentUser();
        if (current.getId().equals(dish.getPublishedBy())) {
            return;
        }
        if (current.getRole() == User.Role.ADMIN) {
            return;
        }
        if (dish.getSourceFamilyId() != null) {
            currentUserService.requireAdmin(dish.getSourceFamilyId());
            return;
        }
        throw new ForbiddenException("You are not allowed to manage this published dish");
    }

    private Map<Long, String> familyNamesOf(List<SystemDish> dishes) {
        List<Long> familyIds = dishes.stream()
                .map(SystemDish::getSourceFamilyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (familyIds.isEmpty()) {
            return Map.of();
        }
        return familyRepository.findAllById(familyIds).stream()
                .collect(Collectors.toMap(Family::getId, Family::getName));
    }

    private Map<Long, User> publishersOf(List<SystemDish> dishes) {
        List<Long> userIds = dishes.stream()
                .map(SystemDish::getPublishedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
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
            Map<Long, String> cuisineNames, Map<Long, String> familyNames, Map<Long, User> publishers) {
        User publisher = dish.getPublishedBy() == null ? null : publishers.get(dish.getPublishedBy());
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
                .sourceType(dish.getSourceType())
                .sourceFamilyId(dish.getSourceFamilyId())
                .publishedBy(dish.getPublishedBy())
                .familyName(dish.getSourceFamilyId() == null ? null : familyNames.get(dish.getSourceFamilyId()))
                .publisherName(publisher == null ? null : publisher.getName())
                .publisherAvatarUrl(publisher == null ? null : publisher.getAvatarUrl())
                .build();
    }

    private Map<Long, double[]> ratingStatsOf(List<SystemDish> dishes) {
        List<Long> dishIds = dishes.stream().map(SystemDish::getId).toList();
        if (dishIds.isEmpty()) {
            return Map.of();
        }
        List<SystemDishReview> reviews = systemDishReviewRepository.findByDishIdInAndParentIdIsNull(dishIds);
        Map<Long, double[]> stats = new HashMap<>();
        for (SystemDishReview review : reviews) {
            if (review.getRating() == null) {
                continue;
            }
            double[] stat = stats.computeIfAbsent(review.getDishId(), k -> new double[2]);
            stat[0] += review.getRating();
            stat[1] += 1;
        }
        return stats;
    }

    private void applyRating(SystemDishSummaryResponse response, double[] stat) {
        if (stat == null || stat[1] == 0) {
            response.setAverageRating(null);
            response.setReviewCount(0);
            return;
        }
        response.setAverageRating(stat[0] / stat[1]);
        response.setReviewCount((int) stat[1]);
    }

    private void applyRating(SystemDishDetailResponse response, double[] stat) {
        if (stat == null || stat[1] == 0) {
            response.setAverageRating(null);
            response.setReviewCount(0);
            return;
        }
        response.setAverageRating(stat[0] / stat[1]);
        response.setReviewCount((int) stat[1]);
    }

    private SystemDishDetailResponse toDetailResponse(SystemDish dish) {
        String categoryName = dish.getCategoryId() == null ? null
                : systemDishCategoryRepository.findById(dish.getCategoryId())
                        .map(SystemDishCategory::getName)
                        .orElse(null);
        String familyName = dish.getSourceFamilyId() == null ? null
                : familyRepository.findById(dish.getSourceFamilyId()).map(Family::getName).orElse(null);
        User publisher = dish.getPublishedBy() == null ? null
                : userRepository.findById(dish.getPublishedBy()).orElse(null);
        SystemDishDetailResponse response = SystemDishDetailResponse.builder()
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
                .sourceFamilyId(dish.getSourceFamilyId())
                .publishedBy(dish.getPublishedBy())
                .familyName(familyName)
                .publisherName(publisher == null ? null : publisher.getName())
                .publisherAvatarUrl(publisher == null ? null : publisher.getAvatarUrl())
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
        applyRating(response, ratingStatsOf(List.of(dish)).get(dish.getId()));
        return response;
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
