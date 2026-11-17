package com.fooddelivery.service.ai;

import com.fooddelivery.entity.AiChatConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 根据用户配置选择对话 Provider；未配置或未知 provider 一律回落到系统 Ollama。
 */
@Component
public class AiChatProviderResolver {

    private final Map<String, AiChatProvider> providers;

    public AiChatProviderResolver(List<AiChatProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(AiChatProvider::name, Function.identity()));
    }

    public AiChatProvider resolve(AiChatConfig config) {
        // 自定义 Provider 仅在 baseUrl 与 apiKey 均配置完整时才生效，否则回落到系统 Ollama。
        if (config != null && AiChatConfig.PROVIDER_OPENAI_COMPATIBLE.equals(config.getProvider())
                && hasText(config.getBaseUrl()) && hasText(config.getApiKeyEncrypted())) {
            AiChatProvider provider = providers.get(config.getProvider());
            if (provider != null) {
                return provider;
            }
        }
        return providers.get(AiChatConfig.PROVIDER_OLLAMA);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
