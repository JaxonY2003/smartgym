-- SmartGym Copilot - V11: meal plans.
-- This migration is schema-only. It does not insert business data.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS meal_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    goal VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    starts_on DATE NULL,
    ends_on DATE NULL,
    estimated_calories DECIMAL(10,2) NULL,
    estimated_protein_g DECIMAL(10,2) NULL,
    estimated_carbohydrate_g DECIMAL(10,2) NULL,
    estimated_fat_g DECIMAL(10,2) NULL,
    estimated_fiber_g DECIMAL(10,2) NULL,
    warnings_json JSON NULL,
    assumptions_json JSON NULL,
    confirmed_run_id VARCHAR(64) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_meal_plan_user_status (user_id, status, starts_on),
    KEY idx_meal_plan_confirmed_run (confirmed_run_id),
    KEY idx_meal_plan_deleted (user_id, deleted_at),
    CONSTRAINT fk_meal_plan_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划';

CREATE TABLE IF NOT EXISTS meal_plan_day (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_plan_id BIGINT UNSIGNED NOT NULL,
    plan_date DATE NOT NULL,
    day_index SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_meal_plan_day (meal_plan_id, plan_date),
    KEY idx_meal_plan_day_plan_order (meal_plan_id, day_index, id),
    CONSTRAINT fk_meal_plan_day_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划日';

CREATE TABLE IF NOT EXISTS meal_plan_meal (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_plan_day_id BIGINT UNSIGNED NOT NULL,
    meal_type VARCHAR(32) NOT NULL,
    meal_time TIME NULL,
    sort_order INT NOT NULL DEFAULT 0,
    notes VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_meal_plan_meal_day (meal_plan_day_id, sort_order, id),
    CONSTRAINT fk_meal_plan_meal_day
        FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_day (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划餐次';

CREATE TABLE IF NOT EXISTS meal_plan_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_plan_meal_id BIGINT UNSIGNED NOT NULL,
    food_id BIGINT UNSIGNED NULL,
    recipe_id BIGINT UNSIGNED NULL,
    food_name_snapshot VARCHAR(128) NOT NULL,
    amount DECIMAL(10,3) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    estimated_calories DECIMAL(10,2) NULL,
    estimated_protein_g DECIMAL(10,2) NULL,
    estimated_carbohydrate_g DECIMAL(10,2) NULL,
    estimated_fat_g DECIMAL(10,2) NULL,
    estimated_fiber_g DECIMAL(10,2) NULL,
    note VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_meal_plan_item_meal (meal_plan_meal_id, sort_order, id),
    KEY idx_meal_plan_item_food (food_id),
    KEY idx_meal_plan_item_recipe (recipe_id),
    CONSTRAINT fk_meal_plan_item_meal
        FOREIGN KEY (meal_plan_meal_id) REFERENCES meal_plan_meal (id),
    CONSTRAINT fk_meal_plan_item_food
        FOREIGN KEY (food_id) REFERENCES food (id),
    CONSTRAINT fk_meal_plan_item_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划食物项';
