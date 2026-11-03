-- SmartGym Copilot - V13: development seed data for the dashboard.
-- Uses the first ACTIVE user only; no demo account is created.
-- Global exercise data is provided by V7. This migration only creates user-scoped demo rows.
-- The migration is idempotent and is safe to skip when no active user exists.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @demo_user_id = (
    SELECT id
    FROM sys_user
    WHERE status = 'ACTIVE'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
);

-- Four body measurements for the weight card and weight trend.
INSERT INTO body_metric (user_id, measured_at, weight_kg, body_fat_percent, waist_cm, note)
SELECT @demo_user_id, '2026-10-27 00:00:00', 73.10, 20.40, 84.00, '看板演示体重记录'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM body_metric
      WHERE user_id = @demo_user_id AND measured_at = '2026-10-27 00:00:00'
  );
INSERT INTO body_metric (user_id, measured_at, weight_kg, body_fat_percent, waist_cm, note)
SELECT @demo_user_id, '2026-10-30 00:00:00', 72.90, 20.20, 83.50, '看板演示体重记录'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM body_metric
      WHERE user_id = @demo_user_id AND measured_at = '2026-10-30 00:00:00'
  );
INSERT INTO body_metric (user_id, measured_at, weight_kg, body_fat_percent, waist_cm, note)
SELECT @demo_user_id, '2026-11-01 00:00:00', 72.70, 20.10, 83.20, '看板演示体重记录'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM body_metric
      WHERE user_id = @demo_user_id AND measured_at = '2026-11-01 00:00:00'
  );
INSERT INTO body_metric (user_id, measured_at, weight_kg, body_fat_percent, waist_cm, note)
SELECT @demo_user_id, '2026-11-03 00:00:00', 72.50, 20.00, 83.00, '看板演示体重记录'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM body_metric
      WHERE user_id = @demo_user_id AND measured_at = '2026-11-03 00:00:00'
  );

-- One active three-day plan. Tuesday, Thursday and Saturday are training days.
INSERT INTO workout_plan (
    user_id, name, source, goal, status, starts_on, ends_on,
    duration_weeks, training_days_per_week, warnings_json, assumptions_json, version
)
SELECT @demo_user_id, '减脂力量训练计划', 'SMARTGYM_SEED', 'FAT_LOSS', 'ACTIVE',
       '2026-10-27', '2026-11-23', 4, 3, JSON_ARRAY(),
       JSON_ARRAY('训练重量为演示数据', '按完成组数计算训练容量'), 1
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan
      WHERE user_id = @demo_user_id
        AND name = '减脂力量训练计划'
        AND starts_on = '2026-10-27'
        AND deleted_at IS NULL
  );

SET @demo_plan_id = (
    SELECT id
    FROM workout_plan
    WHERE user_id = @demo_user_id
      AND name = '减脂力量训练计划'
      AND starts_on = '2026-10-27'
      AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
);

INSERT INTO workout_plan_day (
    plan_id, day_no, day_of_week, title, estimated_minutes, notes
)
SELECT @demo_plan_id, 1, 2, '全身力量 A', 45, '深蹲、推举和核心训练。'
WHERE @demo_plan_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_day
      WHERE plan_id = @demo_plan_id AND day_no = 1
  );
INSERT INTO workout_plan_day (
    plan_id, day_no, day_of_week, title, estimated_minutes, notes
)
SELECT @demo_plan_id, 2, 4, '全身力量 B', 50, '髋主导、拉力和肩部训练。'
WHERE @demo_plan_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_day
      WHERE plan_id = @demo_plan_id AND day_no = 2
  );
INSERT INTO workout_plan_day (
    plan_id, day_no, day_of_week, title, estimated_minutes, notes
)
SELECT @demo_plan_id, 3, 6, '代谢训练 C', 40, '下肢、俯卧撑和壶铃训练。'
WHERE @demo_plan_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_day
      WHERE plan_id = @demo_plan_id AND day_no = 3
  );

SET @demo_day_tue_id = (
    SELECT id FROM workout_plan_day
    WHERE plan_id = @demo_plan_id AND day_of_week = 2
    ORDER BY day_no LIMIT 1
);
SET @demo_day_thu_id = (
    SELECT id FROM workout_plan_day
    WHERE plan_id = @demo_plan_id AND day_of_week = 4
    ORDER BY day_no LIMIT 1
);
SET @demo_day_sat_id = (
    SELECT id FROM workout_plan_day
    WHERE plan_id = @demo_plan_id AND day_of_week = 6
    ORDER BY day_no LIMIT 1
);

-- Planned exercises for each training day.
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_tue_id, e.id, 1, 3, 8, 12, 40.00, 120, '保持核心稳定。'
FROM exercise e
WHERE e.name = '杠铃深蹲' AND e.status = 'ACTIVE'
  AND @demo_day_tue_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_tue_id AND x.exercise_id = e.id
  );
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_tue_id, e.id, 2, 3, 8, 12, 12.00, 90, '肩胛骨收紧。'
FROM exercise e
WHERE e.name = '哑铃卧推' AND e.status = 'ACTIVE'
  AND @demo_day_tue_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_tue_id AND x.exercise_id = e.id
  );
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_tue_id, e.id, 3, 3, 30, 45, NULL, 45, '保持呼吸均匀。'
FROM exercise e
WHERE e.name = '平板支撑' AND e.status = 'ACTIVE'
  AND @demo_day_tue_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_tue_id AND x.exercise_id = e.id
  );

INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_thu_id, e.id, 1, 3, 8, 12, 40.00, 120, '髋部向后折叠。'
FROM exercise e
WHERE e.name = '罗马尼亚硬拉' AND e.status = 'ACTIVE'
  AND @demo_day_thu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_thu_id AND x.exercise_id = e.id
  );
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_thu_id, e.id, 2, 3, 8, 12, 25.00, 90, '背部保持平直。'
FROM exercise e
WHERE e.name = '杠铃俯身划船' AND e.status = 'ACTIVE'
  AND @demo_day_thu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_thu_id AND x.exercise_id = e.id
  );
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_thu_id, e.id, 3, 3, 8, 12, 10.00, 90, '避免耸肩。'
FROM exercise e
WHERE e.name = '哑铃肩推' AND e.status = 'ACTIVE'
  AND @demo_day_thu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_thu_id AND x.exercise_id = e.id
  );

INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_sat_id, e.id, 1, 3, 10, 15, 20.00, 90, '膝盖跟随脚尖方向。'
FROM exercise e
WHERE e.name = '高脚杯深蹲' AND e.status = 'ACTIVE'
  AND @demo_day_sat_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_sat_id AND x.exercise_id = e.id
  );
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_sat_id, e.id, 2, 3, 8, 15, NULL, 60, '身体保持稳定。'
FROM exercise e
WHERE e.name = '俯卧撑' AND e.status = 'ACTIVE'
  AND @demo_day_sat_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_sat_id AND x.exercise_id = e.id
  );
INSERT INTO workout_plan_exercise (
    plan_day_id, exercise_id, sort_order, sets, target_reps_min,
    target_reps_max, target_weight_kg, rest_seconds, notes
)
SELECT @demo_day_sat_id, e.id, 3, 3, 12, 20, 16.00, 90, '用髋部带动摆动。'
FROM exercise e
WHERE e.name = '壶铃摆动' AND e.status = 'ACTIVE'
  AND @demo_day_sat_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_plan_exercise x
      WHERE x.plan_day_id = @demo_day_sat_id AND x.exercise_id = e.id
  );

-- Completed sessions used by the overview completion rate and volume trend.
INSERT INTO workout_session (
    user_id, plan_id, plan_day_id, status, started_at, ended_at,
    fatigue_level, total_volume_kg, note
)
SELECT @demo_user_id, @demo_plan_id, @demo_day_tue_id, 'COMPLETED',
       '2026-11-03 00:30:00', '2026-11-03 01:15:00', 6, 1040.00, '看板演示训练记录'
WHERE @demo_user_id IS NOT NULL
  AND @demo_day_tue_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_session
      WHERE user_id = @demo_user_id AND started_at = '2026-11-03 00:30:00'
  );
SET @demo_session_today_id = (
    SELECT id FROM workout_session
    WHERE user_id = @demo_user_id AND started_at = '2026-11-03 00:30:00'
    ORDER BY id LIMIT 1
);

INSERT INTO workout_session (
    user_id, plan_id, plan_day_id, status, started_at, ended_at,
    fatigue_level, total_volume_kg, note
)
SELECT @demo_user_id, @demo_plan_id, @demo_day_sat_id, 'COMPLETED',
       '2026-10-31 00:30:00', '2026-10-31 01:10:00', 7, 1440.00, '看板演示训练记录'
WHERE @demo_user_id IS NOT NULL
  AND @demo_day_sat_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_session
      WHERE user_id = @demo_user_id AND started_at = '2026-10-31 00:30:00'
  );
SET @demo_session_sat_id = (
    SELECT id FROM workout_session
    WHERE user_id = @demo_user_id AND started_at = '2026-10-31 00:30:00'
    ORDER BY id LIMIT 1
);

INSERT INTO workout_session (
    user_id, plan_id, plan_day_id, status, started_at, ended_at,
    fatigue_level, total_volume_kg, note
)
SELECT @demo_user_id, @demo_plan_id, @demo_day_thu_id, 'COMPLETED',
       '2026-10-29 00:30:00', '2026-10-29 01:20:00', 7, 2310.00, '看板演示训练记录'
WHERE @demo_user_id IS NOT NULL
  AND @demo_day_thu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_session
      WHERE user_id = @demo_user_id AND started_at = '2026-10-29 00:30:00'
  );
SET @demo_session_thu_id = (
    SELECT id FROM workout_session
    WHERE user_id = @demo_user_id AND started_at = '2026-10-29 00:30:00'
    ORDER BY id LIMIT 1
);

INSERT INTO workout_session (
    user_id, plan_id, plan_day_id, status, started_at, ended_at,
    fatigue_level, total_volume_kg, note
)
SELECT @demo_user_id, @demo_plan_id, @demo_day_tue_id, 'COMPLETED',
       '2026-10-27 00:30:00', '2026-10-27 01:15:00', 6, 1350.00, '看板演示训练记录'
WHERE @demo_user_id IS NOT NULL
  AND @demo_day_tue_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM workout_session
      WHERE user_id = @demo_user_id AND started_at = '2026-10-27 00:30:00'
  );
SET @demo_session_first_id = (
    SELECT id FROM workout_session
    WHERE user_id = @demo_user_id AND started_at = '2026-10-27 00:30:00'
    ORDER BY id LIMIT 1
);

-- Set details make the seeded total volume auditable in the training module.
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_today_id, e.id, 1, 40.00, 10, 1, 120
FROM exercise e
WHERE e.name = '杠铃深蹲' AND @demo_session_today_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_today_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_today_id, e.id, 2, 40.00, 10, 1, 120
FROM exercise e
WHERE e.name = '杠铃深蹲' AND @demo_session_today_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_today_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_today_id, e.id, 1, 12.00, 10, 1, 90
FROM exercise e
WHERE e.name = '哑铃卧推' AND @demo_session_today_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_today_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_today_id, e.id, 2, 12.00, 10, 1, 90
FROM exercise e
WHERE e.name = '哑铃卧推' AND @demo_session_today_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_today_id AND x.exercise_id = e.id AND x.set_index = 2);

INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_sat_id, e.id, 1, 20.00, 12, 1, 90
FROM exercise e
WHERE e.name = '高脚杯深蹲' AND @demo_session_sat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_sat_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_sat_id, e.id, 2, 20.00, 12, 1, 90
FROM exercise e
WHERE e.name = '高脚杯深蹲' AND @demo_session_sat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_sat_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_sat_id, e.id, 1, 16.00, 15, 1, 90
FROM exercise e
WHERE e.name = '壶铃摆动' AND @demo_session_sat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_sat_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_sat_id, e.id, 2, 16.00, 15, 1, 90
FROM exercise e
WHERE e.name = '壶铃摆动' AND @demo_session_sat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_sat_id AND x.exercise_id = e.id AND x.set_index = 2);

INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 1, 40.00, 10, 1, 120
FROM exercise e
WHERE e.name = '罗马尼亚硬拉' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 1, 25.00, 10, 1, 90
FROM exercise e
WHERE e.name = '杠铃俯身划船' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 1, 10.00, 12, 1, 90
FROM exercise e
WHERE e.name = '哑铃肩推' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 1);

INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_first_id, e.id, 1, 35.00, 10, 1, 120
FROM exercise e
WHERE e.name = '杠铃深蹲' AND @demo_session_first_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_first_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_first_id, e.id, 1, 10.00, 10, 1, 90
FROM exercise e
WHERE e.name = '哑铃卧推' AND @demo_session_first_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_first_id AND x.exercise_id = e.id AND x.set_index = 1);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_sat_id, e.id, 3, 20.00, 12, 1, 90
FROM exercise e
WHERE e.name = '高脚杯深蹲' AND @demo_session_sat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_sat_id AND x.exercise_id = e.id AND x.set_index = 3);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_sat_id, e.id, 3, 16.00, 15, 1, 90
FROM exercise e
WHERE e.name = '壶铃摆动' AND @demo_session_sat_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_sat_id AND x.exercise_id = e.id AND x.set_index = 3);

INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 2, 40.00, 10, 1, 120
FROM exercise e
WHERE e.name = '罗马尼亚硬拉' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 3, 40.00, 10, 1, 120
FROM exercise e
WHERE e.name = '罗马尼亚硬拉' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 3);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 2, 25.00, 10, 1, 90
FROM exercise e
WHERE e.name = '杠铃俯身划船' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 3, 25.00, 10, 1, 90
FROM exercise e
WHERE e.name = '杠铃俯身划船' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 3);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 2, 10.00, 12, 1, 90
FROM exercise e
WHERE e.name = '哑铃肩推' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_thu_id, e.id, 3, 10.00, 12, 1, 90
FROM exercise e
WHERE e.name = '哑铃肩推' AND @demo_session_thu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_thu_id AND x.exercise_id = e.id AND x.set_index = 3);

INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_first_id, e.id, 2, 35.00, 10, 1, 120
FROM exercise e
WHERE e.name = '杠铃深蹲' AND @demo_session_first_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_first_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_first_id, e.id, 3, 35.00, 10, 1, 120
FROM exercise e
WHERE e.name = '杠铃深蹲' AND @demo_session_first_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_first_id AND x.exercise_id = e.id AND x.set_index = 3);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_first_id, e.id, 2, 10.00, 10, 1, 90
FROM exercise e
WHERE e.name = '哑铃卧推' AND @demo_session_first_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_first_id AND x.exercise_id = e.id AND x.set_index = 2);
INSERT INTO workout_set_record (session_id, exercise_id, set_index, weight_kg, reps, completed, rest_seconds)
SELECT @demo_session_first_id, e.id, 3, 10.00, 10, 1, 90
FROM exercise e
WHERE e.name = '哑铃卧推' AND @demo_session_first_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM workout_set_record x WHERE x.session_id = @demo_session_first_id AND x.exercise_id = e.id AND x.set_index = 3);
