package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 购物车整餐分析结果。summary/nutrition 为段落文本，suggestions/cautions 为列表。
 * AI 只给出「估算/建议」，不得输出精确医疗诊断结论。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCartAnalyzeResponse {

    private String summary;

    private String nutrition;

    private List<String> suggestions;

    private List<String> cautions;
}
