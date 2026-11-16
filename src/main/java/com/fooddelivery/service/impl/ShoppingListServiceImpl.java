package com.fooddelivery.service.impl;

import com.fooddelivery.dto.AddShoppingItemRequest;
import com.fooddelivery.dto.ShoppingItemResponse;
import com.fooddelivery.dto.UpdatePurchasedRequest;
import com.fooddelivery.dto.UpdateShoppingItemRequest;
import com.fooddelivery.entity.FamilyOrder;
import com.fooddelivery.entity.FamilyOrderItem;
import com.fooddelivery.entity.FamilyShoppingItem;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.FamilyOrderItemRepository;
import com.fooddelivery.repository.FamilyOrderRepository;
import com.fooddelivery.repository.FamilyShoppingItemRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.ShoppingListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @Override
    public ShoppingItemResponse updateItem(Long familyId, Long itemId, UpdateShoppingItemRequest request) {
        currentUserService.requireMembership(familyId);

        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Ingredient name is required");
        }

        FamilyShoppingItem item = familyShoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found with id: " + itemId));
        if (!item.getFamilyId().equals(familyId)) {
            throw new ResourceNotFoundException("Shopping item not found with id: " + itemId);
        }

        item.setName(request.getName().trim());
        item.setAmount(request.getAmount());
        item.setUnit(request.getUnit().trim());
        familyShoppingItemRepository.save(item);

        return toResponse(item);
    }

    @Override
    public ShoppingItemResponse markPurchased(Long familyId, Long itemId, UpdatePurchasedRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        if (request == null || request.getPurchased() == null) {
            throw new BadRequestException("Purchased flag is required");
        }

        FamilyShoppingItem item = familyShoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found with id: " + itemId));
        if (!item.getFamilyId().equals(familyId)) {
            throw new ResourceNotFoundException("Shopping item not found with id: " + itemId);
        }

        boolean target = request.getPurchased();
        item.setPurchased(target);
        item.setPurchasedAt(target ? LocalDateTime.now(ZoneOffset.UTC) : null);
        familyShoppingItemRepository.save(item);

        // 反向联动：标记已采购时，若关联菜品仍处于「缺食材」，则自动恢复到「待备菜」
        if (target && item.getOrderItemId() != null) {
            familyOrderItemRepository.findByIdAndOrderFamilyId(item.getOrderItemId(), familyId)
                    .ifPresent(orderItem -> {
                        if (orderItem.getCookStatus() == FamilyOrderItem.CookStatus.BLOCKED) {
                            orderItem.setCookStatus(FamilyOrderItem.CookStatus.PENDING);
                            orderItem.setBlockedReason(null);
                            orderItem.setCookStatusUpdatedBy(userId);
                            orderItem.setCookStatusUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                            familyOrderItemRepository.save(orderItem);

                            FamilyOrder order = orderItem.getOrder();
                            order.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                            familyOrderRepository.save(order);
                        }
                    });
        }

        return toResponse(item);
    }

    @Override
    public void deleteItem(Long familyId, Long itemId) {
        currentUserService.requireMembership(familyId);

        FamilyShoppingItem item = familyShoppingItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping item not found with id: " + itemId));
        if (!item.getFamilyId().equals(familyId)) {
            throw new ResourceNotFoundException("Shopping item not found with id: " + itemId);
        }

        familyShoppingItemRepository.delete(item);
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
                .purchased(item.isPurchased())
                .purchasedAt(item.getPurchasedAt())
                .createdAt(item.getCreatedAt())
                .build();
    }
}