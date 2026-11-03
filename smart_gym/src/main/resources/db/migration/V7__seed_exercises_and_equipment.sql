-- SmartGym Copilot - V7: built-in equipment and exercise catalog.
-- The catalog is global and independent of gyms or user memberships.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Global equipment catalog.
INSERT INTO equipment (name, category, brand, status, description)
SELECT '徒手', 'BODYWEIGHT', NULL, 'ACTIVE', '不依赖外部器械的自重训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '徒手');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '哑铃', 'FREE_WEIGHT', NULL, 'ACTIVE', '适合推、拉、蹲和孤立训练的自由重量器械'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '哑铃');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '杠铃', 'FREE_WEIGHT', NULL, 'ACTIVE', '适合深蹲、卧推和硬拉等复合训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '杠铃');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '壶铃', 'FREE_WEIGHT', NULL, 'ACTIVE', '适合摆动、深蹲和全身力量训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '壶铃');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '弹力带', 'ACCESSORY', NULL, 'ACTIVE', '适合激活、辅助和小负荷抗阻训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '弹力带');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '引体向上杆', 'ACCESSORY', NULL, 'ACTIVE', '用于引体向上和悬垂训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '引体向上杆');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '拉力器', 'MACHINE', NULL, 'ACTIVE', '提供连续阻力，适合推、拉和孤立训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '拉力器');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '跑步机', 'CARDIO', NULL, 'ACTIVE', '用于步行、慢跑和跑步训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '跑步机');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '瑜伽垫', 'ACCESSORY', NULL, 'ACTIVE', '用于地面训练、拉伸和核心训练'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '瑜伽垫');
INSERT INTO equipment (name, category, brand, status, description)
SELECT '泡沫轴', 'ACCESSORY', NULL, 'ACTIVE', '用于放松和训练后的恢复'
WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name = '泡沫轴');

-- Global exercise catalog.
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '俯卧撑', 'Push-up', '使用自身体重训练胸部、肩部和手臂。', 'BEGINNER', 60,
       '双手略宽于肩，身体保持一条直线，屈肘下降后推起。', JSON_ARRAY('急性肩部疼痛', '腕关节无法承重'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '俯卧撑');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '哑铃卧推', 'Dumbbell Bench Press', '使用哑铃训练胸部，并兼顾三角肌和肱三头肌。', 'BEGINNER', 90,
       '肩胛骨收紧，哑铃从胸侧推至手臂接近伸直，再控制下放。', JSON_ARRAY('肩部疼痛', '胸部或肘部急性损伤'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '哑铃卧推');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '杠铃卧推', 'Barbell Bench Press', '经典水平推举动作，主要训练胸部。', 'INTERMEDIATE', 120,
       '双脚稳定，杠铃垂直下放至胸部中下段，再沿稳定轨迹推起。', JSON_ARRAY('肩部疼痛', '胸部或肘部急性损伤'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '杠铃卧推');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '高脚杯深蹲', 'Goblet Squat', '适合入门者的负重深蹲动作。', 'BEGINNER', 90,
       '双手将哑铃置于胸前，屈髋屈膝下蹲，保持胸口打开后站起。', JSON_ARRAY('膝关节急性疼痛', '腰部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '高脚杯深蹲');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '杠铃深蹲', 'Barbell Back Squat', '训练下肢力量和稳定性的复合动作。', 'INTERMEDIATE', 150,
       '杠铃稳定放置于背部，保持脊柱中立，屈髋屈膝下蹲后站起。', JSON_ARRAY('膝关节急性疼痛', '腰部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '杠铃深蹲');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '罗马尼亚硬拉', 'Romanian Deadlift', '强调髋关节折叠和后侧链发力的硬拉变式。', 'INTERMEDIATE', 120,
       '保持膝盖微屈，髋部向后推，杠铃贴近腿部下放，再用臀腿力量站起。', JSON_ARRAY('腰部急性疼痛', '腘绳肌急性拉伤'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '罗马尼亚硬拉');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '臀桥', 'Glute Bridge', '在地面完成的臀部和髋伸展训练。', 'BEGINNER', 60,
       '仰卧屈膝，脚掌踩地，收紧臀部抬起髋部，顶端短暂停留后下放。', JSON_ARRAY('腰部急性疼痛', '髋部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '臀桥');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '引体向上', 'Pull-up', '利用悬垂和拉力训练背部及手臂。', 'INTERMEDIATE', 120,
       '从悬垂开始，收紧肩胛并拉动身体，使下巴超过横杆后控制下降。', JSON_ARRAY('肩部疼痛', '肘部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '引体向上');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '杠铃俯身划船', 'Barbell Bent-over Row', '训练背部水平拉力的复合动作。', 'INTERMEDIATE', 120,
       '保持髋部折叠和背部中立，将杠铃拉向下腹部，再控制下放。', JSON_ARRAY('腰部急性疼痛', '肩部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '杠铃俯身划船');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '哑铃肩推', 'Dumbbell Shoulder Press', '训练肩部推举能力的动作。', 'BEGINNER', 90,
       '哑铃从肩部两侧推至头顶上方，再控制回到肩部位置。', JSON_ARRAY('肩部疼痛', '颈部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '哑铃肩推');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '哑铃侧平举', 'Dumbbell Lateral Raise', '针对三角肌中束的孤立训练。', 'BEGINNER', 60,
       '手肘保持微屈，将哑铃向身体两侧抬至接近肩高后缓慢下放。', JSON_ARRAY('肩部疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '哑铃侧平举');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '哑铃弯举', 'Dumbbell Curl', '训练肱二头肌和前臂的屈肘动作。', 'BEGINNER', 60,
       '保持上臂稳定，用肘关节屈曲将哑铃举起，再缓慢还原。', JSON_ARRAY('肘部疼痛', '腕关节急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '哑铃弯举');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '绳索下压', 'Cable Triceps Pushdown', '利用拉力器训练肱三头肌。', 'BEGINNER', 60,
       '上臂贴近身体，向下伸展肘关节，顶端收紧肱三头肌后回放。', JSON_ARRAY('肘部疼痛', '肩部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '绳索下压');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '平板支撑', 'Plank', '训练核心抗伸展能力的静态动作。', 'BEGINNER', 45,
       '前臂或双手支撑地面，收紧腹部和臀部，保持身体从头到脚成一直线。', JSON_ARRAY('腰部疼痛', '肩部无法承重'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '平板支撑');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '卷腹', 'Crunch', '通过脊柱屈曲训练腹部肌群。', 'BEGINNER', 45,
       '仰卧屈膝，收紧腹部抬起肩胛骨，避免用力拉扯颈部。', JSON_ARRAY('颈部疼痛', '腰部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '卷腹');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '壶铃摆动', 'Kettlebell Swing', '通过髋部爆发伸展训练后侧链和核心。', 'INTERMEDIATE', 90,
       '以髋部折叠蓄力，利用髋部伸展将壶铃摆至胸前高度，保持手臂放松。', JSON_ARRAY('腰部急性疼痛', '肩部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '壶铃摆动');
INSERT INTO exercise (
    name, english_name, description, difficulty, default_rest_seconds,
    instructions, contraindications_json, thumbnail_url, status
)
SELECT '弹力带肩部外旋', 'Band External Rotation', '用于肩袖激活和肩部稳定性的低负荷训练。', 'BEGINNER', 45,
       '肘部贴近身体，缓慢向外旋转前臂，保持肩胛稳定后回到起始位置。', JSON_ARRAY('肩部急性疼痛'), NULL, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = '弹力带肩部外旋');

-- Exercise-to-muscle relationships.
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CHEST'
WHERE e.name = '俯卧撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'TRICEPS'
WHERE e.name = '俯卧撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'SHOULDERS'
WHERE e.name = '俯卧撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CHEST'
WHERE e.name = '哑铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'TRICEPS'
WHERE e.name = '哑铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'SHOULDERS'
WHERE e.name = '哑铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CHEST'
WHERE e.name = '杠铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'TRICEPS'
WHERE e.name = '杠铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'SHOULDERS'
WHERE e.name = '杠铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'QUADRICEPS'
WHERE e.name = '高脚杯深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'GLUTES'
WHERE e.name = '高脚杯深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'HAMSTRINGS'
WHERE e.name = '高脚杯深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '高脚杯深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'QUADRICEPS'
WHERE e.name = '杠铃深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'GLUTES'
WHERE e.name = '杠铃深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'HAMSTRINGS'
WHERE e.name = '杠铃深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '杠铃深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'HAMSTRINGS'
WHERE e.name = '罗马尼亚硬拉'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'GLUTES'
WHERE e.name = '罗马尼亚硬拉'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'BACK'
WHERE e.name = '罗马尼亚硬拉'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '罗马尼亚硬拉'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'GLUTES'
WHERE e.name = '臀桥'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'HAMSTRINGS'
WHERE e.name = '臀桥'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '臀桥'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'BACK'
WHERE e.name = '引体向上'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'BICEPS'
WHERE e.name = '引体向上'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'FOREARMS'
WHERE e.name = '引体向上'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'BACK'
WHERE e.name = '杠铃俯身划船'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'BICEPS'
WHERE e.name = '杠铃俯身划船'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'FOREARMS'
WHERE e.name = '杠铃俯身划船'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'SHOULDERS'
WHERE e.name = '哑铃肩推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'TRICEPS'
WHERE e.name = '哑铃肩推'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'SHOULDERS'
WHERE e.name = '哑铃侧平举'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'BICEPS'
WHERE e.name = '哑铃弯举'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'FOREARMS'
WHERE e.name = '哑铃弯举'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'TRICEPS'
WHERE e.name = '绳索下压'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '平板支撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '卷腹'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'CORE'
WHERE e.name = '壶铃摆动'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'GLUTES'
WHERE e.name = '壶铃摆动'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'SECONDARY'
FROM exercise e JOIN muscle_group m ON m.code = 'HAMSTRINGS'
WHERE e.name = '壶铃摆动'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);
INSERT INTO exercise_muscle (exercise_id, muscle_group_id, muscle_role)
SELECT e.id, m.id, 'PRIMARY'
FROM exercise e JOIN muscle_group m ON m.code = 'SHOULDERS'
WHERE e.name = '弹力带肩部外旋'
  AND NOT EXISTS (SELECT 1 FROM exercise_muscle x WHERE x.exercise_id = e.id AND x.muscle_group_id = m.id);

-- Exercise-to-equipment relationships.
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '徒手'
WHERE e.name = '俯卧撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '哑铃'
WHERE e.name = '哑铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '杠铃'
WHERE e.name = '杠铃卧推'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '哑铃'
WHERE e.name = '高脚杯深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '杠铃'
WHERE e.name = '杠铃深蹲'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '杠铃'
WHERE e.name = '罗马尼亚硬拉'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '徒手'
WHERE e.name = '臀桥'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '瑜伽垫'
WHERE e.name = '臀桥'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '引体向上杆'
WHERE e.name = '引体向上'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '杠铃'
WHERE e.name = '杠铃俯身划船'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '哑铃'
WHERE e.name = '哑铃肩推'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '哑铃'
WHERE e.name = '哑铃侧平举'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '哑铃'
WHERE e.name = '哑铃弯举'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '拉力器'
WHERE e.name = '绳索下压'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '徒手'
WHERE e.name = '平板支撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '瑜伽垫'
WHERE e.name = '平板支撑'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '徒手'
WHERE e.name = '卷腹'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '瑜伽垫'
WHERE e.name = '卷腹'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '壶铃'
WHERE e.name = '壶铃摆动'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
INSERT INTO exercise_equipment (exercise_id, equipment_id, required)
SELECT e.id, q.id, 1
FROM exercise e JOIN equipment q ON q.name = '弹力带'
WHERE e.name = '弹力带肩部外旋'
  AND NOT EXISTS (SELECT 1 FROM exercise_equipment x WHERE x.exercise_id = e.id AND x.equipment_id = q.id);
