package com.fooddelivery.repository;

import com.fooddelivery.entity.FamilyOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyOrderRepository extends JpaRepository<FamilyOrder, Long> {

    List<FamilyOrder> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    List<FamilyOrder> findByFamilyIdAndUserIdOrderByCreatedAtDesc(Long familyId, Long userId);

    Optional<FamilyOrder> findByIdAndFamilyId(Long id, Long familyId);
}