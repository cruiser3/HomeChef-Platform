package com.fooddelivery.repository;

import com.fooddelivery.entity.SystemDishIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemDishIngredientRepository extends JpaRepository<SystemDishIngredient, Long> {

    List<SystemDishIngredient> findByDishIdOrderBySortNoAsc(Long dishId);
}
