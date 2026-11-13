package com.fooddelivery.controller;

import com.fooddelivery.dto.DishCategoryRequest;
import com.fooddelivery.dto.DishCategoryResponse;
import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @PostMapping
    public ResponseEntity<DishResponse> create(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody DishRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dishService.create(requireFamilyId(familyId), request));
    }

    @GetMapping
    public ResponseEntity<List<DishResponse>> list(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean available) {
        return ResponseEntity.ok(dishService.list(requireFamilyId(familyId), categoryId, keyword, available));
    }

    @GetMapping("/{dishId}")
    public ResponseEntity<DishResponse> detail(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId) {
        return ResponseEntity.ok(dishService.getDetail(requireFamilyId(familyId), dishId));
    }

    @PutMapping("/{dishId}")
    public ResponseEntity<DishResponse> update(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId,
            @Valid @RequestBody DishRequest request) {
        return ResponseEntity.ok(dishService.update(requireFamilyId(familyId), dishId, request));
    }

    @PatchMapping("/{dishId}/availability")
    public ResponseEntity<DishResponse> changeAvailability(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId,
            @RequestParam boolean available) {
        return ResponseEntity.ok(dishService.changeAvailability(requireFamilyId(familyId), dishId, available));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<DishCategoryResponse>> listCategories(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId) {
        return ResponseEntity.ok(dishService.listCategories(requireFamilyId(familyId)));
    }

    @PostMapping("/categories")
    public ResponseEntity<DishCategoryResponse> createCategory(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @Valid @RequestBody DishCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dishService.createCategory(requireFamilyId(familyId), request));
    }

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}