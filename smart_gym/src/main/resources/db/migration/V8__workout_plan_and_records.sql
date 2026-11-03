-- SmartGym Copilot - V8: workout plans and workout records.
-- This migration is intentionally schema-only. It does not insert business data.
-- The product is a general-purpose fitness application; no gym fields are used.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS workout_plan (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
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
    KEY idx_workout_plan_deleted (user_id, deleted_at),
    CONSTRAINT fk_workout_plan_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_workout_plan_creator
        FOREIGN KEY (created_by_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练计划';

CREATE TABLE IF NOT EXISTS workout_plan_day (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_id BIGINT UNSIGNED NOT NULL,
    day_no SMALLINT UNSIGNED NOT NULL,
    day_of_week TINYINT UNSIGNED NULL,
    title VARCHAR(128) NOT NULL,
    estimated_minutes SMALLINT UNSIGNED NULL,
    notes VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_plan_day_no (plan_id, day_no),
    KEY idx_workout_plan_day_plan_order (plan_id, day_no),
    CONSTRAINT fk_workout_plan_day_plan
        FOREIGN KEY (plan_id) REFERENCES workout_plan (id)
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
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_workout_plan_exercise_day_order (plan_day_id, sort_order, id),
    KEY idx_workout_plan_exercise_exercise (exercise_id),
    CONSTRAINT fk_workout_plan_exercise_day
        FOREIGN KEY (plan_day_id) REFERENCES workout_plan_day (id),
    CONSTRAINT fk_workout_plan_exercise_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercise (id)
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
    KEY idx_workout_session_user_status (user_id, status, started_at),
    KEY idx_workout_session_plan (plan_id, plan_day_id),
    CONSTRAINT fk_workout_session_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_workout_session_plan
        FOREIGN KEY (plan_id) REFERENCES workout_plan (id),
    CONSTRAINT fk_workout_session_plan_day
        FOREIGN KEY (plan_day_id) REFERENCES workout_plan_day (id)
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
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_workout_set_record_order (session_id, exercise_id, set_index),
    KEY idx_workout_set_record_session (session_id, set_index),
    KEY idx_workout_set_record_exercise (exercise_id),
    CONSTRAINT fk_workout_set_record_session
        FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_workout_set_record_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercise (id)
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
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_workout_feedback_session (session_id),
    KEY idx_workout_feedback_user_time (user_id, created_at),
    CONSTRAINT fk_workout_feedback_session
        FOREIGN KEY (session_id) REFERENCES workout_session (id),
    CONSTRAINT fk_workout_feedback_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练反馈';
