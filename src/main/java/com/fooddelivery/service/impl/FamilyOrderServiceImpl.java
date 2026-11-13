package com.fooddelivery.service.impl;

import com.fooddelivery.dto.AssignResponsibleRequest;
import com.fooddelivery.dto.CreateFamilyOrderRequest;
import com.fooddelivery.dto.FamilyOrderResponse;
import com.fooddelivery.dto.UpdateCookStatusRequest;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.FamilyOrder;
import com.fooddelivery.entity.FamilyOrderItem;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.FamilyMemberRepository;
import com.fooddelivery.repository.FamilyOrderItemRepository;
import com.fooddelivery.repository.FamilyOrderRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.FamilyOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyOrderServiceImpl implements FamilyOrderService {

    private final FamilyOrderRepository familyOrderRepository;
    private final FamilyOrderItemRepository familyOrderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final DishRepository dishRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    public FamilyOrderResponse createOrder(Long familyId, CreateFamilyOrderRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        List<CartItem> cartItems = cartItemRepository.findByUserIdAndFamilyIdOrderByCreatedAtAsc(userId, familyId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cart is empty, cannot create an order");
        }

        Map<Long, Dish> dishes = dishRepository.findAllById(
                        cartItems.stream().map(CartItem::getDishId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));

        List<FamilyOrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Dish dish = dishes.get(cartItem.getDishId());
            if (dish == null || !dish.getFamilyId().equals(familyId)) {
                throw new BadRequestException("Dish does not belong to the current family");
            }
            if (!dish.isAvailable()) {
                throw new BadRequestException("Dish is no longer available: " + dish.getName());
            }

            int quantity = cartItem.getQuantity();
            BigDecimal unitPrice = dish.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);

            orderItems.add(FamilyOrderItem.builder()
                    .dishId(dish.getId())
                    .dishNameSnapshot(dish.getName())
                    .unitPriceSnapshot(unitPrice)
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .cookStatus(FamilyOrderItem.CookStatus.PENDING)
                    .build());
        }

        FamilyOrder order = FamilyOrder.builder()
                .familyId(familyId)
                .userId(userId)
                .totalAmount(total)
                .itemCount(orderItems.size())
                .status(FamilyOrder.OrderStatus.CREATED)
                .mealPeriod(request == null ? null : request.getMealPeriod())
                .remark(request == null ? null : request.getRemark())
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.getItems().addAll(orderItems);

        FamilyOrder saved = familyOrderRepository.save(order);
        cartItemRepository.deleteByUserIdAndFamilyId(userId, familyId);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyOrderResponse> listOrders(Long familyId) {
        currentUserService.requireMembership(familyId);
        return familyOrderRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyOrderResponse getOrder(Long familyId, Long orderId) {
        currentUserService.requireMembership(familyId);
        return toResponse(requireFamilyOrder(familyId, orderId));
    }

    @Override
    public FamilyOrderResponse cancelOrder(Long familyId, Long orderId) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();
        FamilyOrder order = requireFamilyOrder(familyId, orderId);

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("Only the order owner can cancel this order");
        }
        if (order.getStatus() == FamilyOrder.OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        if (order.getStatus() != FamilyOrder.OrderStatus.CREATED) {
            throw new BadRequestException("Order can only be cancelled before preparation starts");
        }
        boolean anyStarted = order.getItems().stream()
                .anyMatch(item -> item.getCookStatus() != FamilyOrderItem.CookStatus.PENDING);
        if (anyStarted) {
            throw new BadRequestException("Order can only be cancelled while all dishes are pending");
        }

        order.setStatus(FamilyOrder.OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(userId);
        order.setUpdatedAt(LocalDateTime.now());

        return toResponse(familyOrderRepository.save(order));
    }

    @Override
    public FamilyOrderResponse startPreparing(Long familyId, Long orderId) {
        currentUserService.requireMembership(familyId);
        FamilyOrder order = requireFamilyOrder(familyId, orderId);

        if (order.getStatus() == FamilyOrder.OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order cannot start preparation");
        }
        if (order.getStatus() != FamilyOrder.OrderStatus.CREATED) {
            throw new BadRequestException("Order is not in CREATED status");
        }

        order.setStatus(FamilyOrder.OrderStatus.PREPARING);
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(familyOrderRepository.save(order));
    }

    @Override
    public FamilyOrderResponse markMealServed(Long familyId, Long orderId) {
        currentUserService.requireMembership(familyId);
        FamilyOrder order = requireFamilyOrder(familyId, orderId);

        if (order.getStatus() == FamilyOrder.OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order cannot be marked as served");
        }

        order.setMealServed(true);
        order.setMealServedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return toResponse(familyOrderRepository.save(order));
    }

    @Override
    public FamilyOrderResponse updateCookStatus(Long familyId, Long orderItemId, UpdateCookStatusRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BadRequestException("Cook status is required");
        }

        FamilyOrderItem.CookStatus target;
        try {
            target = FamilyOrderItem.CookStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unknown cook status: " + request.getStatus());
        }

        FamilyOrderItem item = familyOrderItemRepository.findByIdAndOrderFamilyId(orderItemId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + orderItemId));

        FamilyOrder order = item.getOrder();
        if (order.getStatus() == FamilyOrder.OrderStatus.CANCELLED) {
            throw new BadRequestException("Cancelled order items cannot change cook status");
        }
        if (!item.getCookStatus().canTransitionTo(target)) {
            throw new BadRequestException("Illegal cook status transition: "
                    + item.getCookStatus() + " -> " + target);
        }
        if (target == FamilyOrderItem.CookStatus.BLOCKED
                && (request.getBlockedReason() == null || request.getBlockedReason().isBlank())) {
            throw new BadRequestException("Blocked reason is required when marking a dish as BLOCKED");
        }

        item.setCookStatus(target);
        item.setBlockedReason(target == FamilyOrderItem.CookStatus.BLOCKED ? request.getBlockedReason().trim() : null);
        item.setCookStatusUpdatedBy(userId);
        item.setCookStatusUpdatedAt(LocalDateTime.now());
        familyOrderItemRepository.save(item);

        order.setUpdatedAt(LocalDateTime.now());
        familyOrderRepository.save(order);

        return toResponse(order);
    }

    @Override
    public FamilyOrderResponse assignResponsible(Long familyId, Long orderItemId, AssignResponsibleRequest request) {
        currentUserService.requireMembership(familyId);

        if (request == null || request.getResponsibleUserId() == null) {
            throw new BadRequestException("Responsible user id is required");
        }
        if (!familyMemberRepository.existsByFamilyIdAndUserId(familyId, request.getResponsibleUserId())) {
            throw new ForbiddenException("Responsible user must be a member of this family");
        }

        FamilyOrderItem item = familyOrderItemRepository.findByIdAndOrderFamilyId(orderItemId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + orderItemId));

        item.setResponsibleUserId(request.getResponsibleUserId());
        familyOrderItemRepository.save(item);

        return toResponse(item.getOrder());
    }

    private FamilyOrder requireFamilyOrder(Long familyId, Long orderId) {
        return familyOrderRepository.findByIdAndFamilyId(orderId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private FamilyOrderResponse toResponse(FamilyOrder order) {
        List<FamilyOrderItem> items = order.getItems() == null ? List.of() : order.getItems();

        Map<Long, String> userNames = userRepository.findAllById(
                        items.stream()
                                .map(FamilyOrderItem::getResponsibleUserId)
                                .filter(id -> id != null)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        String orderUserName = userRepository.findById(order.getUserId())
                .map(User::getName)
                .orElse(null);

        List<FamilyOrderResponse.FamilyOrderItemResponse> itemResponses = items.stream()
                .map(item -> FamilyOrderResponse.FamilyOrderItemResponse.builder()
                        .id(item.getId())
                        .dishId(item.getDishId())
                        .dishName(item.getDishNameSnapshot())
                        .unitPrice(item.getUnitPriceSnapshot())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .cookStatus(item.getCookStatus().name())
                        .responsibleUserId(item.getResponsibleUserId())
                        .responsibleUserName(item.getResponsibleUserId() == null
                                ? null : userNames.get(item.getResponsibleUserId()))
                        .blockedReason(item.getBlockedReason())
                        .cookStatusUpdatedBy(item.getCookStatusUpdatedBy())
                        .cookStatusUpdatedAt(item.getCookStatusUpdatedAt())
                        .build())
                .toList();

        return FamilyOrderResponse.builder()
                .id(order.getId())
                .familyId(order.getFamilyId())
                .userId(order.getUserId())
                .userName(orderUserName)
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItemCount())
                .status(order.getStatus().name())
                .mealPeriod(order.getMealPeriod())
                .remark(order.getRemark())
                .mealServed(order.isMealServed())
                .mealServedAt(order.getMealServedAt())
                .cancelledAt(order.getCancelledAt())
                .cancelledBy(order.getCancelledBy())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}