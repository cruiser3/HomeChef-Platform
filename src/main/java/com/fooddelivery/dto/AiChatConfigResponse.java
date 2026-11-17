package com.fooddelivery.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 「小铲」配置响应。apiKey 仅返回脱敏展示（sk-****尾4位），永不回传明文。
 * persona 在未显式配置时回落到个人账户健康档案。
 */
@Data
@Builder
public class AiChatConfigResponse {

    private String provider;
    private String baseUrl;
    private String model;
    private boolean hasApiKey;
    private String apiKeyMasked;
    private boolean personaConfigured;
    private Persona persona;

    @Data
    @Builder
    public static class Persona {
        private List<String> dietaryPreferences;
        private List<String> dietaryRestrictions;
        private List<String> allergies;
        private List<String> medicalConditions;
    }
}
