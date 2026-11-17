-- =====================================================================
-- V15: 新增「菜系」维度（cuisine）
--
-- 目的：
--   1. 新增全局菜系表 cuisines（不分家庭，粤菜/东北菜等对所有家庭通用）。
--   2. 家庭菜品 dishes 增加 cuisine_id（菜系）与 calories（卡路里，可选）。
--   3. 系统菜谱 system_dishes 增加 cuisine_id（菜系）。
--
-- 说明：
--   - 菜系是独立于现有「食材类型分类」（dish_categories / system_dish_categories）
--     的正交维度。
--   - 外键 ON DELETE SET NULL：删除菜系时菜品/菜谱的菜系自动置空，不级联删除菜品。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. ALTER TABLE dishes DROP FOREIGN KEY fk_dishes_cuisine,
--        DROP COLUMN cuisine_id, DROP COLUMN calories, DROP CHECK chk_dishes_calories;
--   2. ALTER TABLE system_dishes DROP FOREIGN KEY fk_system_dishes_cuisine,
--        DROP COLUMN cuisine_id;
--   3. DROP TABLE cuisines;
--   4. DELETE FROM flyway_schema_history WHERE version = '15';
-- =====================================================================

CREATE TABLE cuisines (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cuisines_name UNIQUE (name),
    CONSTRAINT chk_cuisines_sort CHECK (sort_no >= 0),
    CONSTRAINT chk_cuisines_available CHECK (is_available IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO cuisines (name, sort_no) VALUES
    ('家常菜', 1),
    ('徽菜', 2),
    ('粤菜', 3),
    ('东北菜', 4),
    ('海南菜', 5),
    ('川菜', 6),
    ('湘菜', 7),
    ('西餐', 8),
    ('日料', 9),
    ('韩餐', 10),
    ('东南亚菜', 11),
    ('其他', 12);

ALTER TABLE dishes
    ADD COLUMN cuisine_id BIGINT NULL AFTER category_id,
    ADD COLUMN calories INT NULL AFTER cooking_minutes,
    ADD CONSTRAINT chk_dishes_calories CHECK (calories IS NULL OR calories >= 0),
    ADD CONSTRAINT fk_dishes_cuisine
        FOREIGN KEY (cuisine_id) REFERENCES cuisines (id)
        ON DELETE SET NULL ON UPDATE RESTRICT;

ALTER TABLE system_dishes
    ADD COLUMN cuisine_id BIGINT NULL AFTER category_id,
    ADD CONSTRAINT fk_system_dishes_cuisine
        FOREIGN KEY (cuisine_id) REFERENCES cuisines (id)
        ON DELETE SET NULL ON UPDATE RESTRICT;
