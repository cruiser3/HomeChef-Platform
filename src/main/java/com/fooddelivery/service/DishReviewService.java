package com.fooddelivery.service;

import com.fooddelivery.dto.DishReviewRequest;
import com.fooddelivery.dto.DishReviewResponse;

import java.util.List;

public interface DishReviewService {

    List<DishReviewResponse> listReviews(Long familyId, Long dishId);

    DishReviewResponse createReview(Long familyId, Long dishId, DishReviewRequest request);

    DishReviewResponse createReply(Long familyId, Long dishId, Long reviewId, DishReviewRequest request);

    DishReviewResponse updateReview(Long familyId, Long dishId, Long reviewId, DishReviewRequest request);

    void deleteReview(Long familyId, Long dishId, Long reviewId);
}
