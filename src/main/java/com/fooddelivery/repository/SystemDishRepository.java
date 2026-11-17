package com.fooddelivery.repository;

import com.fooddelivery.entity.SystemDish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemDishRepository extends JpaRepository<SystemDish, Long> {

    Optional<SystemDish> findByIdAndAvailableTrue(Long id);

    List<SystemDish> findByAvailableTrueOrderByIdDesc();

    List<SystemDish> findByCategoryIdAndAvailableTrueOrderByIdDesc(Long categoryId);

    List<SystemDish> findByNameContainingIgnoreCaseAndAvailableTrueOrderByIdDesc(String keyword);

    boolean existsBySourceTypeAndOriginalPath(String sourceType, String originalPath);

    Optional<SystemDish> findBySourceTypeAndOriginalPath(String sourceType, String originalPath);

    List<SystemDish> findByCuisineIdIsNullAndAvailableTrueOrderByIdAsc();
}
