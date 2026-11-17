package com.fooddelivery.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存「小铲」独立角色设定（persona），仅小铲生效，不影响个人账户健康档案。
 */
@Data
public class AiChatPersonaRequest {

    private List<String> dietaryPreferences;
    private List<String> dietaryRestrictions;
    private List<String> allergies;
    private List<String> medicalConditions;
}
