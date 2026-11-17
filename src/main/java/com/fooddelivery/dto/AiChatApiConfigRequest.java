package com.fooddelivery.dto;

import lombok.Data;

/**
 * 保存「小铲」对话 Provider 的 API 配置。apiKey 为空表示不修改已保存的 Key。
 */
@Data
public class AiChatApiConfigRequest {

    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
}
