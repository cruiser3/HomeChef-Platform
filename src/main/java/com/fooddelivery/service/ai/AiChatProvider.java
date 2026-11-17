package com.fooddelivery.service.ai;

import java.util.List;
import java.util.function.Consumer;

/**
 * 对话 Provider 抽象：屏蔽不同后端（本地 Ollama / 云端 OpenAI 兼容接口）的差异，
 * 统一以流式逐 token 回调输出。
 */
public interface AiChatProvider {

    /** Provider 标识，与 {@code ai_chat_configs.provider} 取值一致。 */
    String name();

    void streamChat(List<ChatMessage> messages, ChatConfig config, Consumer<String> onToken);

    record ChatMessage(String role, String content) {}

    record ChatConfig(String baseUrl, String apiKey, String model) {}
}
