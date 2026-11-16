package com.fooddelivery.repository;

import com.fooddelivery.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    Optional<Family> findByInviteCode(String inviteCode);
}