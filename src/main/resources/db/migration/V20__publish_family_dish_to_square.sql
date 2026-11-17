-- =====================================================================
-- V20: 家庭菜品「发布到美食广场」
--
-- 目的：支持把「我家」的菜品快照复制到广场（SystemDish），并在广场
--       标注来源家庭与发布人；支持「最新」Tab 按用户发布过滤。
-- 设计：
--   - source_family_id：来源家庭 id（系统导入的 HOWTOCOOK 菜谱为 NULL）。
--   - published_by：发布人 user id（系统导入的菜谱为 NULL）。
--   - 用户发布的菜谱 source_type = 'FAMILY'，original_path 存
--     'family-{familyId}-dish-{dishId}'，用于唯一标识 + 覆盖更新。
--   - idx_system_dishes_source_type 服务于「最新」Tab 的过滤。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. DROP INDEX idx_system_dishes_source_type ON system_dishes;
--   2. ALTER TABLE system_dishes DROP COLUMN published_by, DROP COLUMN source_family_id;
--   3. DELETE FROM flyway_schema_history WHERE version = '20';
-- =====================================================================

ALTER TABLE system_dishes
    ADD COLUMN source_family_id BIGINT NULL,
    ADD COLUMN published_by BIGINT NULL;

CREATE INDEX idx_system_dishes_source_type ON system_dishes (source_type, id);
