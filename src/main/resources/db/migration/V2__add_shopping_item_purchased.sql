ALTER TABLE family_shopping_items
    ADD COLUMN purchased BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN purchased_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_shopping_purchased CHECK (purchased IN (0, 1));
