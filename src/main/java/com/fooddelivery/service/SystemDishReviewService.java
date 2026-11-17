package com.fooddelivery.service;

import com.fooddelivery.dto.SystemDishReviewRequest;
import com.fooddelivery.dto.SystemDishReviewResponse;

import java.util.List;

public interface SystemDishReviewService {

    List<SystemDishReviewResponse> listReviews(Long dishId);

    SystemDishReviewResponse createReview(Long dishId, SystemDishReviewRequest request);

    SystemDishReviewResponse createReply(Long dishId, Long reviewId, SystemDishReviewRequest request);

    SystemDishReviewResponse updateReview(Long dishId, Long reviewId, SystemDishReviewRequest request);

    void deleteReview(Long dishId, Long reviewId);
}
