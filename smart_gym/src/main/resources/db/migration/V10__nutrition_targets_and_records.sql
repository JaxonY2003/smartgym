-- SmartGym Copilot - V10: nutrition targets, meal records and water records.
-- This migration is schema-only. It does not insert business data.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS recipe (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1000) NULL,
    serving_count DECIMAL(8,2) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_recipe_name_status (name, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食谱';

CREATE TABLE IF NOT EXISTS recipe_ingredient (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT UNSIGNED NOT NULL,
    food_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(10,3) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_recipe_ingredient_recipe (recipe_id, sort_order, id),
    KEY idx_recipe_ingredient_food (food_id),
    CONSTRAINT fk_recipe_ingredient_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe (id),
    CONSTRAINT fk_recipe_ingredient_food
        FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食谱食材';

CREATE TABLE IF NOT EXISTS nutrition_target (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    effective_date DATE NOT NULL,
    goal VARCHAR(32) NULL,
    calories DECIMAL(10,2) NULL,
    protein_g DECIMAL(10,2) NULL,
    carbohydrate_g DECIMAL(10,2) NULL,
    fat_g DECIMAL(10,2) NULL,
    fiber_g DECIMAL(10,2) NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'SYSTEM_CALCULATED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_nutrition_target_user_date (user_id, effective_date),
    KEY idx_nutrition_target_user_goal (user_id, goal, effective_date),
    CONSTRAINT fk_nutrition_target_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日营养目标';

CREATE TABLE IF NOT EXISTS meal_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    eaten_at DATETIME(3) NOT NULL,
    meal_type VARCHAR(32) NOT NULL,
    input_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    total_calories DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_protein_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_carbohydrate_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_fat_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_fiber_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    note VARCHAR(2000) NULL,
    confirmed_by_user TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_meal_record_user_time (user_id, eaten_at, id),
    KEY idx_meal_record_user_meal_type (user_id, meal_type, eaten_at),
    CONSTRAINT fk_meal_record_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食记录';

CREATE TABLE IF NOT EXISTS meal_record_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_record_id BIGINT UNSIGNED NOT NULL,
    food_id BIGINT UNSIGNED NULL,
    recipe_id BIGINT UNSIGNED NULL,
    food_name_snapshot VARCHAR(128) NOT NULL,
    amount DECIMAL(10,3) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    estimated_calories DECIMAL(10,2) NOT NULL DEFAULT 0,
    estimated_protein_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    estimated_carbohydrate_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    estimated_fat_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    estimated_fiber_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    input_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    confidence DECIMAL(4,3) NOT NULL DEFAULT 1.000,
    confirmed_by_user TINYINT(1) NOT NULL DEFAULT 1,
    assumptions_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_meal_record_item_record (meal_record_id, id),
    KEY idx_meal_record_item_food (food_id),
    KEY idx_meal_record_item_recipe (recipe_id),
    CONSTRAINT fk_meal_record_item_record
        FOREIGN KEY (meal_record_id) REFERENCES meal_record (id),
    CONSTRAINT fk_meal_record_item_food
        FOREIGN KEY (food_id) REFERENCES food (id),
    CONSTRAINT fk_meal_record_item_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食记录明细';

CREATE TABLE IF NOT EXISTS daily_nutrition_summary (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    summary_date DATE NOT NULL,
    target_calories DECIMAL(10,2) NULL,
    target_protein_g DECIMAL(10,2) NULL,
    target_carbohydrate_g DECIMAL(10,2) NULL,
    target_fat_g DECIMAL(10,2) NULL,
    target_fiber_g DECIMAL(10,2) NULL,
    consumed_calories DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_protein_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_carbohydrate_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_fat_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_fiber_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    meal_count INT UNSIGNED NOT NULL DEFAULT 0,
    record_completeness DECIMAL(5,4) NOT NULL DEFAULT 0,
    calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_nutrition_summary_user_date (user_id, summary_date),
    KEY idx_daily_nutrition_summary_date (summary_date, user_id),
    CONSTRAINT fk_daily_nutrition_summary_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日营养汇总';

CREATE TABLE IF NOT EXISTS water_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    recorded_at DATETIME(3) NOT NULL,
    amount_ml INT UNSIGNED NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_water_record_user_time (user_id, recorded_at, id),
    CONSTRAINT fk_water_record_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮水记录';
