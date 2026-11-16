package com.fooddelivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.dto.AiRecipeParseResponse;
import com.fooddelivery.exception.AiServiceException;
import com.fooddelivery.service.ai.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 菜谱结构化业务编排：拼提示词 → 调 Ollama → 解析 JSON → 二次校验 → 失败重试与降级。
 * AI 只返回「草稿」，不回填数据库；由前端提交后走既有 Dish 创建流程完成落库。
 */
@Service
@RequiredArgsConstructor
public class AiRecipeService {

    private static final int MAX_RETRY = 2;

    private static final String SYSTEM_PROMPT = """
            你是菜谱解析助手，只输出一个 JSON 对象，不要任何解释、markdown、代码块或注释。
            固定结构：{"name":"菜名","ingredients":[{"name":"食材名称","amount":数字,"unit":"单位"}],"steps":["步骤文字"]}。
            强制规则：
            1. amount 必须是阿拉伯数字（整数或小数），禁止中文数字，禁止把单位写进 amount；
            2. unit 不能为空，必须单独提取单位；
            3. 遇到少许、适量、若干这类模糊描述，amount 填 0，unit 填原文。
            """;

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    /**
     * 解析用户粘贴的菜谱文本，返回结构化草稿。
     *
     * @param text 菜谱原文
     * @return 结构化结果（name/ingredients/steps）
     * @throws AiServiceException 多次尝试后仍失败时，由前端降级到「粘贴分步」或手动填写
     */
    public AiRecipeParseResponse parseRecipe(String text) {
        String userPrompt = "请解析下面的菜谱：" + text;

        AiServiceException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String content = ollamaClient.chat(SYSTEM_PROMPT, userPrompt);
                AiRecipeParseResponse result = objectMapper.readValue(content, AiRecipeParseResponse.class);
                result = sanitize(result);
                if (isValid(result)) {
                    return result;
                }
                lastError = new AiServiceException("AI 识别结果不完整，请重试");
            } catch (AiServiceException e) {
                lastError = e;
            } catch (Exception e) {
                lastError = new AiServiceException("AI 返回内容无法解析，请重试");
            }
        }
        throw lastError != null ? lastError : new AiServiceException("AI 识别失败");
    }

    private AiRecipeParseResponse sanitize(AiRecipeParseResponse result) {
        if (result.getName() != null) {
            result.setName(result.getName().trim());
        }
        if (result.getIngredients() != null) {
            for (AiRecipeParseResponse.Ingredient ing : result.getIngredients()) {
                if (ing.getName() != null) {
                    ing.setName(ing.getName().trim());
                }
                if (ing.getAmount() == null || ing.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                    ing.setAmount(BigDecimal.ZERO);
                }
                if (ing.getUnit() != null) {
                    ing.setUnit(ing.getUnit().trim());
                }
            }
        }
        if (result.getSteps() != null) {
            result.setSteps(result.getSteps().stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());
        }
        return result;
    }

    private boolean isValid(AiRecipeParseResponse result) {
        return result != null && result.getSteps() != null && !result.getSteps().isEmpty();
    }
}
