package com.fooddelivery.repository;

import com.fooddelivery.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdAndFamilyIdOrderByCreatedAtAsc(Long userId, Long familyId);

    Optional<CartItem> findByIdAndUserIdAndFamilyId(Long id, Long userId, Long familyId);

    Optional<CartItem> findByUserIdAndFamilyIdAndDishId(Long userId, Long familyId, Long dishId);

    void deleteByUserIdAndFamilyId(Long userId, Long familyId);
}