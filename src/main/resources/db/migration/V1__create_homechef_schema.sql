CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    `role` VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (`role` IN ('CUSTOMER', 'RESTAURANT_OWNER', 'ADMIN')),
    CONSTRAINT chk_users_active CHECK (active IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE families (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_families_created_by (created_by),
    CONSTRAINT fk_families_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE family_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    left_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_family_members_family_user UNIQUE (family_id, user_id),
    KEY idx_family_members_user (user_id),
    CONSTRAINT chk_family_members_role CHECK (`role` IN ('ADMIN', 'MEMBER')),
    CONSTRAINT chk_family_members_leave_time
        CHECK (left_at IS NULL OR left_at >= joined_at),
    CONSTRAINT fk_family_members_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_members_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dish_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_dish_categories_family_name UNIQUE (family_id, name),
    CONSTRAINT uk_dish_categories_family_id UNIQUE (family_id, id),
    KEY idx_dish_categories_family_sort (family_id, sort_no, id),
    CONSTRAINT chk_dish_categories_sort CHECK (sort_no >= 0),
    CONSTRAINT fk_dish_categories_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dishes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    image_url VARCHAR(500) NULL,
    category_id BIGINT NULL,
    cooking_minutes INT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_by BIGINT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_dishes_family_id UNIQUE (family_id, id),
    KEY idx_dishes_family_created (family_id, created_at, id),
    KEY idx_dishes_family_category_created (family_id, category_id, created_at, id),
    KEY idx_dishes_family_available_created (family_id, is_available, created_at, id),
    CONSTRAINT chk_dishes_price CHECK (price >= 0),
    CONSTRAINT chk_dishes_cooking_minutes
        CHECK (cooking_minutes IS NULL OR cooking_minutes >= 0),
    CONSTRAINT chk_dishes_available CHECK (is_available IN (0, 1)),
    CONSTRAINT fk_dishes_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dishes_category_same_family
        FOREIGN KEY (family_id, category_id)
        REFERENCES dish_categories (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_dishes_creator_membership
        FOREIGN KEY (family_id, created_by)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dish_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_dish_ingredients_dish_sort UNIQUE (dish_id, sort_no),
    CONSTRAINT chk_dish_ingredients_amount CHECK (amount >= 0),
    CONSTRAINT chk_dish_ingredients_sort CHECK (sort_no >= 0),
    CONSTRAINT fk_dish_ingredients_dish
        FOREIGN KEY (dish_id) REFERENCES dishes (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE dish_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    dish_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    content VARCHAR(500) NOT NULL,
    image_url VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_dish_steps_dish_step UNIQUE (dish_id, step_no),
    CONSTRAINT chk_dish_steps_step_no CHECK (step_no >= 1),
    CONSTRAINT fk_dish_steps_dish
        FOREIGN KEY (dish_id) REFERENCES dishes (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    family_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_user_family_dish UNIQUE (user_id, family_id, dish_id),
    KEY idx_cart_user_family_created (user_id, family_id, created_at, id),
    CONSTRAINT chk_cart_quantity CHECK (quantity BETWEEN 1 AND 99),
    CONSTRAINT chk_cart_version CHECK (version >= 0),
    CONSTRAINT fk_cart_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cart_member
        FOREIGN KEY (family_id, user_id)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_cart_dish_same_family
        FOREIGN KEY (family_id, dish_id)
        REFERENCES dishes (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE family_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    item_count INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    meal_period VARCHAR(20) NULL,
    remark VARCHAR(500) NULL,
    meal_served BOOLEAN NOT NULL DEFAULT FALSE,
    meal_served_at DATETIME(6) NULL,
    cancelled_at DATETIME(6) NULL,
    cancelled_by BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_family_orders_family_id UNIQUE (family_id, id),
    KEY idx_family_orders_family_created (family_id, created_at, id),
    KEY idx_family_orders_family_user_created (family_id, user_id, created_at, id),
    CONSTRAINT chk_family_orders_status
        CHECK (status IN ('CREATED', 'PREPARING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_family_orders_amount_count
        CHECK (total_amount >= 0 AND item_count > 0),
    CONSTRAINT chk_family_orders_meal_served
        CHECK ((meal_served = FALSE AND meal_served_at IS NULL)
            OR (meal_served = TRUE AND meal_served_at IS NOT NULL)),
    CONSTRAINT chk_family_orders_cancellation
        CHECK ((status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
            OR (status <> 'CANCELLED' AND cancelled_at IS NULL AND cancelled_by IS NULL)),
    CONSTRAINT chk_family_orders_version CHECK (version >= 0),
    CONSTRAINT fk_family_orders_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_orders_member
        FOREIGN KEY (family_id, user_id)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_orders_canceller_member
        FOREIGN KEY (family_id, cancelled_by)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE family_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    dish_name_snapshot VARCHAR(100) NOT NULL,
    unit_price_snapshot DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    cook_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    responsible_user_id BIGINT NULL,
    blocked_reason VARCHAR(200) NULL,
    cook_status_updated_by BIGINT NULL,
    cook_status_updated_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_family_order_items_family_order_id
        UNIQUE (family_id, order_id, id),
    KEY idx_family_order_items_order (order_id, id),
    CONSTRAINT chk_family_order_items_cook_status
        CHECK (cook_status IN ('PENDING', 'COOKING', 'READY', 'BLOCKED', 'SERVED')),
    CONSTRAINT chk_family_order_items_values
        CHECK (unit_price_snapshot >= 0
            AND quantity BETWEEN 1 AND 99
            AND subtotal = unit_price_snapshot * quantity),
    CONSTRAINT chk_family_order_items_blocked_reason
        CHECK ((cook_status = 'BLOCKED'
                AND blocked_reason IS NOT NULL
                AND CHAR_LENGTH(TRIM(blocked_reason)) > 0)
            OR (cook_status <> 'BLOCKED' AND blocked_reason IS NULL)),
    CONSTRAINT chk_family_order_items_status_audit
        CHECK ((cook_status_updated_by IS NULL AND cook_status_updated_at IS NULL)
            OR (cook_status_updated_by IS NOT NULL AND cook_status_updated_at IS NOT NULL)),
    CONSTRAINT chk_family_order_items_version CHECK (version >= 0),
    CONSTRAINT fk_family_order_items_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_order_items_order_same_family
        FOREIGN KEY (family_id, order_id)
        REFERENCES family_orders (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_order_items_dish_same_family
        FOREIGN KEY (family_id, dish_id)
        REFERENCES dishes (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_order_items_responsible_member
        FOREIGN KEY (family_id, responsible_user_id)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_family_order_items_status_member
        FOREIGN KEY (family_id, cook_status_updated_by)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE family_shopping_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    order_id BIGINT NULL,
    order_item_id BIGINT NULL,
    added_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_shopping_family_created (family_id, created_at, id),
    KEY idx_shopping_family_order_item (family_id, order_id, order_item_id),
    CONSTRAINT chk_shopping_amount CHECK (amount > 0),
    CONSTRAINT chk_shopping_order_item_requires_order
        CHECK (order_item_id IS NULL OR order_id IS NOT NULL),
    CONSTRAINT fk_shopping_family
        FOREIGN KEY (family_id) REFERENCES families (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_shopping_adder_member
        FOREIGN KEY (family_id, added_by)
        REFERENCES family_members (family_id, user_id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_shopping_order_same_family
        FOREIGN KEY (family_id, order_id)
        REFERENCES family_orders (family_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_shopping_order_item_matches_order
        FOREIGN KEY (family_id, order_id, order_item_id)
        REFERENCES family_order_items (family_id, order_id, id)
        ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
