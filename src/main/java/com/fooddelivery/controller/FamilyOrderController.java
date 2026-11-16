package com.fooddelivery.controller;

import com.fooddelivery.dto.AssignResponsibleRequest;
import com.fooddelivery.dto.CreateFamilyOrderRequest;
import com.fooddelivery.dto.FamilyOrderResponse;
import com.fooddelivery.dto.UpdateCookStatusRequest;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.FamilyOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-orders")
@RequiredArgsConstructor
public class FamilyOrderController {

    private final FamilyOrderService familyOrderService;

    @PostMapping
    public ResponseEntity<FamilyOrderResponse> createOrder(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody(required = false) CreateFamilyOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(familyOrderService.createOrder(requireFamilyId(familyId), request));
    }

    @GetMapping
    public ResponseEntity<List<FamilyOrderResponse>> listOrders(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId) {
        return ResponseEntity.ok(familyOrderService.listOrders(requireFamilyId(familyId)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<FamilyOrderResponse> getOrder(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(familyOrderService.getOrder(requireFamilyId(familyId), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<FamilyOrderResponse> cancelOrder(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(familyOrderService.cancelOrder(requireFamilyId(familyId), orderId));
    }

    @PostMapping("/{orderId}/start-preparing")
    public ResponseEntity<FamilyOrderResponse> startPreparing(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(familyOrderService.startPreparing(requireFamilyId(familyId), orderId));
    }

    @PostMapping("/{orderId}/meal-served")
    public ResponseEntity<FamilyOrderResponse> markMealServed(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(familyOrderService.markMealServed(requireFamilyId(familyId), orderId));
    }

    @PatchMapping("/items/{orderItemId}/cook-status")
    public ResponseEntity<FamilyOrderResponse> updateCookStatus(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderItemId,
            @Valid @RequestBody UpdateCookStatusRequest request) {
        return ResponseEntity.ok(familyOrderService.updateCookStatus(requireFamilyId(familyId), orderItemId, request));
    }

    @PatchMapping("/items/{orderItemId}/responsible")
    public ResponseEntity<FamilyOrderResponse> assignResponsible(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderItemId,
            @Valid @RequestBody AssignResponsibleRequest request) {
        return ResponseEntity.ok(familyOrderService.assignResponsible(requireFamilyId(familyId), orderItemId, request));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long orderId) {
        familyOrderService.deleteOrder(requireFamilyId(familyId), orderId);
        return ResponseEntity.noContent().build();
    }

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}