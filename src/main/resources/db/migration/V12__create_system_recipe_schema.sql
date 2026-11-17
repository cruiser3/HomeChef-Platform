-- =====================================================================
-- V12: 系统公共菜谱库（美食广场）表结构
-- 设计依据：docs/food-square/SYSTEM_RECIPE_DATABASE_DESIGN.md（第 3 节）
--
-- 本脚本为纯增量迁移：仅新增 4 张表，不 ALTER、不 DROP 任何已有表，
-- 不影响 V1~V11 已建立的 schema 与数据。
--
-- 新增表：
--   system_dish_categories   系统菜谱分类
--   system_dishes            系统公共菜谱主表
--   system_dish_ingredients  系统菜谱食材
--   system_dish_steps        系统菜谱制作步骤
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. DROP TABLE system_dish_steps;
--   2. DROP TABLE system_dish_ingredients;
--   3. DROP TABLE system_dishes;
--   4. DROP TABLE system_dish_categories;
--   5. DELETE FROM flyway_schema_history WHERE version = '12';
-- =====================================================================

CREATE TABLE system_dish_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_system_dish_categories_name UNIQUE (name),
    CONSTRAINT chk_system_dish_categories_sort CHECK (sort_no >= 0),
    CONSTRAINT chk_system_dish_categories_available CHECK (is_available IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE system_dishes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    cover_image_url VARCHAR(500) NULL,
    cooking_minutes INT NULL,
    difficulty INT NULL,
    calories INT NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'HOWTOCOOK',
    source_url VARCHAR(500) NULL,
    original_path VARCHAR(500) NULL,
    source_commit VARCHAR(64) NULL,
    license VARCHAR(50) NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_system_dishes_category (category_id, id),
    KEY idx_system_dishes_available (is_available, id),
    KEY idx_system_dishes_name (name),
    CONSTRAINT uk_system_dishes_source UNIQUE (source_type, original_path),
    CONSTRAINT chk_system_dishes_cooking CHECK (cooking_minutes IS NULL OR cooking_minutes >= 0),
    CONSTRAINT chk_system_dishes_difficulty CHECK (difficulty IS NULL OR difficulty BETWEEN 1 AND 5),
    CONSTRAINT chk_system_dishes_calories CHECK (calories IS NULL OR calories >= 0),
    CONSTRAINT chk_system_dishes_available CHECK (is_available IN (0, 1)),
    CONSTRAINT fk_system_dishes_category
        FOREIGN KEY (category_id) REFERENCES system_dish_categories (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE system_dish_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(10, 2) NULL,
    unit VARCHAR(20) NULL,
    amount_text VARCHAR(100) NULL,
    is_optional BOOLEAN NOT NULL DEFAULT FALSE,
    sort_no INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_dish_ingredients_dish_sort UNIQUE (dish_id, sort_no),
    CONSTRAINT chk_system_dish_ingredients_amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT chk_system_dish_ingredients_sort CHECK (sort_no >= 0),
    CONSTRAINT chk_system_dish_ingredients_optional CHECK (is_optional IN (0, 1)),
    CONSTRAINT fk_system_dish_ingredients_dish
        FOREIGN KEY (dish_id) REFERENCES system_dishes (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE system_dish_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    image_url VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_system_dish_steps_dish_step UNIQUE (dish_id, step_no),
    CONSTRAINT chk_system_dish_steps_step_no CHECK (step_no >= 1),
    CONSTRAINT fk_system_dish_steps_dish
        FOREIGN KEY (dish_id) REFERENCES system_dishes (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
