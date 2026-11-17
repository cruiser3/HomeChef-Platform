package com.fooddelivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 「小铲」对话请求：携带多轮消息历史（含当前用户最新一条）。
 * 健康档案由后端从数据库读取注入，不在此处传递，避免被前端伪造。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    /** 会话 id，可空；为空时需先经 /api/ai/sessions 创建会话。 */
    private Long sessionId;

    private List<Message> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
