package com.fooddelivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.dto.AiCartAnalyzeRequest;
import com.fooddelivery.dto.AiCartAnalyzeResponse;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.DishIngredient;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.AiServiceException;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.DishIngredientRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.service.ai.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车整餐分析业务编排：校验成员 → 聚合购物车菜品与食材 →（可选）注入健康档案 → 调 Ollama → 解析校验 → 重试降级。
 * 健康档案默认不注入，仅在用户前端明确授权（includeHealthProfile=true）后拼入提示词，并强制「不给出医疗诊断」。
 */
@Service
@RequiredArgsConstructor
public class AiCartAnalyzeService {

    private static final int MAX_RETRY = 2;

    private static final String SYSTEM_PROMPT = """
            你是家庭私厨的饮食分析助手，根据用户购物车中的菜品及食材给出整餐分析。
            只输出一个 JSON 对象，不要任何解释、markdown、代码块或注释。
            固定结构：{"summary":"整体评价","nutrition":"营养与热量概况","suggestions":["搭配建议"],"cautions":["注意事项"]}。
            要求：
            1. summary 用一两句话概括这一餐；
            2. nutrition 可估算热量与营养，但必须用「约」「估算」等措辞，禁止给出精确医疗数据或诊断；
            3. suggestions 给出 2~4 条具体、可执行的搭配或调整建议；
            4. cautions 列出需要留意的点（如高油、高糖、可能的过敏冲突等），没有则为空数组；
            5. 全程使用中文。
            """;

    private final CartItemRepository cartItemRepository;
    private final DishRepository dishRepository;
    private final DishIngredientRepository dishIngredientRepository;
    private final CurrentUserService currentUserService;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    /**
     * 分析当前用户当前家庭的购物车。
     *
     * @param familyId 家庭 id（前端 X-Family-Id 头）
     * @param request  可空请求体；includeHealthProfile=true 时才注入健康档案
     * @return 整餐分析结果
     * @throws AiServiceException 购物车为空、AI 不可用或多次解析失败时
     */
    public AiCartAnalyzeResponse analyzeCart(Long familyId, AiCartAnalyzeRequest request) {
        currentUserService.requireMembership(familyId);
        Long userId = currentUserService.getCurrentUserId();

        List<CartItem> cartItems = cartItemRepository.findByUserIdAndFamilyIdOrderByCreatedAtAsc(userId, familyId);
        if (cartItems.isEmpty()) {
            throw new AiServiceException("购物车是空的，无法分析");
        }

        String userPrompt = "当前购物车内容：\n" + buildCartText(cartItems);
        boolean includeHealth = request != null && request.isIncludeHealthProfile();
        if (includeHealth) {
            userPrompt += "\n" + buildHealthProfileText();
        }

        AiServiceException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String content = ollamaClient.chat(SYSTEM_PROMPT, userPrompt);
                AiCartAnalyzeResponse result = objectMapper.readValue(content, AiCartAnalyzeResponse.class);
                if (isValid(result)) {
                    return result;
                }
                lastError = new AiServiceException("AI 分析结果不完整，请重试");
            } catch (AiServiceException e) {
                lastError = e;
            } catch (Exception e) {
                lastError = new AiServiceException("AI 返回内容无法解析，请重试");
            }
        }
        throw lastError != null ? lastError : new AiServiceException("AI 分析失败");
    }

    private String buildCartText(List<CartItem> cartItems) {
        List<Long> dishIds = cartItems.stream().map(CartItem::getDishId).distinct().toList();
        Map<Long, Dish> dishes = dishRepository.findAllById(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity()));

        StringBuilder sb = new StringBuilder();
        for (CartItem item : cartItems) {
            Dish dish = dishes.get(item.getDishId());
            if (dish == null) {
                continue;
            }
            sb.append("- ").append(dish.getName()).append(" ×").append(item.getQuantity());
            List<DishIngredient> ingredients = dishIngredientRepository.findByDishIdOrderBySortNoAsc(dish.getId());
            if (!ingredients.isEmpty()) {
                String text = ingredients.stream()
                        .map(this::formatIngredient)
                        .collect(Collectors.joining("、"));
                sb.append("（食材：").append(text).append("）");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatIngredient(DishIngredient ing) {
        BigDecimal amount = ing.getAmount();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            return ing.getName() + amount.stripTrailingZeros().toPlainString() + ing.getUnit();
        }
        // amount=0 时，unit 即「少许/适量」等模糊量描述
        return ing.getName() + ing.getUnit();
    }

    private String buildHealthProfileText() {
        User user = currentUserService.getCurrentUser();
        List<String> parts = new ArrayList<>();
        if (hasText(user.getDietaryPreferences())) {
            parts.add("偏好：" + user.getDietaryPreferences());
        }
        if (hasText(user.getDietaryRestrictions())) {
            parts.add("忌口：" + user.getDietaryRestrictions());
        }
        if (hasText(user.getAllergies())) {
            parts.add("过敏：" + user.getAllergies());
        }
        if (hasText(user.getMedicalConditions())) {
            parts.add("疾病：" + user.getMedicalConditions());
        }
        if (parts.isEmpty()) {
            return "用户未填写健康档案，按通用人群分析。";
        }
        return "用户健康档案（标签可能为英文枚举，请按语义理解）："
                + String.join("；", parts)
                + "。请据此给出个性化建议，但不得给出医疗诊断。";
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private boolean isValid(AiCartAnalyzeResponse result) {
        if (result == null) {
            return false;
        }
        boolean hasText = hasText(result.getSummary()) || hasText(result.getNutrition());
        boolean hasList = result.getSuggestions() != null && !result.getSuggestions().isEmpty();
        return hasText || hasList;
    }
}
