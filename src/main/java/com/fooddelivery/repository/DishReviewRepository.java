package com.fooddelivery.repository;

import com.fooddelivery.entity.DishReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishReviewRepository extends JpaRepository<DishReview, Long> {

    List<DishReview> findByFamilyIdAndDishIdOrderByCreatedAtAscIdAsc(Long familyId, Long dishId);

    List<DishReview> findByFamilyIdAndDishIdInAndParentIdIsNull(Long familyId, List<Long> dishIds);

    Optional<DishReview> findByIdAndFamilyId(Long id, Long familyId);
}
