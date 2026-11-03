-- SmartGym Copilot - V6: built-in muscle group dictionary.
-- The catalog is global and independent of gyms or user memberships.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'CHEST', '胸部', NULL, 10, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'CHEST');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'BACK', '背部', NULL, 20, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'BACK');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'SHOULDERS', '肩部', NULL, 30, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'SHOULDERS');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'QUADRICEPS', '股四头肌', NULL, 40, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'QUADRICEPS');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'HAMSTRINGS', '腘绳肌', NULL, 50, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'HAMSTRINGS');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'GLUTES', '臀部', NULL, 60, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'GLUTES');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'CALVES', '小腿', NULL, 70, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'CALVES');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'BICEPS', '肱二头肌', NULL, 80, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'BICEPS');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'TRICEPS', '肱三头肌', NULL, 90, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'TRICEPS');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'FOREARMS', '前臂', NULL, 100, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'FOREARMS');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'CORE', '核心', NULL, 110, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'CORE');
INSERT INTO muscle_group (code, name, parent_id, sort_order, status)
SELECT 'FULL_BODY', '全身', NULL, 120, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM muscle_group WHERE code = 'FULL_BODY');
