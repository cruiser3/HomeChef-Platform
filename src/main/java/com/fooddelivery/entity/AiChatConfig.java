package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 「小铲」用户级配置：对话 Provider 选择（系统 Ollama / 自定义 OpenAI 兼容）
 * 以及独立的角色设定（persona，仅小铲生效，不影响个人账户健康档案）。
 */
@Entity
@Table(name = "ai_chat_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatConfig {

    public static final String PROVIDER_OLLAMA = "OLLAMA";
    public static final String PROVIDER_OPENAI_COMPATIBLE = "OPENAI_COMPATIBLE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "api_key_encrypted", length = 1000)
    private String apiKeyEncrypted;

    @Column(length = 100)
    private String model;

    @Column(name = "persona_configured", nullable = false)
    private boolean personaConfigured;

    @Column(name = "persona_preferences", length = 200)
    private String personaPreferences;

    @Column(name = "persona_restrictions", length = 200)
    private String personaRestrictions;

    @Column(name = "persona_allergies", length = 200)
    private String personaAllergies;

    @Column(name = "persona_conditions", length = 200)
    private String personaConditions;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);
}
