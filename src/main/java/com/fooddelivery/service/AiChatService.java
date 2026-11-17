package com.fooddelivery.service;

import com.fooddelivery.dto.AiChatRequest;
import com.fooddelivery.entity.AiChatConfig;
import com.fooddelivery.entity.AiChatMessage;
import com.fooddelivery.entity.AiChatSession;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.AiChatMessageRepository;
import com.fooddelivery.repository.AiChatSessionRepository;
import com.fooddelivery.service.ai.AiChatProvider;
import com.fooddelivery.service.ai.AiChatProviderResolver;
import com.fooddelivery.util.ApiKeyCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 「小铲」对话助手业务编排：校验消息 → 读取有效角色设定（独立 persona 或账户健康档案）
 * 注入系统提示词 → 组装多轮 messages → 按用户配置选择 Provider 以 SseEmitter 流式返回。
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    /** 最多携带最近 20 条历史消息（约 10 轮），避免超出模型上下文窗口。 */
    private static final int MAX_HISTORY = 20;

    private static final String SYSTEM_PROMPT = """
            你是「小铲」，本家庭私厨系统里的健康饮食助手，语气温暖亲切、简洁。
            你会结合用户的饮食偏好、忌口、过敏原和健康状况，提供饮食搭配、食材建议与健康规划。
            原则：
            1. 不提供医疗诊断或治疗方案，涉及疾病时建议咨询医生；
            2. 不推荐极端节食或危险减肥方式；
            3. 数值估算用「约」「大致」等措辞，不给精确医疗数据；
            4. 回答贴近家庭做饭场景，简洁实用。
            下方会给出用户的健康档案，请据此给出个性化建议。
            """;

    private static final Map<String, String> PREFERENCE_LABELS = Map.of(
            "FITNESS", "健身",
            "FAT_LOSS", "减脂",
            "MUSCLE_GAIN", "增肌",
            "LIGHT_DIET", "清淡饮食",
            "VEGETARIAN", "素食"
    );

    private final CurrentUserService currentUserService;
    private final AiChatConfigService aiChatConfigService;
    private final AiChatProviderResolver providerResolver;
    private final ApiKeyCipher apiKeyCipher;
    private final TaskExecutor taskExecutor;
    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;

    /**
     * 流式对话：返回 {@link SseEmitter}，每个增量 token 作为一条 SSE 事件推送，
     * 正常结束推送 [DONE]，异常推送 [ERROR] 后结束。
     */
    public SseEmitter chat(AiChatRequest request) {
        List<AiChatRequest.Message> input =
                (request == null || request.getMessages() == null) ? List.of() : request.getMessages();
        if (input.isEmpty()) {
            throw new BadRequestException("消息不能为空");
        }

        Long userId = currentUserService.getCurrentUserId();
        AiChatSession session = resolveSession(request.getSessionId(), userId);

        // 保存最后一条用户消息（前端 history 末尾可能是空 assistant，故向前查找）。
        String lastUserContent = findLastUserContent(input);
        if (!lastUserContent.isEmpty()) {
            messageRepository.save(AiChatMessage.builder()
                    .sessionId(session.getId())
                    .role("user")
                    .content(lastUserContent)
                    .build());
        }

        AiChatConfigService.EffectivePersona persona = aiChatConfigService.getEffectivePersona();

        List<AiChatProvider.ChatMessage> messages = new ArrayList<>();
        messages.add(new AiChatProvider.ChatMessage("system", buildSystemPrompt(persona)));
        for (AiChatRequest.Message m : trimHistory(input)) {
            String content = m.getContent() == null ? "" : m.getContent().trim();
            if (!content.isEmpty()) {
                messages.add(new AiChatProvider.ChatMessage(normalizeRole(m.getRole()), content));
            }
        }

        AiChatConfig config = aiChatConfigService.getConfigEntity();
        AiChatProvider provider = providerResolver.resolve(config);
        AiChatProvider.ChatConfig chatConfig = new AiChatProvider.ChatConfig(
                config != null ? config.getBaseUrl() : null,
                decryptKey(config),
                config != null ? config.getModel() : null);

        SseEmitter emitter = new SseEmitter(300_000L);
        taskExecutor.execute(() -> {
            StringBuilder reply = new StringBuilder();
            try {
                provider.streamChat(messages, chatConfig, token -> {
                    reply.append(token);
                    sendSafe(emitter, token);
                });
                if (reply.length() > 0) {
                    messageRepository.save(AiChatMessage.builder()
                            .sessionId(session.getId())
                            .role("assistant")
                            .content(reply.toString())
                            .build());
                    session.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    sessionRepository.save(session);
                }
                sendSafe(emitter, "[DONE]");
                emitter.complete();
            } catch (Exception e) {
                sendSafe(emitter, "[ERROR]");
                emitter.complete();
            }
        });
        return emitter;
    }

    private String findLastUserContent(List<AiChatRequest.Message> input) {
        for (int i = input.size() - 1; i >= 0; i--) {
            AiChatRequest.Message m = input.get(i);
            if ("user".equalsIgnoreCase(m.getRole())
                    && m.getContent() != null && !m.getContent().trim().isEmpty()) {
                return m.getContent().trim();
            }
        }
        return "";
    }

    private String decryptKey(AiChatConfig config) {
        if (config == null || config.getApiKeyEncrypted() == null || config.getApiKeyEncrypted().isBlank()) {
            return null;
        }
        try {
            return apiKeyCipher.decrypt(config.getApiKeyEncrypted());
        } catch (Exception e) {
            return null;
        }
    }

    private AiChatSession resolveSession(Long sessionId, Long userId) {
        if (sessionId == null) {
            throw new BadRequestException("缺少会话 id，请先创建会话");
        }
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在"));
        if (!session.getUserId().equals(userId)) {
            throw new ForbiddenException("无权访问该会话");
        }
        return session;
    }

    private void sendSafe(SseEmitter emitter, String data) {
        try {
            emitter.send(SseEmitter.event().data(data));
        } catch (Exception ignored) {
            // 客户端已断开，忽略后续推送
        }
    }

    private List<AiChatRequest.Message> trimHistory(List<AiChatRequest.Message> input) {
        return input.size() > MAX_HISTORY
                ? input.subList(input.size() - MAX_HISTORY, input.size())
                : input;
    }

    private String normalizeRole(String role) {
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
    }

    private String buildSystemPrompt(AiChatConfigService.EffectivePersona persona) {
        List<String> parts = new ArrayList<>();

        List<String> preferences = mapPreferences(persona.preferences());
        if (!preferences.isEmpty()) {
            parts.add("饮食偏好：" + String.join("、", preferences));
        }
        if (hasText(persona.restrictions())) {
            parts.add("忌口：" + persona.restrictions());
        }
        if (hasText(persona.allergies())) {
            parts.add("过敏原：" + persona.allergies());
        }
        if (hasText(persona.conditions())) {
            parts.add("健康状况：" + persona.conditions());
        }

        String profile = parts.isEmpty()
                ? "用户暂未填写健康档案，请按通用人群给出建议。"
                : "用户健康档案：" + String.join("；", parts) + "。";
        return SYSTEM_PROMPT + "\n" + profile;
    }

    private List<String> mapPreferences(String csv) {
        if (!hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> PREFERENCE_LABELS.getOrDefault(s.toUpperCase(Locale.ROOT), s))
                .toList();
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
