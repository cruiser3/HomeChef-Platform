CREATE TABLE order_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_order_reviews_member_item UNIQUE (family_id, order_item_id, user_id),
    KEY idx_order_reviews_order (family_id, order_id, created_at, id),
    CONSTRAINT chk_order_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_order_reviews_content
        CHECK (content IS NULL OR CHAR_LENGTH(TRIM(content)) > 0),
    CONSTRAINT fk_reviews_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_order_same_family
        FOREIGN KEY (family_id, order_id)
        REFERENCES family_orders (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_order_item_same_order
        FOREIGN KEY (family_id, order_id, order_item_id)
        REFERENCES family_order_items (family_id, order_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_reviews_member
        FOREIGN KEY (family_id, user_id)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
