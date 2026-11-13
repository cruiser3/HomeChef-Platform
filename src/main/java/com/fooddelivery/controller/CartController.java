package com.fooddelivery.controller;

import com.fooddelivery.dto.AddCartItemRequest;
import com.fooddelivery.dto.CartResponse;
import com.fooddelivery.dto.UpdateCartItemRequest;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId) {
        return ResponseEntity.ok(cartService.getCart(requireFamilyId(familyId)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(requireFamilyId(familyId), request));
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(requireFamilyId(familyId), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(requireFamilyId(familyId), itemId));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clear(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId) {
        return ResponseEntity.ok(cartService.clear(requireFamilyId(familyId)));
    }

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}