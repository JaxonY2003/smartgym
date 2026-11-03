-- SmartGym Copilot - V3: fix repeated profile updates.
-- training_goal keeps inactive history, so status must not participate in a unique key.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

ALTER TABLE training_goal
    DROP INDEX uk_training_goal_user_type_status;
