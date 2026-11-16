package com.fooddelivery.service.impl;

import com.fooddelivery.dto.DishReviewRequest;
import com.fooddelivery.dto.DishReviewResponse;
import com.fooddelivery.entity.DishReview;
import com.fooddelivery.entity.FamilyMember;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.DishReviewRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.DishReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DishReviewServiceImpl implements DishReviewService {

    private static final String ANONYMOUS_NAME = "匿名用户";

    private final DishReviewRepository dishReviewRepository;
    private final DishRepository dishRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<DishReviewResponse> listReviews(Long familyId, Long dishId) {
        FamilyMember member = currentUserService.requireMembership(familyId);
        requireDish(familyId, dishId);
        Long currentUserId = member.getUserId();

        List<DishReview> all = dishReviewRepository
                .findByFamilyIdAndDishIdOrderByCreatedAtAscIdAsc(familyId, dishId);

        Map<Long, User> users = userRepository.findAllById(
                        all.stream().map(DishReview::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, DishReviewResponse> nodeMap = new LinkedHashMap<>();
        for (DishReview review : all) {
            nodeMap.put(review.getId(), toResponse(review, users.get(review.getUserId()),
                    review.getUserId().equals(currentUserId)));
        }

        List<DishReviewResponse> roots = new ArrayList<>();
        for (DishReview review : all) {
            DishReviewResponse node = nodeMap.get(review.getId());
            if (review.getParentId() == null) {
                roots.add(node);
            } else {
                DishReviewResponse parent = nodeMap.get(review.getParentId());
                if (parent != null) {
                    parent.getReplies().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        roots.sort(Comparator.comparing(DishReviewResponse::getCreatedAt).reversed());
        return roots;
    }

    @Override
    public DishReviewResponse createReview(Long familyId, Long dishId, DishReviewRequest request) {
        Long userId = currentUserService.requireMembership(familyId).getUserId();
        requireDish(familyId, dishId);
        validateTopLevel(request);

        DishReview review = DishReview.builder()
                .familyId(familyId)
                .dishId(dishId)
                .userId(userId)
                .parentId(null)
                .rating(request.getRating())
                .content(normalize(request.getContent()))
                .imageUrl(normalize(request.getImageUrl()))
                .anonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .build();

        DishReview saved = dishReviewRepository.save(review);
        return toResponse(saved, userRepository.findById(userId).orElse(null), true);
    }

    @Override
    public DishReviewResponse createReply(Long familyId, Long dishId, Long reviewId, DishReviewRequest request) {
        Long userId = currentUserService.requireMembership(familyId).getUserId();
        requireDish(familyId, dishId);
        DishReview parent = requireReview(familyId, dishId, reviewId);
        validateReply(request);

        DishReview reply = DishReview.builder()
                .familyId(familyId)
                .dishId(dishId)
                .userId(userId)
                .parentId(parent.getId())
                .rating(null)
                .content(normalize(request.getContent()))
                .imageUrl(normalize(request.getImageUrl()))
                .anonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .build();

        DishReview saved = dishReviewRepository.save(reply);
        return toResponse(saved, userRepository.findById(userId).orElse(null), true);
    }

    @Override
    public DishReviewResponse updateReview(Long familyId, Long dishId, Long reviewId, DishReviewRequest request) {
        Long userId = currentUserService.requireMembership(familyId).getUserId();
        requireDish(familyId, dishId);

        DishReview review = requireReview(familyId, dishId, reviewId);
        if (!review.getUserId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own review");
        }

        if (review.getParentId() == null) {
            validateTopLevel(request);
            review.setRating(request.getRating());
        } else {
            validateReply(request);
            review.setRating(null);
        }
        review.setContent(normalize(request.getContent()));
        review.setImageUrl(normalize(request.getImageUrl()));
        review.setAnonymous(Boolean.TRUE.equals(request.getAnonymous()));
        review.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return toResponse(dishReviewRepository.save(review), userRepository.findById(userId).orElse(null), true);
    }

    @Override
    public void deleteReview(Long familyId, Long dishId, Long reviewId) {
        FamilyMember member = currentUserService.requireMembership(familyId);
        Long userId = member.getUserId();
        requireDish(familyId, dishId);

        DishReview review = requireReview(familyId, dishId, reviewId);

        boolean isOwner = review.getUserId().equals(userId);
        boolean isAdmin = member.getRole() == FamilyMember.FamilyRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the review owner or a family admin can delete this review");
        }

        dishReviewRepository.delete(review);
    }

    private void validateTopLevel(DishReviewRequest request) {
        if (request == null || request.getRating() == null) {
            throw new BadRequestException("Rating is required");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }

    private void validateReply(DishReviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Reply content or image is required");
        }
        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty();
        if (!hasContent && !hasImage) {
            throw new BadRequestException("Reply content or image is required");
        }
    }

    private void requireDish(Long familyId, Long dishId) {
        dishRepository.findByIdAndFamilyId(dishId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + dishId));
    }

    private DishReview requireReview(Long familyId, Long dishId, Long reviewId) {
        DishReview review = dishReviewRepository.findByIdAndFamilyId(reviewId, familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
        if (!review.getDishId().equals(dishId)) {
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }
        return review;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DishReviewResponse toResponse(DishReview review, User user, boolean mine) {
        boolean anonymous = review.isAnonymous();
        return DishReviewResponse.builder()
                .id(review.getId())
                .dishId(review.getDishId())
                .parentId(review.getParentId())
                .anonymous(anonymous)
                .userName(anonymous ? ANONYMOUS_NAME : (user != null ? user.getName() : null))
                .avatarUrl(anonymous ? null : (user != null ? user.getAvatarUrl() : null))
                .mine(mine)
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
