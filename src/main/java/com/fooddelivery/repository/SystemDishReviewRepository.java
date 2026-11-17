package com.fooddelivery.repository;

import com.fooddelivery.entity.SystemDishReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemDishReviewRepository extends JpaRepository<SystemDishReview, Long> {

    List<SystemDishReview> findByDishIdOrderByCreatedAtAscIdAsc(Long dishId);

    List<SystemDishReview> findByDishIdInAndParentIdIsNull(List<Long> dishIds);

    Optional<SystemDishReview> findByIdAndDishId(Long id, Long dishId);
}
