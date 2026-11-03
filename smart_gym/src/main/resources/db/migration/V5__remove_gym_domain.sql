-- SmartGym Copilot - V5: remove the gym and membership domain.
-- The product is a general-purpose fitness application.
-- V1 and V4 are historical migrations and must remain unchanged.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Remove references before dropping the gym table.
DROP TABLE IF EXISTS gym_equipment;
DROP TABLE IF EXISTS gym_user;
DROP TABLE IF EXISTS gym;
