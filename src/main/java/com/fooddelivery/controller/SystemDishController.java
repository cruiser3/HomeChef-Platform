package com.fooddelivery.controller;

import com.fooddelivery.dto.CopyToFamilyRequest;
import com.fooddelivery.dto.DishResponse;
import com.fooddelivery.dto.SystemDishCategoryResponse;
import com.fooddelivery.dto.SystemDishDetailResponse;
import com.fooddelivery.dto.SystemDishSummaryResponse;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.SystemDishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SystemDishController {

    private final SystemDishService systemDishService;

    @GetMapping("/api/system-dish-categories")
    public ResponseEntity<List<SystemDishCategoryResponse>> listCategories() {
        return ResponseEntity.ok(systemDishService.listCategories());
    }

    @GetMapping("/api/system-dishes")
    public ResponseEntity<List<SystemDishSummaryResponse>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(systemDishService.list(categoryId, keyword));
    }

    @GetMapping("/api/system-dishes/{id}")
    public ResponseEntity<SystemDishDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(systemDishService.getDetail(id));
    }

    @PostMapping("/api/system-dishes/{id}/copy-to-family")
    public ResponseEntity<DishResponse> copyToFamily(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long id,
            @Valid @RequestBody CopyToFamilyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(systemDishService.copyToFamily(requireFamilyId(familyId), id, request));
    }

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}
