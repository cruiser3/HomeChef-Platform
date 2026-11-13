package com.fooddelivery.repository;

import com.fooddelivery.entity.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishCategoryRepository extends JpaRepository<DishCategory, Long> {

    List<DishCategory> findByFamilyIdOrderBySortNoAsc(Long familyId);

    Optional<DishCategory> findByFamilyIdAndName(Long familyId, String name);

    Optional<DishCategory> findByIdAndFamilyId(Long id, Long familyId);

    boolean existsByFamilyIdAndName(Long familyId, String name);
}