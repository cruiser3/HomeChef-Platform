package com.fooddelivery.repository;

import com.fooddelivery.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    Optional<Dish> findByIdAndFamilyId(Long id, Long familyId);

    List<Dish> findByFamilyIdAndCategoryIdOrderByCreatedAtDesc(Long familyId, Long categoryId);

    List<Dish> findByFamilyIdAndAvailableOrderByCreatedAtDesc(Long familyId, boolean available);

    List<Dish> findByFamilyIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long familyId, String keyword);
}