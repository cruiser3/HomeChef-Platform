package com.fooddelivery.repository;

import com.fooddelivery.entity.AiChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiChatConfigRepository extends JpaRepository<AiChatConfig, Long> {

    Optional<AiChatConfig> findByUserId(Long userId);
}
