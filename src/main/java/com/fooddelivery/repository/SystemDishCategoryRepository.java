package com.fooddelivery.repository;

import com.fooddelivery.entity.SystemDishCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemDishCategoryRepository extends JpaRepository<SystemDishCategory, Long> {

    List<SystemDishCategory> findByAvailableTrueOrderBySortNoAsc();
}
