package com.fooddelivery.service.impl;

import com.fooddelivery.dto.AddCartItemRequest;
import com.fooddelivery.dto.CartResponse;
import com.fooddelivery.dto.UpdateCartItemRequest;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.service.CartService;
import com.fooddelivery.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 99;

    private final CartItemRepository cartItemRepository;
    private final DishRepository dishRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long familyId) {
        currentUserService.requireMembership(familyId);
        return buildCart(familyId, currentUserService.getCurrentUserId());
    }

    @Override
    public CartResponse addItem(Long familyId, AddCartItemRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        if (request == null || request.getDishId() == null) {
            throw new BadRequestException("Dish id is required");
        }
        validateQuantity(request.getQuantity());

        Dish dish = requireAvailableFamilyDish(familyId, request.getDishId());

        CartItem item = cartItemRepository.findByUserIdAndFamilyIdAndDishId(userId, familyId, dish.getId())
                .orElseGet(() -> CartItem.builder()
                        .userId(userId)
                        .familyId(familyId)
                        .dishId(dish.getId())
                        .quantity(0)
                        .build());

        int merged = item.getQuantity() + request.getQuantity();
        validateQuantity(merged);
        item.setQuantity(merged);
        item.setUpdatedAt(LocalDateTime.now());
        cartItemRepository.save(item);

        return buildCart(familyId, userId);
    }

    @Override
    public CartResponse updateItem(Long familyId, Long itemId, UpdateCartItemRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        if (request == null || request.getQuantity() == null) {
            throw new BadRequestException("Quantity is required");
        }
        validateQuantity(request.getQuantity());

        CartItem item = requireOwnCartItem(familyId, userId, itemId);
        requireAvailableFamilyDish(familyId, item.getDishId());

        item.setQuantity(request.getQuantity());
        item.setUpdatedAt(LocalDateTime.now());
        cartItemRepository.save(item);

        return buildCart(familyId, userId);
    }

    @Override
    public CartResponse removeItem(Long familyId, Long itemId) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        CartItem item = requireOwnCartItem(familyId, userId, itemId);
        cartItemRepository.delete(item);

        return buildCart(familyId, userId);
    }

    @Override
    public CartResponse clear(Long familyId) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        cartItemRepository.deleteByUserIdAndFamilyId(userId, familyId);

        return buildCart(familyId, userId);
    }

    private CartItem requireOwnCartItem(Long familyId, Long userId, Long itemId) {
        return cartItemRepository.findByIdAndUserIdAndFamilyId(itemId, userId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));
    }

    private Dish requireAvailableFamilyDish(Long familyId, Long dishId) {
        Dish dish = dishRepository.findByIdAndFamilyId(dishId, familyId)
                .orElseThrow(() -> new BadRequestException("Dish does not belong to the current family"));
        if (!dish.isAvailable()) {
            throw new BadRequestException("Dish is not available for ordering");
        }
        return dish;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new BadRequestException("Quantity is required");
        }
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new BadRequestException("Quantity must be between " + MIN_QUANTITY + " and " + MAX_QUANTITY);
        }
    }

    private CartResponse buildCart(Long familyId, Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdAndFamilyIdOrderByCreatedAtAsc(userId, familyId);

        Map<Long, Dish> dishes = dishRepository.findAllById(
                        cartItems.stream().map(CartItem::getDishId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));

        List<CartResponse.CartItemResponse> items = cartItems.stream()
                .filter(item -> dishes.containsKey(item.getDishId()))
                .sorted(Comparator.comparing(CartItem::getCreatedAt))
                .map(item -> toItemResponse(item, dishes.get(item.getDishId())))
                .toList();

        BigDecimal total = items.stream()
                .map(CartResponse.CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .userId(userId)
                .familyId(familyId)
                .itemCount(items.size())
                .totalAmount(total)
                .items(items)
                .build();
    }

    private CartResponse.CartItemResponse toItemResponse(CartItem item, Dish dish) {
        BigDecimal unitPrice = dish.getPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartResponse.CartItemResponse.builder()
                .id(item.getId())
                .dishId(dish.getId())
                .dishName(dish.getName())
                .imageUrl(dish.getImageUrl())
                .unitPrice(unitPrice)
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .dishAvailable(dish.isAvailable())
                .createdAt(item.getCreatedAt())
                .build();
    }
}