package com.fooddelivery.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 对本地 Ollama 的唯一 HTTP 封装。所有 AI 调用都经由此类，隔离 Ollama 原生接口细节，
 * 未来切换到云端 OpenAI 兼容接口只需替换本类实现，业务代码不变。
 */
@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public OllamaClient(
            @Value("${app.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${app.ai.ollama.model:qwen2.5:7b}") String model,
            @Value("${app.ai.ollama.timeout-ms:120000}") int timeoutMs,
            RestClient.Builder builder,
            ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 调用 Ollama 原生 /api/chat（format=json 强制 JSON 输出），
     * 返回模型生成的文本内容（即模型输出的 JSON 字符串）。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return 模型生成的原始文本（JSON 字符串）
     * @throws AiServiceException Ollama 不可用、超时或响应异常时
     */
    public String chat(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "format", "json",
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            String raw = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            return root.path("message").path("content").asText();
        } catch (Exception e) {
            log.warn("Ollama chat failed: {}", e.getMessage());
            throw new AiServiceException("AI 服务不可用，请确认 Ollama 已启动后重试");
        }
    }
}
