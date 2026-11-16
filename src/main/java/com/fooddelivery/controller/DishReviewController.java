package com.fooddelivery.controller;

import com.fooddelivery.dto.DishReviewRequest;
import com.fooddelivery.dto.DishReviewResponse;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.service.DishReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishReviewController {

    private final DishReviewService dishReviewService;

    @GetMapping("/{dishId}/reviews")
    public ResponseEntity<List<DishReviewResponse>> listReviews(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId) {
        return ResponseEntity.ok(dishReviewService.listReviews(requireFamilyId(familyId), dishId));
    }

    @PostMapping("/{dishId}/reviews")
    public ResponseEntity<DishReviewResponse> createReview(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId,
            @Valid @RequestBody DishReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dishReviewService.createReview(requireFamilyId(familyId), dishId, request));
    }

    @PostMapping("/{dishId}/reviews/{reviewId}/replies")
    public ResponseEntity<DishReviewResponse> createReply(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId,
            @PathVariable Long reviewId,
            @Valid @RequestBody DishReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dishReviewService.createReply(requireFamilyId(familyId), dishId, reviewId, request));
    }

    @PatchMapping("/{dishId}/reviews/{reviewId}")
    public ResponseEntity<DishReviewResponse> updateReview(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId,
            @PathVariable Long reviewId,
            @Valid @RequestBody DishReviewRequest request) {
        return ResponseEntity.ok(
                dishReviewService.updateReview(requireFamilyId(familyId), dishId, reviewId, request));
    }

    @DeleteMapping("/{dishId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @RequestHeader(name = "X-Family-Id", required = false) Long familyId,
            @PathVariable Long dishId,
            @PathVariable Long reviewId) {
        dishReviewService.deleteReview(requireFamilyId(familyId), dishId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private Long requireFamilyId(Long familyId) {
        if (familyId == null) {
            throw new BadRequestException("X-Family-Id header is required");
        }
        return familyId;
    }
}
