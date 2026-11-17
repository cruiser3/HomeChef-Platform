package com.fooddelivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.dto.AiCalorieRequest;
import com.fooddelivery.dto.AiCalorieResponse;
import com.fooddelivery.exception.AiServiceException;
import com.fooddelivery.service.ai.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * 卡路里估算业务编排：拼提示词 → 调 Ollama → 解析 JSON → 校验 → 失败重试。
 * AI 只返回「估算值」，不回填数据库；由前端确认后随菜品表单一起提交。
 */
@Service
@RequiredArgsConstructor
public class AiCalorieService {

    private static final int MAX_RETRY = 2;

    private static final String SYSTEM_PROMPT = """
            你是营养估算助手，根据菜名和食材估算这道菜的总卡路里（千卡）。
            只输出一个 JSON 对象，不要任何解释、markdown、代码块或注释。
            固定结构：{"calories":数字,"note":"估算说明"}。
            要求：
            1. calories 必须是整数（单位：千卡），按常见一人份估算，基于常见食材的热量与常见用量；
            2. note 用一句话说明估算依据，必须使用「约」「估算」等措辞，禁止给出精确医疗数据；
            3. 全程中文。
            """;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public AiCalorieResponse analyzeCalories(AiCalorieRequest request) {
        String userPrompt = buildUserPrompt(request);

        AiServiceException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String content = ollamaClient.chat(SYSTEM_PROMPT, userPrompt);
                AiCalorieResponse result = objectMapper.readValue(content, AiCalorieResponse.class);
                if (isValid(result)) {
                    return result;
                }
                lastError = new AiServiceException("AI 卡路里估算结果不完整，请重试");
            } catch (AiServiceException e) {
                lastError = e;
            } catch (Exception e) {
                lastError = new AiServiceException("AI 返回内容无法解析，请重试");
            }
        }
        throw lastError != null ? lastError : new AiServiceException("AI 卡路里估算失败");
    }

    private String buildUserPrompt(AiCalorieRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("菜名：").append(request.getName().trim());
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            String ingredients = request.getIngredients().stream()
                    .map(this::formatIngredient)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.joining("、"));
            if (!ingredients.isBlank()) {
                sb.append("\n食材：").append(ingredients);
            }
        }
        sb.append("\n请估算这道菜的总卡路里。");
        return sb.toString();
    }

    private String formatIngredient(AiCalorieRequest.IngredientItem ing) {
        BigDecimal amount = ing.getAmount();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            return ing.getName() + amount.stripTrailingZeros().toPlainString()
                    + (ing.getUnit() == null ? "" : ing.getUnit());
        }
        String text = ing.getName();
        if (ing.getAmountText() != null && !ing.getAmountText().isBlank()) {
            text += "（" + ing.getAmountText() + "）";
        } else if (ing.getUnit() != null && !ing.getUnit().isBlank()) {
            text += "（" + ing.getUnit() + "）";
        }
        return text;
    }

    private boolean isValid(AiCalorieResponse result) {
        return result != null && result.getCalories() != null && result.getCalories() > 0;
    }
}
