package com.fooddelivery.repository;

import com.fooddelivery.entity.FamilyShoppingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyShoppingItemRepository extends JpaRepository<FamilyShoppingItem, Long> {

    List<FamilyShoppingItem> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}