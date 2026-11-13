package com.fooddelivery.controller;

import com.fooddelivery.dto.AddShoppingItemRequest;
import com.fooddelivery.dto.ShoppingItemResponse;
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

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}