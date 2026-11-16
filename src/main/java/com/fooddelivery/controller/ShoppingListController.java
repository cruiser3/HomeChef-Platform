package com.fooddelivery.controller;

import com.fooddelivery.dto.AddShoppingItemRequest;
import com.fooddelivery.dto.ShoppingItemResponse;
import com.fooddelivery.dto.UpdatePurchasedRequest;
import com.fooddelivery.dto.UpdateShoppingItemRequest;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.ShoppingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    @GetMapping
    public ResponseEntity<List<ShoppingItemResponse>> listItems(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId) {
        return ResponseEntity.ok(shoppingListService.listItems(requireFamilyId(familyId)));
    }

    @PostMapping("/items")
    public ResponseEntity<ShoppingItemResponse> addItem(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody AddShoppingItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shoppingListService.addItem(requireFamilyId(familyId), request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ShoppingItemResponse> updateItem(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateShoppingItemRequest request) {
        return ResponseEntity.ok(
                shoppingListService.updateItem(requireFamilyId(familyId), itemId, request));
    }

    @PatchMapping("/items/{itemId}/purchased")
    public ResponseEntity<ShoppingItemResponse> markPurchased(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdatePurchasedRequest request) {
        return ResponseEntity.ok(
                shoppingListService.markPurchased(requireFamilyId(familyId), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long itemId) {
        shoppingListService.deleteItem(requireFamilyId(familyId), itemId);
        return ResponseEntity.noContent().build();
    }

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}