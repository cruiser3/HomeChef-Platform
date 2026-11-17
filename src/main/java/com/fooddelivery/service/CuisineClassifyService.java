package com.fooddelivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.dto.CuisineClassifyResult;
import com.fooddelivery.entity.Cuisine;
import com.fooddelivery.entity.SystemDish;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.repository.CuisineRepository;
import com.fooddelivery.repository.SystemDishRepository;
import com.fooddelivery.service.ai.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 系统菜谱「菜系」批量分类：读取未分类菜谱，分批交给 Ollama 从预置菜系白名单中判定，
 * 按顺序回填 cuisine_id。幂等：只处理 cuisine_id 为空的菜谱，可重复调用补漏。
 */
@Service
@RequiredArgsConstructor
public class CuisineClassifyService {

    private static final Logger log = LoggerFactory.getLogger(CuisineClassifyService.class);

    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRY = 2;

    private static final String SYSTEM_PROMPT = """
            你是中餐菜系分类助手。下面会给你一份菜品名称列表，请为每道菜判断它最可能属于哪个菜系。
            可选菜系（只能从以下选项中选择，不要输出列表之外的菜系）：%s
            只输出一个 JSON 对象：{"cuisines":["菜系1","菜系2",...]}，数组长度必须等于菜品数量，按输入顺序一一对应。
            无法判断时归入「其他」。
            """;

    private final SystemDishRepository systemDishRepository;
    private final CuisineRepository cuisineRepository;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public CuisineClassifyResult classifyCuisines() {
        List<Cuisine> cuisines = cuisineRepository.findByAvailableTrueOrderBySortNoAsc();
        if (cuisines.isEmpty()) {
            throw new BadRequestException("菜系列表为空，无法分类");
        }
        Map<String, Long> cuisineIdByName = cuisines.stream()
                .collect(Collectors.toMap(Cuisine::getName, Cuisine::getId));
        String whitelist = cuisines.stream().map(Cuisine::getName).collect(Collectors.joining("、"));

        List<SystemDish> pending = systemDishRepository.findByCuisineIdIsNullAndAvailableTrueOrderByIdAsc();
        if (pending.isEmpty()) {
            return CuisineClassifyResult.builder()
                    .total(0).classified(0).failed(0)
                    .message("没有待分类的菜谱")
                    .build();
        }

        int classified = 0;
        int failed = 0;
        for (int i = 0; i < pending.size(); i += BATCH_SIZE) {
            List<SystemDish> batch = pending.subList(i, Math.min(i + BATCH_SIZE, pending.size()));
            Map<Long, Long> dishCuisine = classifyBatch(batch, cuisineIdByName, whitelist);
            for (SystemDish dish : batch) {
                Long cuisineId = dishCuisine.get(dish.getId());
                if (cuisineId != null) {
                    dish.setCuisineId(cuisineId);
                    systemDishRepository.save(dish);
                    classified++;
                } else {
                    failed++;
                }
            }
        }

        return CuisineClassifyResult.builder()
                .total(pending.size())
                .classified(classified)
                .failed(failed)
                .message("待分类 " + pending.size() + " 道，成功 " + classified + " 道，失败 " + failed + " 道")
                .build();
    }

    private Map<Long, Long> classifyBatch(List<SystemDish> batch, Map<String, Long> cuisineIdByName, String whitelist) {
        String userPrompt = "菜品列表（按顺序）：\n"
                + IntStream.range(0, batch.size())
                        .mapToObj(i -> (i + 1) + ". " + batch.get(i).getName())
                        .collect(Collectors.joining("\n"));
        String systemPrompt = String.format(SYSTEM_PROMPT, whitelist);

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String content = ollamaClient.chat(systemPrompt, userPrompt);
                JsonNode arr = objectMapper.readTree(content).path("cuisines");
                if (!arr.isArray() || arr.size() != batch.size()) {
                    continue;
                }
                Map<Long, Long> result = new HashMap<>();
                for (int i = 0; i < batch.size(); i++) {
                    String cuisineName = arr.get(i).asText(null);
                    Long cuisineId = cuisineName == null ? null : cuisineIdByName.get(cuisineName.trim());
                    if (cuisineId != null) {
                        result.put(batch.get(i).getId(), cuisineId);
                    }
                }
                return result;
            } catch (Exception e) {
                log.warn("菜系分类批次失败（第 {} 次尝试）：{}", attempt, e.getMessage());
            }
        }
        return Map.of();
    }
}
