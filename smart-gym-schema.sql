-- SmartGym Copilot
-- MySQL 8.0+ baseline schema
-- Encoding: utf8mb4 / Collation: utf8mb4_0900_ai_ci
-- This file is intended to be used as the first Flyway migration.
-- It does not contain DROP statements. Production changes should be delivered by later migrations.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_account (account),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    user_agent VARCHAR(512) NULL,
    client_ip VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_token_user (user_id, expires_at),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷新令牌';

CREATE TABLE IF NOT EXISTS gym (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_gym_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健身房';

CREATE TABLE IF NOT EXISTS gym_user (
    gym_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    membership_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at DATETIME(3) NULL,
    PRIMARY KEY (gym_id, user_id),
    KEY idx_gym_user_user (user_id, membership_status),
    CONSTRAINT fk_gym_user_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
    CONSTRAINT fk_gym_user_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健身房用户关系';

CREATE TABLE IF NOT EXISTS member_profile (
    user_id BIGINT UNSIGNED NOT NULL,
    gender VARCHAR(16) NULL,
    birth_date DATE NULL,
    height_cm DECIMAL(5,2) NULL,
    weight_kg DECIMAL(6,2) NULL,
    training_experience VARCHAR(32) NULL,
    weekly_training_days TINYINT UNSIGNED NULL,
    max_session_minutes SMALLINT UNSIGNED NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_member_profile_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员健康基础档案';

CREATE TABLE IF NOT EXISTS body_metric (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    measured_at DATETIME(3) NOT NULL,
    weight_kg DECIMAL(6,2) NULL,
    body_fat_percent DECIMAL(5,2) NULL,
    waist_cm DECIMAL(6,2) NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_body_metric_user_time (user_id, measured_at),
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
    KEY idx_training_goal_user_status (user_id, status),
    CONSTRAINT fk_training_goal_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练目标';

CREATE TABLE IF NOT EXISTS nutrition_profile (
    user_id BIGINT UNSIGNED NOT NULL,
    diet_type VARCHAR(32) NOT NULL DEFAULT 'OMNIVORE',
    daily_water_target_ml INT UNSIGNED NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_nutrition_profile_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食档案';

CREATE TABLE IF NOT EXISTS allergen (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_allergen_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='过敏原字典';

CREATE TABLE IF NOT EXISTS food_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_category_code (code),
    CONSTRAINT fk_food_category_parent FOREIGN KEY (parent_id) REFERENCES food_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物分类';

CREATE TABLE IF NOT EXISTS food (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NULL,
    name VARCHAR(128) NOT NULL,
    english_name VARCHAR(128) NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(128) NULL,
    source_version VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_food_name (name),
    KEY idx_food_category_status (category_id, status),
    CONSTRAINT fk_food_category FOREIGN KEY (category_id) REFERENCES food_category (id)
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
    source VARCHAR(128) NULL,
    source_version VARCHAR(64) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_nutrition_serving (food_id, serving_amount, serving_unit, source_version),
    KEY idx_food_nutrition_food (food_id),
    CONSTRAINT fk_food_nutrition_food FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物营养数据';

CREATE TABLE IF NOT EXISTS food_serving (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    food_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(64) NOT NULL,
    amount DECIMAL(10,3) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    gram_weight DECIMAL(10,3) NULL,
    PRIMARY KEY (id),
    KEY idx_food_serving_food (food_id),
    CONSTRAINT fk_food_serving_food FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物常用份量';

CREATE TABLE IF NOT EXISTS food_allergen (
    food_id BIGINT UNSIGNED NOT NULL,
    allergen_id BIGINT UNSIGNED NOT NULL,
    source VARCHAR(128) NULL,
    PRIMARY KEY (food_id, allergen_id),
    CONSTRAINT fk_food_allergen_food FOREIGN KEY (food_id) REFERENCES food (id),
    CONSTRAINT fk_food_allergen_allergen FOREIGN KEY (allergen_id) REFERENCES allergen (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食物过敏原关系';

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
    KEY idx_food_preference_user (user_id, preference_type),
    KEY idx_food_preference_food (food_id),
    CONSTRAINT fk_food_preference_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_food_preference_food FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户食物偏好';

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
    CONSTRAINT fk_dietary_restriction_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户饮食限制';

CREATE TABLE IF NOT EXISTS muscle_group (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT UNSIGNED NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_muscle_group_code (code),
    CONSTRAINT fk_muscle_group_parent FOREIGN KEY (parent_id) REFERENCES muscle_group (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='肌群';

CREATE TABLE IF NOT EXISTS exercise (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    english_name VARCHAR(128) NULL,
    description TEXT NULL,
    difficulty VARCHAR(32) NOT NULL DEFAULT 'BEGINNER',
    instructions TEXT NULL,
    contraindications_json JSON NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_exercise_name_status (name, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练动作';

CREATE TABLE IF NOT EXISTS exercise_muscle (
    exercise_id BIGINT UNSIGNED NOT NULL,
    muscle_group_id BIGINT UNSIGNED NOT NULL,
    muscle_role VARCHAR(32) NOT NULL DEFAULT 'PRIMARY',
    PRIMARY KEY (exercise_id, muscle_group_id),
    CONSTRAINT fk_exercise_muscle_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
    CONSTRAINT fk_exercise_muscle_group FOREIGN KEY (muscle_group_id) REFERENCES muscle_group (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动作肌群关系';

CREATE TABLE IF NOT EXISTS equipment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    brand VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_equipment_name_status (name, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='器械类型';

CREATE TABLE IF NOT EXISTS gym_equipment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    gym_id BIGINT UNSIGNED NOT NULL,
    equipment_id BIGINT UNSIGNED NOT NULL,
    asset_code VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    quantity INT UNSIGNED NOT NULL DEFAULT 1,
    note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_gym_equipment_asset_code (gym_id, asset_code),
    KEY idx_gym_equipment_lookup (gym_id, equipment_id, status),
    CONSTRAINT fk_gym_equipment_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
    CONSTRAINT fk_gym_equipment_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健身房器械实例';

CREATE TABLE IF NOT EXISTS exercise_equipment (
    exercise_id BIGINT UNSIGNED NOT NULL,
    equipment_id BIGINT UNSIGNED NOT NULL,
    required TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (exercise_id, equipment_id),
    CONSTRAINT fk_exercise_equipment_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
    CONSTRAINT fk_exercise_equipment_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动作器械关系';

CREATE TABLE IF NOT EXISTS workout_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    gym_id BIGINT UNSIGNED NULL,
    name VARCHAR(128) NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    goal VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    starts_on DATE NULL,
    ends_on DATE NULL,
    duration_weeks SMALLINT UNSIGNED NULL,
    training_days_per_week TINYINT UNSIGNED NULL,
    warnings_json JSON NULL,
    assumptions_json JSON NULL,
    confirmed_run_id VARCHAR(64) NULL,
    version INT UNSIGNED NOT NULL DEFAULT 1,
    created_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_workout_plan_user_status (user_id, status, starts_on),
    KEY idx_workout_plan_confirmed_run (confirmed_run_id),
    CONSTRAINT fk_workout_plan_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_workout_plan_gym FOREIGN KEY (gym_id) REFERENCES gym (id),
    CONSTRAINT fk_workout_plan_creator FOREIGN KEY (created_by_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练计划';

CREATE TABLE IF NOT EXISTS workout_plan_day (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_id BIGINT UNSIGNED NOT NULL,
    day_no SMALLINT UNSIGNED NOT NULL,
    day_of_week TINYINT UNSIGNED NULL,
    title VARCHAR(128) NOT NULL,
    estimated_minutes SMALLINT UNSIGNED NULL,
    notes VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_plan_day_no (plan_id, day_no),
    CONSTRAINT fk_workout_plan_day_plan FOREIGN KEY (plan_id) REFERENCES workout_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练计划日';

CREATE TABLE IF NOT EXISTS workout_plan_exercise (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_day_id BIGINT UNSIGNED NOT NULL,
    exercise_id BIGINT UNSIGNED NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    sets TINYINT UNSIGNED NOT NULL DEFAULT 1,
    target_reps_min SMALLINT UNSIGNED NULL,
    target_reps_max SMALLINT UNSIGNED NULL,
    target_weight_kg DECIMAL(8,2) NULL,
    rest_seconds SMALLINT UNSIGNED NULL,
    tempo VARCHAR(32) NULL,
    notes VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_workout_plan_exercise_day (plan_day_id, sort_order),
    CONSTRAINT fk_workout_plan_exercise_day FOREIGN KEY (plan_day_id) REFERENCES workout_plan_day (id),
    CONSTRAINT fk_workout_plan_exercise_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练计划动作';

CREATE TABLE IF NOT EXISTS workout_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    plan_id BIGINT UNSIGNED NULL,
    plan_day_id BIGINT UNSIGNED NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at DATETIME(3) NULL,
    ended_at DATETIME(3) NULL,
    fatigue_level TINYINT UNSIGNED NULL,
    pain_body_part VARCHAR(64) NULL,
    pain_level TINYINT UNSIGNED NULL,
    total_volume_kg DECIMAL(12,2) NOT NULL DEFAULT 0,
    note VARCHAR(2000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_workout_session_user_time (user_id, started_at),
    KEY idx_workout_session_plan (plan_id, plan_day_id),
    CONSTRAINT fk_workout_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_workout_session_plan FOREIGN KEY (plan_id) REFERENCES workout_plan (id),
    CONSTRAINT fk_workout_session_plan_day FOREIGN KEY (plan_day_id) REFERENCES workout_plan_day (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练执行记录';

CREATE TABLE IF NOT EXISTS workout_set_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    exercise_id BIGINT UNSIGNED NOT NULL,
    set_index SMALLINT UNSIGNED NOT NULL,
    weight_kg DECIMAL(8,2) NULL,
    reps SMALLINT UNSIGNED NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    rest_seconds SMALLINT UNSIGNED NULL,
    rpe DECIMAL(4,1) NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_set_record_order (session_id, exercise_id, set_index),
    CONSTRAINT fk_workout_set_record_session FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_workout_set_record_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练组记录';

CREATE TABLE IF NOT EXISTS workout_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    fatigue_level TINYINT UNSIGNED NULL,
    pain_body_part VARCHAR(64) NULL,
    pain_level TINYINT UNSIGNED NULL,
    feeling VARCHAR(2000) NULL,
    should_adjust_next_plan TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_workout_feedback_session (session_id),
    CONSTRAINT fk_workout_feedback_session FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_workout_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练反馈';

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
    CONSTRAINT fk_nutrition_target_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日营养目标';

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
    PRIMARY KEY (id),
    KEY idx_recipe_ingredient_recipe (recipe_id, sort_order),
    CONSTRAINT fk_recipe_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id),
    CONSTRAINT fk_recipe_ingredient_food FOREIGN KEY (food_id) REFERENCES food (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='食谱食材';

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
    CONSTRAINT fk_meal_plan_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划';

CREATE TABLE IF NOT EXISTS meal_plan_day (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_plan_id BIGINT UNSIGNED NOT NULL,
    plan_date DATE NOT NULL,
    day_index SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uk_meal_plan_day (meal_plan_id, plan_date),
    CONSTRAINT fk_meal_plan_day_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plan (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划日';

CREATE TABLE IF NOT EXISTS meal_plan_meal (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    meal_plan_day_id BIGINT UNSIGNED NOT NULL,
    meal_type VARCHAR(32) NOT NULL,
    meal_time TIME NULL,
    sort_order INT NOT NULL DEFAULT 0,
    notes VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_meal_plan_meal_day (meal_plan_day_id, sort_order),
    CONSTRAINT fk_meal_plan_meal_day FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_day (id)
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
    note VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_meal_plan_item_meal (meal_plan_meal_id, sort_order),
    CONSTRAINT fk_meal_plan_item_meal FOREIGN KEY (meal_plan_meal_id) REFERENCES meal_plan_meal (id),
    CONSTRAINT fk_meal_plan_item_food FOREIGN KEY (food_id) REFERENCES food (id),
    CONSTRAINT fk_meal_plan_item_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食计划食物项';

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
    KEY idx_meal_record_user_time (user_id, eaten_at),
    CONSTRAINT fk_meal_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
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
    PRIMARY KEY (id),
    KEY idx_meal_record_item_record (meal_record_id),
    CONSTRAINT fk_meal_record_item_record FOREIGN KEY (meal_record_id) REFERENCES meal_record (id),
    CONSTRAINT fk_meal_record_item_food FOREIGN KEY (food_id) REFERENCES food (id),
    CONSTRAINT fk_meal_record_item_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食记录明细';

CREATE TABLE IF NOT EXISTS daily_nutrition_summary (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    summary_date DATE NOT NULL,
    target_calories DECIMAL(10,2) NULL,
    target_protein_g DECIMAL(10,2) NULL,
    target_carbohydrate_g DECIMAL(10,2) NULL,
    target_fat_g DECIMAL(10,2) NULL,
    consumed_calories DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_protein_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_carbohydrate_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_fat_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    consumed_fiber_g DECIMAL(10,2) NOT NULL DEFAULT 0,
    meal_count INT UNSIGNED NOT NULL DEFAULT 0,
    record_completeness DECIMAL(5,4) NOT NULL DEFAULT 0,
    calculated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_nutrition_summary_user_date (user_id, summary_date),
    CONSTRAINT fk_daily_nutrition_summary_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日营养汇总';

CREATE TABLE IF NOT EXISTS water_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    recorded_at DATETIME(3) NOT NULL,
    amount_ml INT UNSIGNED NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_water_record_user_time (user_id, recorded_at),
    CONSTRAINT fk_water_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮水记录';

CREATE TABLE IF NOT EXISTS knowledge_source (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_url VARCHAR(1024) NULL,
    publisher VARCHAR(255) NULL,
    license VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_knowledge_source_type (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识来源';

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_id BIGINT UNSIGNED NULL,
    title VARCHAR(255) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    source_url VARCHAR(1024) NULL,
    version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATE NULL,
    metadata_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_document_hash_version (content_hash, version),
    KEY idx_knowledge_document_type_status (document_type, status),
    CONSTRAINT fk_knowledge_document_source FOREIGN KEY (source_id) REFERENCES knowledge_source (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文档';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    document_id BIGINT UNSIGNED NOT NULL,
    chunk_no INT UNSIGNED NOT NULL,
    content LONGTEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    embedding_model VARCHAR(128) NULL,
    embedding_json JSON NULL,
    metadata_json JSON NULL,
    token_count INT UNSIGNED NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INDEXED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_chunk_document_no (document_id, chunk_no),
    KEY idx_knowledge_chunk_status (status),
    FULLTEXT KEY ft_knowledge_chunk_content (content),
    CONSTRAINT fk_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识文档分块和向量';

CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    prompt_name VARCHAR(128) NOT NULL,
    version VARCHAR(64) NOT NULL,
    content LONGTEXT NOT NULL,
    model_name VARCHAR(128) NULL,
    temperature DECIMAL(4,3) NULL,
    max_tokens INT UNSIGNED NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    created_by_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_prompt_version_name_version (prompt_name, version),
    KEY idx_prompt_version_enabled (prompt_name, enabled),
    CONSTRAINT fk_prompt_version_creator FOREIGN KEY (created_by_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Prompt 版本';

CREATE TABLE IF NOT EXISTS agent_conversation (
    id VARCHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_message_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_agent_conversation_user_time (user_id, updated_at),
    CONSTRAINT fk_agent_conversation_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 会话';

CREATE TABLE IF NOT EXISTS agent_message (
    id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    run_id VARCHAR(64) NULL,
    sequence_no INT UNSIGNED NOT NULL,
    role VARCHAR(16) NOT NULL,
    content LONGTEXT NULL,
    content_json JSON NULL,
    client_message_id VARCHAR(128) NULL,
    input_tokens INT UNSIGNED NULL,
    output_tokens INT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_message_client_id (conversation_id, client_message_id),
    UNIQUE KEY uk_agent_message_sequence (conversation_id, sequence_no),
    KEY idx_agent_message_run (run_id),
    CONSTRAINT fk_agent_message_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 会话消息';

CREATE TABLE IF NOT EXISTS agent_run (
    id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    request_message_id VARCHAR(64) NULL,
    intent VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    draft_type VARCHAR(64) NULL,
    draft_json JSON NULL,
    draft_expires_at DATETIME(3) NULL,
    confirmation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    model_name VARCHAR(128) NULL,
    prompt_version_id BIGINT UNSIGNED NULL,
    input_tokens INT UNSIGNED NULL,
    output_tokens INT UNSIGNED NULL,
    first_token_ms INT UNSIGNED NULL,
    total_duration_ms INT UNSIGNED NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_agent_run_user_time (user_id, created_at),
    KEY idx_agent_run_conversation (conversation_id, created_at),
    KEY idx_agent_run_status (status),
    CONSTRAINT fk_agent_run_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation (id),
    CONSTRAINT fk_agent_run_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_agent_run_request_message FOREIGN KEY (request_message_id) REFERENCES agent_message (id),
    CONSTRAINT fk_agent_run_prompt FOREIGN KEY (prompt_version_id) REFERENCES prompt_version (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 运行记录';

-- Agent run references are stored as application-level references because the Agent tables
-- and business tables are created in separate dependency groups. The corresponding
-- *_run_id fields are indexed and validated by the confirmation service.

CREATE TABLE IF NOT EXISTS agent_step (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    run_id VARCHAR(64) NOT NULL,
    step_no INT UNSIGNED NOT NULL,
    node VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_json JSON NULL,
    output_json JSON NULL,
    error_message VARCHAR(1000) NULL,
    duration_ms INT UNSIGNED NULL,
    started_at DATETIME(3) NULL,
    ended_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_step_run_no (run_id, step_no),
    CONSTRAINT fk_agent_step_run FOREIGN KEY (run_id) REFERENCES agent_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 工作流步骤';

CREATE TABLE IF NOT EXISTS agent_tool_call (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    run_id VARCHAR(64) NOT NULL,
    tool_call_id VARCHAR(128) NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    request_json JSON NULL,
    response_json JSON NULL,
    status VARCHAR(32) NOT NULL,
    duration_ms INT UNSIGNED NULL,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_tool_call_id (tool_call_id),
    KEY idx_agent_tool_call_run (run_id, created_at),
    KEY idx_agent_tool_call_name (tool_name, status),
    CONSTRAINT fk_agent_tool_call_run FOREIGN KEY (run_id) REFERENCES agent_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 工具调用审计';

CREATE TABLE IF NOT EXISTS agent_feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    tags_json JSON NULL,
    comment VARCHAR(2000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_feedback_user_message (user_id, message_id),
    CONSTRAINT fk_agent_feedback_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_agent_feedback_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation (id),
    CONSTRAINT fk_agent_feedback_message FOREIGN KEY (message_id) REFERENCES agent_message (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 用户反馈';

CREATE TABLE IF NOT EXISTS agent_memory (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    memory_type VARCHAR(64) NOT NULL,
    memory_key VARCHAR(128) NOT NULL,
    value_text VARCHAR(2000) NULL,
    value_json JSON NULL,
    source VARCHAR(64) NOT NULL,
    confidence DECIMAL(4,3) NOT NULL DEFAULT 1.000,
    confirmed_at DATETIME(3) NULL,
    expires_at DATETIME(3) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_memory_user_key (user_id, memory_type, memory_key),
    KEY idx_agent_memory_user_status (user_id, status),
    CONSTRAINT fk_agent_memory_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 长期偏好记忆';

CREATE TABLE IF NOT EXISTS chat_memory_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id VARCHAR(64) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content LONGTEXT NOT NULL,
    metadata_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_chat_memory_conversation_time (conversation_id, created_at),
    CONSTRAINT fk_chat_memory_conversation FOREIGN KEY (conversation_id) REFERENCES agent_conversation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Spring AI 短期会话记忆';

CREATE TABLE IF NOT EXISTS idempotency_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    response_status SMALLINT UNSIGNED NULL,
    response_body_json JSON NULL,
    expires_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_user_key_path (user_id, idempotency_key, request_path),
    KEY idx_idempotency_expires (expires_at),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='写操作幂等记录';

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    actor_type VARCHAR(32) NOT NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NULL,
    resource_id VARCHAR(64) NULL,
    request_id VARCHAR(128) NULL,
    run_id VARCHAR(64) NULL,
    detail_json JSON NULL,
    client_ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_audit_log_user_time (user_id, created_at),
    KEY idx_audit_log_resource (resource_type, resource_id),
    KEY idx_audit_log_run (run_id),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_audit_log_run FOREIGN KEY (run_id) REFERENCES agent_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统审计日志';

-- Recommended seed data. Passwords must be replaced by application-generated BCrypt hashes.
INSERT INTO sys_role (code, name, description)
SELECT 'MEMBER', '会员', '普通会员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'MEMBER');

INSERT INTO sys_role (code, name, description)
SELECT 'TRAINER', '教练', '授权会员的训练管理人员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'TRAINER');

INSERT INTO sys_role (code, name, description)
SELECT 'ADMIN', '管理员', '系统管理员'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'ADMIN');

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
