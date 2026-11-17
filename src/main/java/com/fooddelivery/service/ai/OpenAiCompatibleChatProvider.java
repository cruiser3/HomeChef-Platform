package com.fooddelivery.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI 兼容流式 Provider（DeepSeek 等）：POST {baseUrl}/chat/completions，
 * 解析 SSE {@code choices[0].delta.content}，遇 {@code [DONE]} 结束。
 */
@Component
public class OpenAiCompatibleChatProvider implements AiChatProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleChatProvider.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public OpenAiCompatibleChatProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "OPENAI_COMPATIBLE";
    }

    @Override
    public void streamChat(List<ChatMessage> messages, ChatConfig config, Consumer<String> onToken) {
        try {
            String url = trimTrailingSlash(config.baseUrl()) + "/chat/completions";
            String model = (config.model() == null || config.model().isBlank())
                    ? "deepseek-chat" : config.model();

            List<Map<String, Object>> messageList = messages.stream()
                    .map(m -> Map.<String, Object>of("role", m.role(), "content", m.content()))
                    .toList();
            Map<String, Object> body = Map.of(
                    "model", model,
                    "stream", true,
                    "messages", messageList
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new AiServiceException("AI 服务请求失败（HTTP " + status + "），请检查 API 配置");
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        String content = node.path("choices").path(0).path("delta").path("content").asText(null);
                        if (content != null && !content.isEmpty()) {
                            onToken.accept(content);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse OpenAI-compatible stream chunk: {}", data);
                    }
                }
            }
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OpenAI-compatible stream chat failed: {}", e.getMessage());
            throw new AiServiceException("AI 服务不可用，请检查 API 配置后重试");
        }
    }

    private String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String s = url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
