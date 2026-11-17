package com.fooddelivery.controller;

import com.fooddelivery.dto.SystemDishReviewRequest;
import com.fooddelivery.dto.SystemDishReviewResponse;
import com.fooddelivery.service.SystemDishReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 广场菜品评论接口。
 * <p>
 * 挂在 {@code /api/system-dishes} 之下，与广场详情接口同域；由于
 * {@code SecurityConfig} 对除认证、上传外的所有请求默认要求登录，
 * 因此「未登录不可浏览评论」由安全配置天然保证。
 */
@RestController
@RequestMapping("/api/system-dishes")
@RequiredArgsConstructor
public class SystemDishReviewController {

    private final SystemDishReviewService systemDishReviewService;

    @GetMapping("/{dishId}/reviews")
    public ResponseEntity<List<SystemDishReviewResponse>> listReviews(@PathVariable Long dishId) {
        return ResponseEntity.ok(systemDishReviewService.listReviews(dishId));
    }

    @PostMapping("/{dishId}/reviews")
    public ResponseEntity<SystemDishReviewResponse> createReview(
            @PathVariable Long dishId,
            @Valid @RequestBody SystemDishReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(systemDishReviewService.createReview(dishId, request));
    }

    @PostMapping("/{dishId}/reviews/{reviewId}/replies")
    public ResponseEntity<SystemDishReviewResponse> createReply(
            @PathVariable Long dishId,
            @PathVariable Long reviewId,
            @Valid @RequestBody SystemDishReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(systemDishReviewService.createReply(dishId, reviewId, request));
    }

    @PatchMapping("/{dishId}/reviews/{reviewId}")
    public ResponseEntity<SystemDishReviewResponse> updateReview(
            @PathVariable Long dishId,
            @PathVariable Long reviewId,
            @Valid @RequestBody SystemDishReviewRequest request) {
        return ResponseEntity.ok(systemDishReviewService.updateReview(dishId, reviewId, request));
    }

    @DeleteMapping("/{dishId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long dishId,
            @PathVariable Long reviewId) {
        systemDishReviewService.deleteReview(dishId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
