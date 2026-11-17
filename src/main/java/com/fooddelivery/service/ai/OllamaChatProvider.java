package com.fooddelivery.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 系统默认 Provider：委托 {@link OllamaClient} 调用本地 Ollama，忽略用户自配的 config。
 */
@Component
@RequiredArgsConstructor
public class OllamaChatProvider implements AiChatProvider {

    private final OllamaClient ollamaClient;

    @Override
    public String name() {
        return "OLLAMA";
    }

    @Override
    public void streamChat(List<ChatMessage> messages, ChatConfig config, Consumer<String> onToken) {
        List<OllamaClient.ChatMessage> converted = messages.stream()
                .map(m -> new OllamaClient.ChatMessage(m.role(), m.content()))
                .toList();
        ollamaClient.streamChat(converted, onToken);
    }
}
