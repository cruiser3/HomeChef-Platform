package com.fooddelivery.service;

import com.fooddelivery.dto.AiChatApiConfigRequest;
import com.fooddelivery.dto.AiChatConfigResponse;
import com.fooddelivery.dto.AiChatPersonaRequest;
import com.fooddelivery.entity.AiChatConfig;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.repository.AiChatConfigRepository;
import com.fooddelivery.util.ApiKeyCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 「小铲」用户级配置：Provider 选择 + 独立角色设定（persona）。
 * persona 未显式配置时回落到个人账户健康档案；已配置则仅小铲生效。
 * API 配置与 persona 分开保存，互不影响。
 */
@Service
@RequiredArgsConstructor
public class AiChatConfigService {

    private final CurrentUserService currentUserService;
    private final AiChatConfigRepository configRepository;
    private final ApiKeyCipher apiKeyCipher;

    @Transactional(readOnly = true)
    public AiChatConfigResponse getConfig() {
        User user = currentUserService.getCurrentUser();
        AiChatConfig config = configRepository.findByUserId(user.getId()).orElse(null);
        return toResponse(config, resolvePersona(config, user));
    }

    @Transactional
    public AiChatConfigResponse saveApiConfig(AiChatApiConfigRequest request) {
        User user = currentUserService.getCurrentUser();
        AiChatConfig config = getOrCreate(user);

        config.setProvider(normalizeProvider(request.getProvider()));
        config.setBaseUrl(normalizeOptional(request.getBaseUrl()));
        config.setModel(normalizeOptional(request.getModel()));

        // apiKey 为空表示不修改已保存的 Key
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            config.setApiKeyEncrypted(apiKeyCipher.encrypt(request.getApiKey().trim()));
        }
        config.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        AiChatConfig saved = configRepository.save(config);
        return toResponse(saved, resolvePersona(saved, user));
    }

    @Transactional
    public AiChatConfigResponse savePersona(AiChatPersonaRequest request) {
        User user = currentUserService.getCurrentUser();
        AiChatConfig config = getOrCreate(user);

        config.setPersonaConfigured(true);
        config.setPersonaPreferences(serializePreferences(request.getDietaryPreferences()));
        config.setPersonaRestrictions(serializeTags(request.getDietaryRestrictions(), 5, "忌口"));
        config.setPersonaAllergies(serializeTags(request.getAllergies(), 8, "过敏"));
        config.setPersonaConditions(serializeTags(request.getMedicalConditions(), 8, "健康情况"));
        config.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        AiChatConfig saved = configRepository.save(config);
        return toResponse(saved, resolvePersona(saved, user));
    }

    @Transactional
    public void resetConfig() {
        Long userId = currentUserService.getCurrentUserId();
        configRepository.findByUserId(userId).ifPresent(configRepository::delete);
    }

    /** 对话链路使用：返回当前用户配置实体（可能为 null）。 */
    @Transactional(readOnly = true)
    public AiChatConfig getConfigEntity() {
        return configRepository.findByUserId(currentUserService.getCurrentUserId()).orElse(null);
    }

    /** 对话链路使用：返回已回落的 persona（4 个 CSV 字段，与 users 表格式一致）。 */
    @Transactional(readOnly = true)
    public EffectivePersona getEffectivePersona() {
        User user = currentUserService.getCurrentUser();
        AiChatConfig config = configRepository.findByUserId(user.getId()).orElse(null);
        if (config != null && config.isPersonaConfigured()) {
            return new EffectivePersona(config.getPersonaPreferences(), config.getPersonaRestrictions(),
                    config.getPersonaAllergies(), config.getPersonaConditions());
        }
        return new EffectivePersona(user.getDietaryPreferences(), user.getDietaryRestrictions(),
                user.getAllergies(), user.getMedicalConditions());
    }

    public record EffectivePersona(String preferences, String restrictions, String allergies, String conditions) {}

    private AiChatConfig getOrCreate(User user) {
        return configRepository.findByUserId(user.getId()).orElseGet(() ->
                AiChatConfig.builder()
                        .userId(user.getId())
                        .provider(AiChatConfig.PROVIDER_OLLAMA)
                        .build());
    }

    private AiChatConfigResponse.Persona resolvePersona(AiChatConfig config, User user) {
        if (config != null && config.isPersonaConfigured()) {
            return AiChatConfigResponse.Persona.builder()
                    .dietaryPreferences(parseCsv(config.getPersonaPreferences()))
                    .dietaryRestrictions(parseCsv(config.getPersonaRestrictions()))
                    .allergies(parseCsv(config.getPersonaAllergies()))
                    .medicalConditions(parseCsv(config.getPersonaConditions()))
                    .build();
        }
        return AiChatConfigResponse.Persona.builder()
                .dietaryPreferences(parseCsv(user.getDietaryPreferences()))
                .dietaryRestrictions(parseCsv(user.getDietaryRestrictions()))
                .allergies(parseCsv(user.getAllergies()))
                .medicalConditions(parseCsv(user.getMedicalConditions()))
                .build();
    }

    private AiChatConfigResponse toResponse(AiChatConfig config, AiChatConfigResponse.Persona persona) {
        boolean hasApiKey = hasText(config != null ? config.getApiKeyEncrypted() : null);
        return AiChatConfigResponse.builder()
                .provider(config != null ? config.getProvider() : AiChatConfig.PROVIDER_OLLAMA)
                .baseUrl(config != null ? config.getBaseUrl() : null)
                .model(config != null ? config.getModel() : null)
                .hasApiKey(hasApiKey)
                .apiKeyMasked(maskApiKey(config != null ? config.getApiKeyEncrypted() : null))
                .personaConfigured(config != null && config.isPersonaConfigured())
                .persona(persona)
                .build();
    }

    private String maskApiKey(String encrypted) {
        if (!hasText(encrypted)) {
            return null;
        }
        try {
            String plain = apiKeyCipher.decrypt(encrypted);
            if (plain.length() <= 8) {
                return "****";
            }
            return plain.substring(0, 4) + "****" + plain.substring(plain.length() - 4);
        } catch (Exception e) {
            return "****";
        }
    }

    private String normalizeProvider(String provider) {
        if (AiChatConfig.PROVIDER_OPENAI_COMPATIBLE.equals(provider)) {
            return AiChatConfig.PROVIDER_OPENAI_COMPATIBLE;
        }
        return AiChatConfig.PROVIDER_OLLAMA;
    }

    private String normalizeOptional(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String serializePreferences(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
        for (String v : normalized) {
            try {
                User.DietaryPreference.valueOf(v);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unsupported dietary preference: " + v);
            }
        }
        if (normalized.size() > 5) {
            throw new BadRequestException("At most 5 dietary preferences are allowed");
        }
        return String.join(",", normalized);
    }

    private String serializeTags(List<String> values, int max, String label) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        if (normalized.size() > max) {
            throw new BadRequestException("At most " + max + " " + label + " are allowed");
        }
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
