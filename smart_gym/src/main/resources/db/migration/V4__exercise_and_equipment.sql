-- SmartGym Copilot - V4: exercise, muscle group and gym equipment catalog.
-- All tables use the project-wide id / created_at / updated_at fields.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS muscle_group (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT UNSIGNED NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_muscle_group_code (code),
    KEY idx_muscle_group_parent_status (parent_id, status),
    CONSTRAINT fk_muscle_group_parent FOREIGN KEY (parent_id) REFERENCES muscle_group (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='肌群';

CREATE TABLE IF NOT EXISTS exercise (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    english_name VARCHAR(128) NULL,
    description TEXT NULL,
    difficulty VARCHAR(32) NOT NULL DEFAULT 'BEGINNER',
    default_rest_seconds INT UNSIGNED NOT NULL DEFAULT 90,
    instructions TEXT NULL,
    contraindications_json JSON NULL,
    thumbnail_url VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_exercise_name_status (name, status),
    KEY idx_exercise_difficulty_status (difficulty, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='训练动作';

CREATE TABLE IF NOT EXISTS exercise_muscle (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    exercise_id BIGINT UNSIGNED NOT NULL,
    muscle_group_id BIGINT UNSIGNED NOT NULL,
    muscle_role VARCHAR(32) NOT NULL DEFAULT 'PRIMARY',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_muscle_exercise_group (exercise_id, muscle_group_id),
    KEY idx_exercise_muscle_group (muscle_group_id),
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
    KEY idx_equipment_name_status (name, status),
    KEY idx_equipment_category_status (category, status)
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
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    exercise_id BIGINT UNSIGNED NOT NULL,
    equipment_id BIGINT UNSIGNED NOT NULL,
    required TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_exercise_equipment_exercise_equipment (exercise_id, equipment_id),
    KEY idx_exercise_equipment_equipment (equipment_id),
    CONSTRAINT fk_exercise_equipment_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
    CONSTRAINT fk_exercise_equipment_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动作器械关系';
