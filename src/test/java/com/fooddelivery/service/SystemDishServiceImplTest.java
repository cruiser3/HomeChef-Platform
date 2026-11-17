package com.fooddelivery.service;

import com.fooddelivery.dto.CopyToFamilyRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.dto.SystemDishDetailResponse;
import com.fooddelivery.dto.SystemDishSummaryResponse;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.DishCategory;
import com.fooddelivery.entity.SystemDish;
import com.fooddelivery.entity.SystemDishCategory;
import com.fooddelivery.entity.SystemDishIngredient;
import com.fooddelivery.entity.SystemDishStep;
import com.fooddelivery.entity.User;
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
import com.fooddelivery.service.impl.SystemDishServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemDishServiceImplTest {

    @Mock private SystemDishRepository systemDishRepository;
    @Mock private CuisineRepository cuisineRepository;
    @Mock private SystemDishIngredientRepository systemDishIngredientRepository;
    @Mock private SystemDishStepRepository systemDishStepRepository;
    @Mock private SystemDishReviewRepository systemDishReviewRepository;
    @Mock private SystemDishCategoryRepository systemDishCategoryRepository;
    @Mock private FamilyDishCategoryResolver familyDishCategoryResolver;
    @Mock private DishRepository dishRepository;
    @Mock private DishIngredientRepository dishIngredientRepository;
    @Mock private DishStepRepository dishStepRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private FamilyRepository familyRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private SystemDishServiceImpl service;

    @Test
    void list_returnsOnlyAvailableDishesWithCategoryName() {
        SystemDish dish = SystemDish.builder().id(1L).name("红烧肉").categoryId(10L).build();
        SystemDishCategory category = SystemDishCategory.builder().id(10L).name("荤菜").build();
        when(systemDishRepository.findByAvailableTrueOrderByIdDesc()).thenReturn(List.of(dish));
        when(systemDishCategoryRepository.findAllById(List.of(10L))).thenReturn(List.of(category));

        List<SystemDishSummaryResponse> result = service.list(null, null, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("红烧肉", result.get(0).getName());
        assertEquals("荤菜", result.get(0).getCategoryName());
    }

    @Test
    void getDetail_returnsIngredientsAndSteps() {
        SystemDish dish = SystemDish.builder().id(1L).name("红烧肉").sourceType("HOWTOCOOK").build();
        SystemDishIngredient ingredient = SystemDishIngredient.builder()
                .dishId(1L).name("五花肉").amount(new BigDecimal("500")).unit("g").sortNo(0).build();
        SystemDishStep step = SystemDishStep.builder().dishId(1L).stepNo(1).content("焯水").build();
        when(systemDishRepository.findByIdAndAvailableTrue(1L)).thenReturn(Optional.of(dish));
        when(systemDishIngredientRepository.findByDishIdOrderBySortNoAsc(1L)).thenReturn(List.of(ingredient));
        when(systemDishStepRepository.findByDishIdOrderByStepNoAsc(1L)).thenReturn(List.of(step));

        SystemDishDetailResponse response = service.getDetail(1L);

        assertEquals("红烧肉", response.getName());
        assertEquals("HOWTOCOOK", response.getSourceType());
        assertEquals(1, response.getIngredients().size());
        assertEquals(1, response.getSteps().size());
        assertEquals("焯水", response.getSteps().get(0).getContent());
    }

    @Test
    void getDetail_notFoundOrUnavailable_throws() {
        when(systemDishRepository.findByIdAndAvailableTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDetail(1L));
    }

    @Test
    void copyToFamily_success_mapsCategoryAndCopiesIngredientsAndSteps() {
        SystemDish systemDish = SystemDish.builder()
                .id(1L).name("红烧肉").categoryId(10L).cookingMinutes(90).build();
        SystemDishCategory systemCategory = SystemDishCategory.builder().id(10L).name("荤菜").build();
        DishCategory familyCategory = DishCategory.builder().id(20L).familyId(1L).name("荤菜").build();
        SystemDishIngredient ingredient = SystemDishIngredient.builder()
                .dishId(1L).name("五花肉").amount(new BigDecimal("500")).unit("g").sortNo(0).build();
        SystemDishStep step = SystemDishStep.builder().dishId(1L).stepNo(1).content("焯水").build();
        Dish savedDish = Dish.builder().id(100L).familyId(1L).name("红烧肉")
                .cookingMinutes(90).price(new BigDecimal("38.00")).createdBy(1L).available(true).build();
        User user = mock(User.class);

        when(currentUserService.requireMembership(1L)).thenReturn(null);
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(user.getName()).thenReturn("Alice");
        when(systemDishRepository.findByIdAndAvailableTrue(1L)).thenReturn(Optional.of(systemDish));
        when(systemDishIngredientRepository.findByDishIdOrderBySortNoAsc(1L)).thenReturn(List.of(ingredient));
        when(systemDishStepRepository.findByDishIdOrderByStepNoAsc(1L)).thenReturn(List.of(step));
        when(systemDishCategoryRepository.findById(10L)).thenReturn(Optional.of(systemCategory));
        when(familyDishCategoryResolver.getOrCreate(1L, "荤菜")).thenReturn(familyCategory);
        when(dishRepository.save(any(Dish.class))).thenReturn(savedDish);

        CopyToFamilyRequest request = new CopyToFamilyRequest();
        request.setPrice(new BigDecimal("38.00"));

        DishResponse response = service.copyToFamily(1L, 1L, request);

        assertEquals("红烧肉", response.getName());
        assertEquals(20L, response.getCategoryId());
        assertEquals("荤菜", response.getCategoryName());
        assertEquals("Alice", response.getCreatedByName());
        assertEquals(1, response.getIngredients().size());
        assertEquals(1, response.getSteps().size());
        verify(dishIngredientRepository).save(any(com.fooddelivery.entity.DishIngredient.class));
        verify(dishStepRepository).save(any(com.fooddelivery.entity.DishStep.class));
    }

    @Test
    void copyToFamily_unmeasurableIngredientWithoutOverride_keepsAmountText() {
        SystemDish systemDish = SystemDish.builder().id(1L).name("红烧肉").categoryId(null).build();
        SystemDishIngredient ingredient = SystemDishIngredient.builder()
                .dishId(1L).name("盐").amount(null).unit(null).amountText("适量").sortNo(0).build();
        Dish savedDish = Dish.builder().id(100L).familyId(1L).name("红烧肉").createdBy(1L).available(true).build();
        User user = mock(User.class);

        when(currentUserService.requireMembership(1L)).thenReturn(null);
        when(currentUserService.getCurrentUserId()).thenReturn(1L);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(user.getName()).thenReturn("Alice");
        when(systemDishRepository.findByIdAndAvailableTrue(1L)).thenReturn(Optional.of(systemDish));
        when(systemDishIngredientRepository.findByDishIdOrderBySortNoAsc(1L)).thenReturn(List.of(ingredient));
        when(systemDishStepRepository.findByDishIdOrderByStepNoAsc(1L)).thenReturn(List.of());
        when(dishRepository.save(any(Dish.class))).thenReturn(savedDish);

        CopyToFamilyRequest request = new CopyToFamilyRequest();
        request.setPrice(new BigDecimal("38.00"));

        DishResponse response = service.copyToFamily(1L, 1L, request);

        assertEquals(1, response.getIngredients().size());
        assertEquals("盐", response.getIngredients().get(0).getName());
        assertNull(response.getIngredients().get(0).getAmount());
        assertNull(response.getIngredients().get(0).getUnit());
        assertEquals("适量", response.getIngredients().get(0).getAmountText());
        verify(dishIngredientRepository).save(any(com.fooddelivery.entity.DishIngredient.class));
    }

    @Test
    void copyToFamily_nonMember_throwsForbidden() {
        when(currentUserService.requireMembership(1L))
                .thenThrow(new ForbiddenException("You are not a member of this family"));

        CopyToFamilyRequest request = new CopyToFamilyRequest();
        request.setPrice(new BigDecimal("38.00"));

        assertThrows(ForbiddenException.class, () -> service.copyToFamily(1L, 1L, request));
    }
}
