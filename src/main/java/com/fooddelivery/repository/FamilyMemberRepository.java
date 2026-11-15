package com.fooddelivery.repository;

import com.fooddelivery.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByUserIdAndLeftAtIsNull(Long userId);
    List<FamilyMember> findByFamilyIdAndLeftAtIsNull(Long familyId);
    List<FamilyMember> findByFamilyIdInAndLeftAtIsNull(List<Long> familyIds);
    Optional<FamilyMember> findByFamilyIdAndUserIdAndLeftAtIsNull(Long familyId, Long userId);
    boolean existsByFamilyIdAndUserIdAndLeftAtIsNull(Long familyId, Long userId);
}