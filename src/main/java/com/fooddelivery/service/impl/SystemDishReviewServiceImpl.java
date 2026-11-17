package com.fooddelivery.service.impl;

import com.fooddelivery.dto.SystemDishReviewRequest;
import com.fooddelivery.dto.SystemDishReviewResponse;
import com.fooddelivery.entity.SystemDishReview;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.SystemDishRepository;
import com.fooddelivery.repository.SystemDishReviewRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.SystemDishReviewService;
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

/**
 * 广场菜品评论服务实现。
 * <p>
 * 业务规则对齐家庭菜品评论 {@link DishReviewServiceImpl}，差异在于：
 * <ul>
 *   <li>广场无家庭隔离，登录用户即可浏览、发表、回复；</li>
 *   <li>编辑仅限评论本人；删除仅限评论本人或系统管理员。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SystemDishReviewServiceImpl implements SystemDishReviewService {

    private static final String ANONYMOUS_NAME = "匿名用户";

    private final SystemDishReviewRepository systemDishReviewRepository;
    private final SystemDishRepository systemDishRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<SystemDishReviewResponse> listReviews(Long dishId) {
        // 未登录时 getCurrentUserId 抛 AuthenticationCredentialsNotFoundException（401）。
        Long currentUserId = currentUserService.getCurrentUserId();
        requireDish(dishId);

        List<SystemDishReview> all = systemDishReviewRepository
                .findByDishIdOrderByCreatedAtAscIdAsc(dishId);

        Map<Long, User> users = userRepository.findAllById(
                        all.stream().map(SystemDishReview::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, SystemDishReviewResponse> nodeMap = new LinkedHashMap<>();
        for (SystemDishReview review : all) {
            nodeMap.put(review.getId(), toResponse(review, users.get(review.getUserId()),
                    review.getUserId().equals(currentUserId)));
        }

        List<SystemDishReviewResponse> roots = new ArrayList<>();
        for (SystemDishReview review : all) {
            SystemDishReviewResponse node = nodeMap.get(review.getId());
            if (review.getParentId() == null) {
                roots.add(node);
            } else {
                SystemDishReviewResponse parent = nodeMap.get(review.getParentId());
                if (parent != null) {
                    parent.getReplies().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        roots.sort(Comparator.comparing(SystemDishReviewResponse::getCreatedAt).reversed());
        return roots;
    }

    @Override
    public SystemDishReviewResponse createReview(Long dishId, SystemDishReviewRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        requireDish(dishId);
        validateTopLevel(request);

        SystemDishReview review = SystemDishReview.builder()
                .dishId(dishId)
                .userId(userId)
                .parentId(null)
                .rating(request.getRating())
                .content(normalize(request.getContent()))
                .imageUrl(normalize(request.getImageUrl()))
                .anonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .build();

        SystemDishReview saved = systemDishReviewRepository.save(review);
        return toResponse(saved, userRepository.findById(userId).orElse(null), true);
    }

    @Override
    public SystemDishReviewResponse createReply(Long dishId, Long reviewId, SystemDishReviewRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        requireDish(dishId);
        SystemDishReview parent = requireReview(dishId, reviewId);
        validateReply(request);

        SystemDishReview reply = SystemDishReview.builder()
                .dishId(dishId)
                .userId(userId)
                .parentId(parent.getId())
                .rating(null)
                .content(normalize(request.getContent()))
                .imageUrl(normalize(request.getImageUrl()))
                .anonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .build();

        SystemDishReview saved = systemDishReviewRepository.save(reply);
        return toResponse(saved, userRepository.findById(userId).orElse(null), true);
    }

    @Override
    public SystemDishReviewResponse updateReview(Long dishId, Long reviewId, SystemDishReviewRequest request) {
        Long userId = currentUserService.getCurrentUserId();
        requireDish(dishId);

        SystemDishReview review = requireReview(dishId, reviewId);
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

        return toResponse(systemDishReviewRepository.save(review),
                userRepository.findById(userId).orElse(null), true);
    }

    @Override
    public void deleteReview(Long dishId, Long reviewId) {
        User current = currentUserService.getCurrentUser();
        Long userId = current.getId();
        requireDish(dishId);

        SystemDishReview review = requireReview(dishId, reviewId);

        boolean isOwner = review.getUserId().equals(userId);
        boolean isAdmin = current.getRole() == User.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("Only the review owner or an admin can delete this review");
        }

        systemDishReviewRepository.delete(review);
    }

    private void validateTopLevel(SystemDishReviewRequest request) {
        if (request == null || request.getRating() == null) {
            throw new BadRequestException("Rating is required");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
    }

    private void validateReply(SystemDishReviewRequest request) {
        if (request == null) {
            throw new BadRequestException("Reply content or image is required");
        }
        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasImage = request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty();
        if (!hasContent && !hasImage) {
            throw new BadRequestException("Reply content or image is required");
        }
    }

    private void requireDish(Long dishId) {
        if (!systemDishRepository.existsById(dishId)) {
            throw new ResourceNotFoundException("System dish not found with id: " + dishId);
        }
    }

    private SystemDishReview requireReview(Long dishId, Long reviewId) {
        return systemDishReviewRepository.findByIdAndDishId(reviewId, dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SystemDishReviewResponse toResponse(SystemDishReview review, User user, boolean mine) {
        boolean anonymous = review.isAnonymous();
        return SystemDishReviewResponse.builder()
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
