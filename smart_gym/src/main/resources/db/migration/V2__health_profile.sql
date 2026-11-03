-- SmartGym Copilot - V2: member health profile and nutrition preferences.
-- All tables use the shared id, created_at and updated_at fields.
-- Health timestamps are stored with UTC semantics; date-only fields remain DATE.
-- food_id in food_preference is reserved for the future food catalog migration.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS member_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    gender VARCHAR(16) NULL,
    birth_date DATE NULL,
    height_cm DECIMAL(5,2) NULL,
    weight_kg DECIMAL(6,2) NULL,
    training_experience VARCHAR(32) NULL,
    weekly_training_days TINYINT UNSIGNED NULL,
    max_session_minutes SMALLINT UNSIGNED NULL,
    medical_notice VARCHAR(1000) NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_profile_user (user_id),
    CONSTRAINT fk_member_profile_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员健康基础档案';

CREATE TABLE IF NOT EXISTS training_goal (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    goal_type VARCHAR(32) NOT NULL,
    priority TINYINT UNSIGNED NOT NULL DEFAULT 1,
    target_weight_kg DECIMAL(6,2) NULL,
    target_body_fat_percent DECIMAL(5,2) NULL,
    target_date DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_training_goal_user_type_status (user_id, goal_type, status),
    KEY idx_training_goal_user_status (user_id, status),
    CONSTRAINT fk_training_goal_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练目标';

CREATE TABLE IF NOT EXISTS body_metric (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    measured_at DATETIME(3) NOT NULL,
    weight_kg DECIMAL(6,2) NULL,
    body_fat_percent DECIMAL(5,2) NULL,
    waist_cm DECIMAL(6,2) NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_body_metric_user_time (user_id, measured_at, id),
    CONSTRAINT fk_body_metric_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='身体指标记录';

CREATE TABLE IF NOT EXISTS health_constraint (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    constraint_type VARCHAR(32) NOT NULL,
    body_part VARCHAR(64) NULL,
    severity VARCHAR(32) NULL,
    description VARCHAR(1000) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    started_on DATE NULL,
    ended_on DATE NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_health_constraint_user_active (user_id, active),
    CONSTRAINT fk_health_constraint_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康限制和身体不适';

CREATE TABLE IF NOT EXISTS nutrition_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    diet_type VARCHAR(32) NOT NULL DEFAULT 'OMNIVORE',
    daily_water_target_ml INT UNSIGNED NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_nutrition_profile_user (user_id),
    CONSTRAINT fk_nutrition_profile_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食档案';

CREATE TABLE IF NOT EXISTS allergen (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_allergen_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='过敏原字典';

CREATE TABLE IF NOT EXISTS food_allergy (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    allergen_id BIGINT UNSIGNED NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    source VARCHAR(64) NOT NULL DEFAULT 'USER_EXPLICIT',
    active TINYINT(1) NOT NULL DEFAULT 1,
    note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_allergy_user_allergen (user_id, allergen_id),
    KEY idx_food_allergy_user_active (user_id, active),
    CONSTRAINT fk_food_allergy_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_food_allergy_allergen FOREIGN KEY (allergen_id) REFERENCES allergen (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户食物过敏';

CREATE TABLE IF NOT EXISTS dietary_restriction (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    restriction_code VARCHAR(64) NOT NULL,
    description VARCHAR(500) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_dietary_restriction_user_code (user_id, restriction_code),
    KEY idx_dietary_restriction_user_active (user_id, active),
    CONSTRAINT fk_dietary_restriction_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户饮食限制';

CREATE TABLE IF NOT EXISTS food_preference (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    preference_type VARCHAR(32) NOT NULL,
    food_id BIGINT UNSIGNED NULL,
    preference_key VARCHAR(128) NULL,
    preference_value VARCHAR(255) NOT NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'USER_EXPLICIT',
    confidence DECIMAL(4,3) NOT NULL DEFAULT 1.000,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_preference_user_type_value (user_id, preference_type, preference_value),
    KEY idx_food_preference_user (user_id, preference_type),
    KEY idx_food_preference_food (food_id),
    CONSTRAINT fk_food_preference_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户食物偏好';

-- Initial allergen dictionary used by the nutrition profile contract.
INSERT INTO allergen (code, name)
SELECT 'PEANUT', '花生'
WHERE NOT EXISTS (SELECT 1 FROM allergen WHERE code = 'PEANUT');

INSERT INTO allergen (code, name)
SELECT 'MILK', '牛奶'
WHERE NOT EXISTS (SELECT 1 FROM allergen WHERE code = 'MILK');

INSERT INTO allergen (code, name)
SELECT 'EGG', '鸡蛋'
WHERE NOT EXISTS (SELECT 1 FROM allergen WHERE code = 'EGG');

INSERT INTO allergen (code, name)
SELECT 'SOY', '大豆'
WHERE NOT EXISTS (SELECT 1 FROM allergen WHERE code = 'SOY');

INSERT INTO allergen (code, name)
SELECT 'WHEAT', '小麦'
WHERE NOT EXISTS (SELECT 1 FROM allergen WHERE code = 'WHEAT');

INSERT INTO allergen (code, name)
SELECT 'TREE_NUT', '坚果'
WHERE NOT EXISTS (SELECT 1 FROM allergen WHERE code = 'TREE_NUT');
