-- =====================================================================
-- V14: dish_ingredients 增加 amount_text 字段，放宽 amount/unit 为可空
-- 设计依据：docs/food-square/SYSTEM_RECIPE_API_MODULE_DESIGN.md
--          （「复制到我家」保留「适量/少许」等无法量化食材的原文）
--
-- 目的：支持无法量化的食材在复制到家庭菜时保留原文，
--       与 system_dish_ingredients 的 amount_text 设计对齐。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. 删除 amount_text 列，恢复 amount/unit 非空，恢复原 CHECK 约束；
--   2. DELETE FROM flyway_schema_history WHERE version = '14';
-- =====================================================================

ALTER TABLE dish_ingredients
    ADD COLUMN amount_text VARCHAR(100) NULL AFTER unit,
    MODIFY COLUMN amount DECIMAL(10, 2) NULL,
    MODIFY COLUMN unit VARCHAR(20) NULL,
    DROP CHECK chk_dish_ingredients_amount,
    ADD CONSTRAINT chk_dish_ingredients_amount CHECK (amount IS NULL OR amount >= 0);
