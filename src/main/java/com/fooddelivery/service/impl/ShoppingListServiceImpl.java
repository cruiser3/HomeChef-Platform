package com.fooddelivery.service.impl;

import com.fooddelivery.dto.AddShoppingItemRequest;
import com.fooddelivery.dto.ShoppingItemResponse;
import com.fooddelivery.entity.FamilyOrder;
import com.fooddelivery.entity.FamilyOrderItem;
import com.fooddelivery.entity.FamilyShoppingItem;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.repository.FamilyOrderItemRepository;
import com.fooddelivery.repository.FamilyOrderRepository;
import com.fooddelivery.repository.FamilyShoppingItemRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.ShoppingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShoppingListServiceImpl implements ShoppingListService {

    private final FamilyShoppingItemRepository familyShoppingItemRepository;
    private final FamilyOrderRepository familyOrderRepository;
    private final FamilyOrderItemRepository familyOrderItemRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingItemResponse> listItems(Long familyId) {
        currentUserService.requireMembership(familyId);
        return familyShoppingItemRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ShoppingItemResponse addItem(Long familyId, AddShoppingItemRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Ingredient name is required");
        }

        Long orderId = null;
        Long orderItemId = null;

        if (request.getOrderId() != null) {
            FamilyOrder order = familyOrderRepository.findByIdAndFamilyId(request.getOrderId(), familyId)
                    .orElseThrow(() -> new BadRequestException("Order does not belong to the current family"));
            orderId = order.getId();
        }

        if (request.getOrderItemId() != null) {
            FamilyOrderItem item = familyOrderItemRepository
                    .findByIdAndOrderFamilyId(request.getOrderItemId(), familyId)
                    .orElseThrow(() -> new BadRequestException("Order item does not belong to the current family"));
            orderItemId = item.getId();
            if (orderId != null && !orderId.equals(item.getOrder().getId())) {
                throw new BadRequestException("Order id and order item id do not match");
            }
            orderId = item.getOrder().getId();
        }

        FamilyShoppingItem saved = familyShoppingItemRepository.save(FamilyShoppingItem.builder()
                .familyId(familyId)
                .name(request.getName().trim())
                .amount(request.getAmount())
                .unit(request.getUnit().trim())
                .orderId(orderId)
                .orderItemId(orderItemId)
                .addedBy(userId)
                .build());

        return toResponse(saved);
    }

    private ShoppingItemResponse toResponse(FamilyShoppingItem item) {
        return ShoppingItemResponse.builder()
                .id(item.getId())
                .familyId(item.getFamilyId())
                .name(item.getName())
                .amount(item.getAmount())
                .unit(item.getUnit())
                .orderId(item.getOrderId())
                .orderItemId(item.getOrderItemId())
                .addedBy(item.getAddedBy())
                .createdAt(item.getCreatedAt())
                .build();
    }
}