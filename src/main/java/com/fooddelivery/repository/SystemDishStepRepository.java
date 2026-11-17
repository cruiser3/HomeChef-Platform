package com.fooddelivery.repository;

import com.fooddelivery.entity.SystemDishStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemDishStepRepository extends JpaRepository<SystemDishStep, Long> {

    List<SystemDishStep> findByDishIdOrderByStepNoAsc(Long dishId);
}
