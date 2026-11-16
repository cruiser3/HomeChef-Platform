ALTER TABLE families
    ADD COLUMN invite_code VARCHAR(16) NULL;

ALTER TABLE families
    ADD CONSTRAINT uk_families_invite_code UNIQUE (invite_code);
