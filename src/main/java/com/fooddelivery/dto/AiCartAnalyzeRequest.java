package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCartAnalyzeRequest {

    /**
     * 是否结合当前用户的健康档案（偏好/忌口/过敏/疾病）做个性化分析。
     * 默认 false，仅做不带个人健康信息的通用营养分析（合规：默认不注入敏感信息）。
     */
    private boolean includeHealthProfile = false;
}
