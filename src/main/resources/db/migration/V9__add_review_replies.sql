ALTER TABLE dish_reviews
    ADD COLUMN parent_id BIGINT NULL,
    MODIFY COLUMN rating INT NULL;

ALTER TABLE dish_reviews
    ADD CONSTRAINT fk_dish_reviews_parent
        FOREIGN KEY (parent_id) REFERENCES dish_reviews (id)
        ON DELETE CASCADE ON UPDATE RESTRICT;
