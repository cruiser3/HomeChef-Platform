-- =====================================================================
-- V19: 「小铲」用户级配置：对话 Provider + 角色设定（persona）
--
-- 目的：让用户为小铲对话自配 OpenAI 兼容 API（如 DeepSeek），
--       并为小铲单独维护一份角色设定（饮食偏好/忌口/过敏/健康情况）。
-- 设计：
--   - provider：OLLAMA（系统默认）或 OPENAI_COMPATIBLE（用户自配）。
--   - api_key_encrypted：AES-256-GCM 加密后的密文，永不落明文。
--   - persona_configured：标记用户是否已显式保存过角色设定；
--     为 false 时读取个人账户健康档案作为初始值。
--   - persona_* 字段格式与 users 表一致（逗号分隔的标签值）。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. DROP TABLE ai_chat_configs;
--   2. DELETE FROM flyway_schema_history WHERE version = '19';
-- =====================================================================

CREATE TABLE ai_chat_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'OLLAMA',
    base_url VARCHAR(500) NULL,
    api_key_encrypted VARCHAR(1000) NULL,
    model VARCHAR(100) NULL,
    persona_configured BOOLEAN NOT NULL DEFAULT FALSE,
    persona_preferences VARCHAR(200) NULL,
    persona_restrictions VARCHAR(200) NULL,
    persona_allergies VARCHAR(200) NULL,
    persona_conditions VARCHAR(200) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_ai_chat_configs_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
