-- =====================================================================
-- V18: AI 对话历史持久化：会话表 + 消息表
--
-- 目的：为「小铲」助手持久化多轮对话，支持多会话列表与历史恢复。
-- 设计：
--   - ai_chat_sessions：一个会话一行，归属单个用户（健康档案为用户级）。
--   - ai_chat_messages：会话下的消息，role 为 user / assistant。
--   - 时间戳由应用层（实体）以 UTC 写入，DEFAULT CURRENT_TIMESTAMP 仅作兜底。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. DROP TABLE ai_chat_messages;
--   2. DROP TABLE ai_chat_sessions;
--   3. DELETE FROM flyway_schema_history WHERE version = '18';
-- =====================================================================

CREATE TABLE ai_chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ai_chat_sessions_user (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ai_chat_messages_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
