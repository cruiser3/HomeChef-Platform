-- =====================================================================
-- V21: 广场菜品评论（system_dish_reviews）
--
-- 目的：为「美食广场」的每个菜品提供评论能力，形式与家庭菜品评论
--       （dish_reviews）一致：支持评分、文字、图片、匿名、回复。
-- 差异：广场是全局空间，无家庭隔离，因此去掉了 family_id，并改为
--       关联 system_dishes；所有接口都要求登录（未登录不可浏览）。
--
-- 回滚方式（如确需撤销，须人工审批）：
--   1. DROP TABLE system_dish_reviews;
--   2. DELETE FROM flyway_schema_history WHERE version = '21';
-- =====================================================================

CREATE TABLE system_dish_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    rating INT NULL,
    content VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_system_dish_reviews_dish (dish_id, created_at, id),
    CONSTRAINT chk_system_dish_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_system_dish_reviews_content
        CHECK (content IS NULL OR CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT fk_system_dish_reviews_dish
        FOREIGN KEY (dish_id) REFERENCES system_dishes (id)
        ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT fk_system_dish_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_system_dish_reviews_parent
        FOREIGN KEY (parent_id) REFERENCES system_dish_reviews (id)
        ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
