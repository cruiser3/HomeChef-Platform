package com.fooddelivery.service.impl;

import com.fooddelivery.dto.DishCategoryRequest;
import com.fooddelivery.dto.DishCategoryResponse;
import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.DishCategory;
import com.fooddelivery.entity.DishIngredient;
import com.fooddelivery.entity.DishStep;
import com.fooddelivery.entity.FamilyMember;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.DishCategoryRepository;
import com.fooddelivery.repository.DishIngredientRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.DishStepRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final DishStepRepository dishStepRepository;
    private final DishCategoryRepository dishCategoryRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    public DishResponse create(Long familyId, DishRequest request) {
        currentUserService.requireMembership(familyId);
        Long creatorId = currentUserService.getCurrentUserId();

        validateDishRequest(request);
        validateCategory(familyId, request.getCategoryId());
        List<DishRequest.DishStepRequest> steps = normalizeSteps(request.getSteps());

        Dish dish = Dish.builder()
                .familyId(familyId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .categoryId(request.getCategoryId())
                .cookingMinutes(request.getCookingMinutes())
                .price(request.getPrice())
                .createdBy(creatorId)
                .available(true)
                .build();
        Dish saved = dishRepository.save(dish);

        saveIngredients(saved.getId(), request.getIngredients());
        saveSteps(saved.getId(), steps);

        DishResponse response = buildResponse(saved, currentUserName());
        response.setCategoryName(categoryNameOf(familyId, saved.getCategoryId()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishResponse> list(Long familyId, Long categoryId, String keyword, Boolean available) {
        currentUserService.requireMembership(familyId);

        List<Dish> dishes;
        if (keyword != null && !keyword.isBlank()) {
            dishes = dishRepository.findByFamilyIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(familyId, keyword.trim());
            if (categoryId != null) {
                dishes = dishes.stream().filter(d -> categoryId.equals(d.getCategoryId())).toList();
            }
        } else if (categoryId != null) {
            dishes = dishRepository.findByFamilyIdAndCategoryIdOrderByCreatedAtDesc(familyId, categoryId);
        } else if (available != null) {
            dishes = dishRepository.findByFamilyIdAndAvailableOrderByCreatedAtDesc(familyId, available);
        } else {
            dishes = dishRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        }

        if (keyword != null && available != null) {
            boolean target = available;
            dishes = dishes.stream().filter(d -> d.isAvailable() == target).toList();
        }

        Map<Long, String> categoryNames = categoryNamesOf(familyId);
        Map<Long, String> creatorNames = creatorNamesOf(dishes);

        return dishes.stream()
                .map(dish -> toSummaryResponse(dish, categoryNames, creatorNames))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DishResponse getDetail(Long familyId, Long dishId) {
        currentUserService.requireMembership(familyId);
        Dish dish = findDishInFamily(familyId, dishId);

        DishResponse response = buildResponse(dish, creatorNameOf(dish.getCreatedBy()));
        response.setCategoryName(categoryNameOf(familyId, dish.getCategoryId()));
        return response;
    }

    @Override
    public DishResponse update(Long familyId, Long dishId, DishRequest request) {
        currentUserService.requireMembership(familyId);
        Dish dish = findDishInFamily(familyId, dishId);
        requireModifyPermission(familyId, dish);

        validateDishRequest(request);
        validateCategory(familyId, request.getCategoryId());
        List<DishRequest.DishStepRequest> steps = normalizeSteps(request.getSteps());

        dish.setName(request.getName().trim());
        dish.setDescription(request.getDescription());
        dish.setImageUrl(request.getImageUrl());
        dish.setCategoryId(request.getCategoryId());
        dish.setCookingMinutes(request.getCookingMinutes());
        dish.setPrice(request.getPrice());
        dish.setUpdatedAt(LocalDateTime.now());
        Dish saved = dishRepository.save(dish);

        dishIngredientRepository.deleteByDishId(dishId);
        dishStepRepository.deleteByDishId(dishId);
        saveIngredients(dishId, request.getIngredients());
        saveSteps(dishId, steps);

        DishResponse response = buildResponse(saved, creatorNameOf(saved.getCreatedBy()));
        response.setCategoryName(categoryNameOf(familyId, saved.getCategoryId()));
        return response;
    }

    @Override
    public DishResponse changeAvailability(Long familyId, Long dishId, boolean available) {
        currentUserService.requireMembership(familyId);
        Dish dish = findDishInFamily(familyId, dishId);
        requireModifyPermission(familyId, dish);

        dish.setAvailable(available);
        dish.setUpdatedAt(LocalDateTime.now());
        Dish saved = dishRepository.save(dish);

        DishResponse response = buildResponse(saved, creatorNameOf(saved.getCreatedBy()));
        response.setCategoryName(categoryNameOf(familyId, saved.getCategoryId()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishCategoryResponse> listCategories(Long familyId) {
        currentUserService.requireMembership(familyId);
        return dishCategoryRepository.findByFamilyIdOrderBySortNoAsc(familyId).stream()
                .map(category -> DishCategoryResponse.builder()
                        .id(category.getId())
                        .familyId(category.getFamilyId())
                        .name(category.getName())
                        .sortNo(category.getSortNo())
                        .build())
                .toList();
    }

    @Override
    public DishCategoryResponse createCategory(Long familyId, DishCategoryRequest request) {
        currentUserService.requireMembership(familyId);
        String name = request.getName().trim();
        if (dishCategoryRepository.existsByFamilyIdAndName(familyId, name)) {
            throw new BadRequestException("Category already exists in this family: " + name);
        }
        DishCategory category = DishCategory.builder()
                .familyId(familyId)
                .name(name)
                .sortNo(request.getSortNo() == null ? 0 : request.getSortNo())
                .build();
        DishCategory saved = dishCategoryRepository.save(category);
        return DishCategoryResponse.builder()
                .id(saved.getId())
                .familyId(saved.getFamilyId())
                .name(saved.getName())
                .sortNo(saved.getSortNo())
                .build();
    }

    private void validateDishRequest(DishRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Dish name is required");
        }
        if (request.getPrice() == null) {
            throw new BadRequestException("Price is required");
        }
        if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Price must be zero or positive");
        }
        if (request.getCookingMinutes() != null && request.getCookingMinutes() < 0) {
            throw new BadRequestException("Cooking minutes must be zero or positive");
        }
        if (request.getIngredients() != null) {
            for (DishRequest.DishIngredientRequest ingredient : request.getIngredients()) {
                if (ingredient == null || ingredient.getName() == null || ingredient.getName().isBlank()) {
                    throw new BadRequestException("Ingredient name is required");
                }
                if (ingredient.getAmount() == null) {
                    throw new BadRequestException("Ingredient amount is required");
                }
                if (ingredient.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException("Ingredient amount must be zero or positive");
                }
                if (ingredient.getUnit() == null || ingredient.getUnit().isBlank()) {
                    throw new BadRequestException("Ingredient unit is required");
                }
            }
        }
        if (request.getSteps() != null) {
            for (DishRequest.DishStepRequest step : request.getSteps()) {
                if (step == null || step.getStepNo() == null || step.getStepNo() < 1) {
                    throw new BadRequestException("Step number must start from 1");
                }
                if (step.getContent() == null || step.getContent().isBlank()) {
                    throw new BadRequestException("Step content is required");
                }
            }
        }
    }

    private void requireModifyPermission(Long familyId, Dish dish) {
        Long currentUserId = currentUserService.getCurrentUserId();
        if (dish.getCreatedBy().equals(currentUserId)) {
            return;
        }
        FamilyMember membership = currentUserService.requireMembership(familyId);
        if (membership.getRole() != FamilyMember.FamilyRole.ADMIN) {
            throw new ForbiddenException("Only the creator or a family admin can modify this dish");
        }
    }

    private Dish findDishInFamily(Long familyId, Long dishId) {
        return dishRepository.findByIdAndFamilyId(dishId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + dishId));
    }

    private void validateCategory(Long familyId, Long categoryId) {
        if (categoryId != null && dishCategoryRepository.findByIdAndFamilyId(categoryId, familyId).isEmpty()) {
            throw new BadRequestException("Category does not belong to this family: " + categoryId);
        }
    }

    private List<DishRequest.DishStepRequest> normalizeSteps(List<DishRequest.DishStepRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        List<DishRequest.DishStepRequest> sorted = steps.stream()
                .sorted(Comparator.comparing(DishRequest.DishStepRequest::getStepNo))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getStepNo() != i + 1) {
                throw new BadRequestException("Step numbers must be continuous starting from 1");
            }
        }
        return sorted;
    }

    private void saveIngredients(Long dishId, List<DishRequest.DishIngredientRequest> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        for (int i = 0; i < ingredients.size(); i++) {
            DishRequest.DishIngredientRequest item = ingredients.get(i);
            dishIngredientRepository.save(DishIngredient.builder()
                    .dishId(dishId)
                    .name(item.getName().trim())
                    .amount(item.getAmount())
                    .unit(item.getUnit().trim())
                    .sortNo(i)
                    .build());
        }
    }

    private void saveSteps(Long dishId, List<DishRequest.DishStepRequest> steps) {
        for (DishRequest.DishStepRequest step : steps) {
            dishStepRepository.save(DishStep.builder()
                    .dishId(dishId)
                    .stepNo(step.getStepNo())
                    .content(step.getContent().trim())
                    .imageUrl(step.getImageUrl())
                    .build());
        }
    }

    private DishResponse buildResponse(Dish dish, String creatorName) {
        return DishResponse.builder()
                .id(dish.getId())
                .familyId(dish.getFamilyId())
                .name(dish.getName())
                .description(dish.getDescription())
                .imageUrl(dish.getImageUrl())
                .categoryId(dish.getCategoryId())
                .cookingMinutes(dish.getCookingMinutes())
                .price(dish.getPrice())
                .createdBy(dish.getCreatedBy())
                .createdByName(creatorName)
                .available(dish.isAvailable())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .ingredients(dishIngredientRepository.findByDishIdOrderBySortNoAsc(dish.getId()).stream()
                        .map(item -> DishResponse.IngredientItem.builder()
                                .name(item.getName())
                                .amount(item.getAmount())
                                .unit(item.getUnit())
                                .build())
                        .toList())
                .steps(dishStepRepository.findByDishIdOrderByStepNoAsc(dish.getId()).stream()
                        .map(step -> DishResponse.StepItem.builder()
                                .stepNo(step.getStepNo())
                                .content(step.getContent())
                                .imageUrl(step.getImageUrl())
                                .build())
                        .toList())
                .build();
    }

    private DishResponse toSummaryResponse(Dish dish, Map<Long, String> categoryNames, Map<Long, String> creatorNames) {
        return DishResponse.builder()
                .id(dish.getId())
                .familyId(dish.getFamilyId())
                .name(dish.getName())
                .description(dish.getDescription())
                .imageUrl(dish.getImageUrl())
                .categoryId(dish.getCategoryId())
                .categoryName(dish.getCategoryId() == null ? null : categoryNames.get(dish.getCategoryId()))
                .cookingMinutes(dish.getCookingMinutes())
                .price(dish.getPrice())
                .createdBy(dish.getCreatedBy())
                .createdByName(creatorNames.get(dish.getCreatedBy()))
                .available(dish.isAvailable())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .build();
    }

    private String categoryNameOf(Long familyId, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return dishCategoryRepository.findByIdAndFamilyId(categoryId, familyId)
                .map(DishCategory::getName)
                .orElse(null);
    }

    private Map<Long, String> categoryNamesOf(Long familyId) {
        return dishCategoryRepository.findByFamilyIdOrderBySortNoAsc(familyId).stream()
                .collect(Collectors.toMap(DishCategory::getId, DishCategory::getName));
    }

    private Map<Long, String> creatorNamesOf(List<Dish> dishes) {
        List<Long> creatorIds = dishes.stream().map(Dish::getCreatedBy).distinct().toList();
        return userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    private String creatorNameOf(Long creatorId) {
        return userRepository.findById(creatorId).map(User::getName).orElse(null);
    }

    private String currentUserName() {
        return currentUserService.getCurrentUser().getName();
    }
}