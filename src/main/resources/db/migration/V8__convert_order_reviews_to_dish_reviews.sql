DROP TABLE order_reviews;

CREATE TABLE dish_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_dish_reviews_dish (family_id, dish_id, created_at, id),
    CONSTRAINT chk_dish_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_dish_reviews_content
        CHECK (content IS NULL OR CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT fk_dish_reviews_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dish_reviews_dish_same_family
        FOREIGN KEY (family_id, dish_id)
        REFERENCES dishes (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dish_reviews_member
        FOREIGN KEY (family_id, user_id)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
