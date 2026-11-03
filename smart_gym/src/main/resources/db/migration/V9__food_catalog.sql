-- SmartGym Copilot - V9: food catalog and nutrition source data.
-- This migration is schema-only. Food seed data is kept in a later migration.
-- The product is a general-purpose fitness application; no gym fields are used.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS food_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT UNSIGNED NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_category_code (code),
    KEY idx_food_category_parent_status (parent_id, status, sort_order),
    CONSTRAINT fk_food_category_parent
        FOREIGN KEY (parent_id) REFERENCES food_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物分类';

CREATE TABLE IF NOT EXISTS food (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NULL,
    name VARCHAR(128) NOT NULL,
    english_name VARCHAR(128) NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(128) NOT NULL DEFAULT 'SYSTEM',
    source_version VARCHAR(64) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_food_name_status (name, status),
    KEY idx_food_category_status (category_id, status),
    CONSTRAINT fk_food_category
        FOREIGN KEY (category_id) REFERENCES food_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物目录';

CREATE TABLE IF NOT EXISTS food_nutrition (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    food_id BIGINT UNSIGNED NOT NULL,
    serving_amount DECIMAL(10,3) NOT NULL DEFAULT 100,
    serving_unit VARCHAR(16) NOT NULL DEFAULT 'g',
    calories DECIMAL(10,2) NOT NULL DEFAULT 0,
    protein_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    carbohydrate_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    fat_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    fiber_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    sodium_mg DECIMAL(10,2) NOT NULL DEFAULT 0,
    sugar_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    source VARCHAR(128) NOT NULL DEFAULT 'SYSTEM',
    source_version VARCHAR(64) NOT NULL DEFAULT 'UNKNOWN',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_nutrition_serving (food_id, serving_amount, serving_unit, source_version),
    KEY idx_food_nutrition_food (food_id),
    CONSTRAINT fk_food_nutrition_food
        FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物营养数据';

CREATE TABLE IF NOT EXISTS food_serving (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    food_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(64) NOT NULL,
    amount DECIMAL(10,3) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    gram_weight DECIMAL(10,3) NULL,
    is_default TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_food_serving_food_default (food_id, is_default, id),
    CONSTRAINT fk_food_serving_food
        FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物常用份量';

CREATE TABLE IF NOT EXISTS food_allergen (
    food_id BIGINT UNSIGNED NOT NULL,
    allergen_id BIGINT UNSIGNED NOT NULL,
    source VARCHAR(128) NOT NULL DEFAULT 'SYSTEM',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (food_id, allergen_id),
    KEY idx_food_allergen_allergen (allergen_id, food_id),
    CONSTRAINT fk_food_allergen_food
        FOREIGN KEY (food_id) REFERENCES food (id),
    CONSTRAINT fk_food_allergen_allergen
        FOREIGN KEY (allergen_id) REFERENCES allergen (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物过敏原关系';

ALTER TABLE food_preference
    ADD CONSTRAINT fk_food_preference_food
        FOREIGN KEY (food_id) REFERENCES food (id);
