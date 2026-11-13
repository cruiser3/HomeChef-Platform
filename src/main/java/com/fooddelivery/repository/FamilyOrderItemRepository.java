package com.fooddelivery.repository;

import com.fooddelivery.entity.FamilyOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyOrderItemRepository extends JpaRepository<FamilyOrderItem, Long> {

    Optional<FamilyOrderItem> findByIdAndOrderFamilyId(Long id, Long familyId);
}