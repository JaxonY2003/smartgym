-- SmartGym Copilot - V12: development seed data for the nutrition module.
-- The catalog data is global. User-scoped demo data is attached to the first ACTIVE user.
-- If no user exists yet, the user-scoped INSERT ... SELECT statements insert nothing.
-- This migration is idempotent and does not create a demo account.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Food categories.
INSERT INTO food_category (code, name, sort_order, status)
SELECT 'PROTEIN', '蛋白质类', 10, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM food_category WHERE code = 'PROTEIN');
INSERT INTO food_category (code, name, sort_order, status)
SELECT 'GRAIN', '谷物主食', 20, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM food_category WHERE code = 'GRAIN');
INSERT INTO food_category (code, name, sort_order, status)
SELECT 'VEGETABLE', '蔬菜类', 30, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM food_category WHERE code = 'VEGETABLE');
INSERT INTO food_category (code, name, sort_order, status)
SELECT 'FRUIT', '水果类', 40, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM food_category WHERE code = 'FRUIT');
INSERT INTO food_category (code, name, sort_order, status)
SELECT 'DAIRY', '乳制品', 50, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM food_category WHERE code = 'DAIRY');

-- Global food catalog.
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '鸡蛋', 'Egg', '全蛋，可按可食用部分估算。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'PROTEIN'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '鸡蛋' AND source = 'SMARTGYM_SEED');
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '鸡胸肉', 'Chicken Breast', '去皮熟鸡胸肉。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'PROTEIN'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '鸡胸肉' AND source = 'SMARTGYM_SEED');
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '燕麦片', 'Oats', '未烹煮燕麦片。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'GRAIN'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '燕麦片' AND source = 'SMARTGYM_SEED');
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '糙米饭', 'Brown Rice', '熟糙米饭。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'GRAIN'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '糙米饭' AND source = 'SMARTGYM_SEED');
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '西兰花', 'Broccoli', '熟西兰花。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'VEGETABLE'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '西兰花' AND source = 'SMARTGYM_SEED');
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '香蕉', 'Banana', '去皮香蕉。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'FRUIT'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '香蕉' AND source = 'SMARTGYM_SEED');
INSERT INTO food (category_id, name, english_name, description, status, source, source_version)
SELECT c.id, '低脂牛奶', 'Low-fat Milk', '低脂纯牛奶，按毫升估算。', 'ACTIVE', 'SMARTGYM_SEED', '2026-01'
FROM food_category c
WHERE c.code = 'DAIRY'
  AND NOT EXISTS (SELECT 1 FROM food WHERE name = '低脂牛奶' AND source = 'SMARTGYM_SEED');

-- Nutrition values. Values are per 100 g, except milk which is per 100 ml.
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'g', 143, 12.60, 1.10, 9.50, 0.00, 142, 1.10, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '鸡蛋' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'g');
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'g', 165, 31.00, 0.00, 3.60, 0.00, 74, 0.00, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '鸡胸肉' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'g');
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'g', 389, 16.90, 66.30, 6.90, 10.60, 2, 0.90, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '燕麦片' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'g');
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'g', 123, 2.70, 25.60, 1.00, 1.60, 1, 0.20, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '糙米饭' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'g');
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'g', 34, 2.80, 6.60, 0.40, 2.60, 33, 1.70, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '西兰花' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'g');
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'g', 89, 1.10, 22.80, 0.30, 2.60, 1, 12.20, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '香蕉' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'g');
INSERT INTO food_nutrition (
    food_id, serving_amount, serving_unit, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, sodium_mg, sugar_g, source, source_version
)
SELECT f.id, 100, 'ml', 61, 3.20, 4.80, 3.30, 0.00, 43, 4.80, 'SMARTGYM_SEED', '2026-01'
FROM food f
WHERE f.name = '低脂牛奶' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_nutrition n WHERE n.food_id = f.id AND n.source_version = '2026-01' AND n.serving_unit = 'ml');

-- Common serving sizes.
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '1个', 50, 'g', 50, 1
FROM food f
WHERE f.name = '鸡蛋' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '1个');
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '1份', 150, 'g', 150, 1
FROM food f
WHERE f.name = '鸡胸肉' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '1份');
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '半杯', 50, 'g', 50, 1
FROM food f
WHERE f.name = '燕麦片' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '半杯');
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '1碗', 150, 'g', 150, 1
FROM food f
WHERE f.name = '糙米饭' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '1碗');
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '半盘', 200, 'g', 200, 1
FROM food f
WHERE f.name = '西兰花' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '半盘');
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '1根', 120, 'g', 120, 1
FROM food f
WHERE f.name = '香蕉' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '1根');
INSERT INTO food_serving (food_id, name, amount, unit, gram_weight, is_default)
SELECT f.id, '1杯', 250, 'ml', 258, 1
FROM food f
WHERE f.name = '低脂牛奶' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_serving s WHERE s.food_id = f.id AND s.name = '1杯');

-- Food allergen relations.
INSERT INTO food_allergen (food_id, allergen_id, source)
SELECT f.id, a.id, 'SMARTGYM_SEED'
FROM food f JOIN allergen a ON a.code = 'EGG'
WHERE f.name = '鸡蛋' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_allergen x WHERE x.food_id = f.id AND x.allergen_id = a.id);
INSERT INTO food_allergen (food_id, allergen_id, source)
SELECT f.id, a.id, 'SMARTGYM_SEED'
FROM food f JOIN allergen a ON a.code = 'MILK'
WHERE f.name = '低脂牛奶' AND f.source = 'SMARTGYM_SEED'
  AND NOT EXISTS (SELECT 1 FROM food_allergen x WHERE x.food_id = f.id AND x.allergen_id = a.id);

-- One global recipe for future recipe-based plan items.
INSERT INTO recipe (name, description, serving_count, status)
SELECT '鸡胸肉西兰花糙米饭', '鸡胸肉、西兰花和糙米饭组合餐。', 1, 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM recipe WHERE name = '鸡胸肉西兰花糙米饭');
INSERT INTO recipe_ingredient (recipe_id, food_id, amount, unit, sort_order)
SELECT r.id, f.id, 150, 'g', 1
FROM recipe r JOIN food f ON f.name = '鸡胸肉' AND f.source = 'SMARTGYM_SEED'
WHERE r.name = '鸡胸肉西兰花糙米饭'
  AND NOT EXISTS (SELECT 1 FROM recipe_ingredient x WHERE x.recipe_id = r.id AND x.food_id = f.id);
INSERT INTO recipe_ingredient (recipe_id, food_id, amount, unit, sort_order)
SELECT r.id, f.id, 200, 'g', 2
FROM recipe r JOIN food f ON f.name = '西兰花' AND f.source = 'SMARTGYM_SEED'
WHERE r.name = '鸡胸肉西兰花糙米饭'
  AND NOT EXISTS (SELECT 1 FROM recipe_ingredient x WHERE x.recipe_id = r.id AND x.food_id = f.id);
INSERT INTO recipe_ingredient (recipe_id, food_id, amount, unit, sort_order)
SELECT r.id, f.id, 150, 'g', 3
FROM recipe r JOIN food f ON f.name = '糙米饭' AND f.source = 'SMARTGYM_SEED'
WHERE r.name = '鸡胸肉西兰花糙米饭'
  AND NOT EXISTS (SELECT 1 FROM recipe_ingredient x WHERE x.recipe_id = r.id AND x.food_id = f.id);

-- User-scoped demo rows. No account is created by this migration.
SET @demo_user_id = (
    SELECT id FROM sys_user
    WHERE status = 'ACTIVE' AND deleted_at IS NULL
    ORDER BY id
    LIMIT 1
);

INSERT INTO nutrition_target (
    user_id, effective_date, goal, calories, protein_g,
    carbohydrate_g, fat_g, fiber_g, source
)
SELECT @demo_user_id, '2026-11-03', 'FAT_LOSS', 2100, 150, 220, 65, 30, 'SMARTGYM_SEED'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM nutrition_target
      WHERE user_id = @demo_user_id AND effective_date = '2026-11-03'
  );

INSERT INTO meal_record (
    user_id, eaten_at, meal_type, input_source,
    total_calories, total_protein_g, total_carbohydrate_g, total_fat_g, total_fiber_g,
    note, confirmed_by_user
)
SELECT @demo_user_id, '2026-11-03 00:30:00', 'BREAKFAST', 'MANUAL',
       418.50, 22.75, 45.70, 16.45, 5.30, '演示早餐记录', 1
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM meal_record
      WHERE user_id = @demo_user_id AND eaten_at = '2026-11-03 00:30:00' AND meal_type = 'BREAKFAST'
  );
SET @demo_breakfast_record_id = (
    SELECT id FROM meal_record
    WHERE user_id = @demo_user_id AND eaten_at = '2026-11-03 00:30:00' AND meal_type = 'BREAKFAST'
    LIMIT 1
);
INSERT INTO meal_record_item (
    meal_record_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, input_source, confidence, confirmed_by_user
)
SELECT @demo_breakfast_record_id, f.id, f.name, 50, 'g', 71.50, 6.30, 0.55, 4.75, 0.00, 'MANUAL', 1.000, 1
FROM food f
WHERE f.name = '鸡蛋' AND f.source = 'SMARTGYM_SEED'
  AND @demo_breakfast_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_record_item x WHERE x.meal_record_id = @demo_breakfast_record_id AND x.food_id = f.id);
INSERT INTO meal_record_item (
    meal_record_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, input_source, confidence, confirmed_by_user
)
SELECT @demo_breakfast_record_id, f.id, f.name, 50, 'g', 194.50, 8.45, 33.15, 3.45, 5.30, 'MANUAL', 1.000, 1
FROM food f
WHERE f.name = '燕麦片' AND f.source = 'SMARTGYM_SEED'
  AND @demo_breakfast_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_record_item x WHERE x.meal_record_id = @demo_breakfast_record_id AND x.food_id = f.id);
INSERT INTO meal_record_item (
    meal_record_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, input_source, confidence, confirmed_by_user
)
SELECT @demo_breakfast_record_id, f.id, f.name, 250, 'ml', 152.50, 8.00, 12.00, 8.25, 0.00, 'MANUAL', 1.000, 1
FROM food f
WHERE f.name = '低脂牛奶' AND f.source = 'SMARTGYM_SEED'
  AND @demo_breakfast_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_record_item x WHERE x.meal_record_id = @demo_breakfast_record_id AND x.food_id = f.id);

INSERT INTO meal_record (
    user_id, eaten_at, meal_type, input_source,
    total_calories, total_protein_g, total_carbohydrate_g, total_fat_g, total_fiber_g,
    note, confirmed_by_user
)
SELECT @demo_user_id, '2026-11-03 05:00:00', 'LUNCH', 'MANUAL',
       466.00, 53.35, 45.00, 7.30, 5.00, '演示午餐记录', 1
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM meal_record
      WHERE user_id = @demo_user_id AND eaten_at = '2026-11-03 05:00:00' AND meal_type = 'LUNCH'
  );
SET @demo_lunch_record_id = (
    SELECT id FROM meal_record
    WHERE user_id = @demo_user_id AND eaten_at = '2026-11-03 05:00:00' AND meal_type = 'LUNCH'
    LIMIT 1
);
INSERT INTO meal_record_item (
    meal_record_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, input_source, confidence, confirmed_by_user
)
SELECT @demo_lunch_record_id, f.id, f.name, 150, 'g', 247.50, 46.50, 0.00, 5.40, 0.00, 'MANUAL', 1.000, 1
FROM food f
WHERE f.name = '鸡胸肉' AND f.source = 'SMARTGYM_SEED'
  AND @demo_lunch_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_record_item x WHERE x.meal_record_id = @demo_lunch_record_id AND x.food_id = f.id);
INSERT INTO meal_record_item (
    meal_record_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, input_source, confidence, confirmed_by_user
)
SELECT @demo_lunch_record_id, f.id, f.name, 150, 'g', 184.50, 4.05, 38.40, 1.50, 2.40, 'MANUAL', 1.000, 1
FROM food f
WHERE f.name = '糙米饭' AND f.source = 'SMARTGYM_SEED'
  AND @demo_lunch_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_record_item x WHERE x.meal_record_id = @demo_lunch_record_id AND x.food_id = f.id);
INSERT INTO meal_record_item (
    meal_record_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, input_source, confidence, confirmed_by_user
)
SELECT @demo_lunch_record_id, f.id, f.name, 100, 'g', 34.00, 2.80, 6.60, 0.40, 2.60, 'MANUAL', 1.000, 1
FROM food f
WHERE f.name = '西兰花' AND f.source = 'SMARTGYM_SEED'
  AND @demo_lunch_record_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_record_item x WHERE x.meal_record_id = @demo_lunch_record_id AND x.food_id = f.id);

INSERT INTO daily_nutrition_summary (
    user_id, summary_date, target_calories, target_protein_g, target_carbohydrate_g,
    target_fat_g, target_fiber_g, consumed_calories, consumed_protein_g,
    consumed_carbohydrate_g, consumed_fat_g, consumed_fiber_g,
    meal_count, record_completeness, calculated_at
)
SELECT @demo_user_id, '2026-11-03', 2100, 150, 220, 65, 30,
       884.50, 76.10, 90.70, 23.75, 10.30, 2, 0.6667, CURRENT_TIMESTAMP(3)
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM daily_nutrition_summary
      WHERE user_id = @demo_user_id AND summary_date = '2026-11-03'
  );

INSERT INTO water_record (user_id, recorded_at, amount_ml, source)
SELECT @demo_user_id, '2026-11-03 02:00:00', 300, 'MANUAL'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM water_record
      WHERE user_id = @demo_user_id AND recorded_at = '2026-11-03 02:00:00' AND amount_ml = 300
  );
INSERT INTO water_record (user_id, recorded_at, amount_ml, source)
SELECT @demo_user_id, '2026-11-03 06:00:00', 500, 'MANUAL'
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM water_record
      WHERE user_id = @demo_user_id AND recorded_at = '2026-11-03 06:00:00' AND amount_ml = 500
  );

-- One single-day meal plan for the first active user.
INSERT INTO meal_plan (
    user_id, name, source, goal, status, starts_on, ends_on,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, warnings_json, assumptions_json, version
)
SELECT @demo_user_id, '减脂训练日饮食计划', 'SMARTGYM_SEED', 'FAT_LOSS', 'ACTIVE',
       '2026-11-03', '2026-11-03', 1306.80, 129.52, 131.26, 30.31, 18.62,
       JSON_ARRAY(), JSON_ARRAY('食物重量按可食用部分估算', '营养值为演示数据'), 1
WHERE @demo_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM meal_plan
      WHERE user_id = @demo_user_id AND name = '减脂训练日饮食计划' AND starts_on = '2026-11-03'
  );
SET @demo_plan_id = (
    SELECT id FROM meal_plan
    WHERE user_id = @demo_user_id AND name = '减脂训练日饮食计划' AND starts_on = '2026-11-03'
    LIMIT 1
);
INSERT INTO meal_plan_day (meal_plan_id, plan_date, day_index)
SELECT @demo_plan_id, '2026-11-03', 1
WHERE @demo_plan_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_day WHERE meal_plan_id = @demo_plan_id AND plan_date = '2026-11-03');
SET @demo_plan_day_id = (
    SELECT id FROM meal_plan_day
    WHERE meal_plan_id = @demo_plan_id AND plan_date = '2026-11-03'
    LIMIT 1
);

INSERT INTO meal_plan_meal (meal_plan_day_id, meal_type, meal_time, sort_order, notes)
SELECT @demo_plan_day_id, 'BREAKFAST', '07:30:00', 0, '训练日前早餐，提供碳水和蛋白质。'
WHERE @demo_plan_day_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_meal WHERE meal_plan_day_id = @demo_plan_day_id AND meal_type = 'BREAKFAST');
INSERT INTO meal_plan_meal (meal_plan_day_id, meal_type, meal_time, sort_order, notes)
SELECT @demo_plan_day_id, 'LUNCH', '12:00:00', 1, '午餐以瘦肉和主食为主。'
WHERE @demo_plan_day_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_meal WHERE meal_plan_day_id = @demo_plan_day_id AND meal_type = 'LUNCH');
INSERT INTO meal_plan_meal (meal_plan_day_id, meal_type, meal_time, sort_order, notes)
SELECT @demo_plan_day_id, 'DINNER', '18:30:00', 2, '晚餐增加蔬菜和适量蛋白质。'
WHERE @demo_plan_day_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_meal WHERE meal_plan_day_id = @demo_plan_day_id AND meal_type = 'DINNER');

SET @demo_breakfast_meal_id = (
    SELECT id FROM meal_plan_meal WHERE meal_plan_day_id = @demo_plan_day_id AND meal_type = 'BREAKFAST' LIMIT 1
);
SET @demo_lunch_meal_id = (
    SELECT id FROM meal_plan_meal WHERE meal_plan_day_id = @demo_plan_day_id AND meal_type = 'LUNCH' LIMIT 1
);
SET @demo_dinner_meal_id = (
    SELECT id FROM meal_plan_meal WHERE meal_plan_day_id = @demo_plan_day_id AND meal_type = 'DINNER' LIMIT 1
);

INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_breakfast_meal_id, f.id, f.name, 50, 'g', 71.50, 6.30, 0.55, 4.75, 0.00, '水煮或蒸蛋', 0
FROM food f
WHERE f.name = '鸡蛋' AND f.source = 'SMARTGYM_SEED'
  AND @demo_breakfast_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_breakfast_meal_id AND x.food_id = f.id);
INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_breakfast_meal_id, f.id, f.name, 50, 'g', 194.50, 8.45, 33.15, 3.45, 5.30, '搭配牛奶或酸奶食用', 1
FROM food f
WHERE f.name = '燕麦片' AND f.source = 'SMARTGYM_SEED'
  AND @demo_breakfast_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_breakfast_meal_id AND x.food_id = f.id);
INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_breakfast_meal_id, f.id, f.name, 250, 'ml', 152.50, 8.00, 12.00, 8.25, 0.00, '低脂纯牛奶', 2
FROM food f
WHERE f.name = '低脂牛奶' AND f.source = 'SMARTGYM_SEED'
  AND @demo_breakfast_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_breakfast_meal_id AND x.food_id = f.id);

INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_lunch_meal_id, f.id, f.name, 150, 'g', 247.50, 46.50, 0.00, 5.40, 0.00, '少油烹饪', 0
FROM food f
WHERE f.name = '鸡胸肉' AND f.source = 'SMARTGYM_SEED'
  AND @demo_lunch_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_lunch_meal_id AND x.food_id = f.id);
INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_lunch_meal_id, f.id, f.name, 150, 'g', 184.50, 4.05, 38.40, 1.50, 2.40, '熟糙米饭', 1
FROM food f
WHERE f.name = '糙米饭' AND f.source = 'SMARTGYM_SEED'
  AND @demo_lunch_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_lunch_meal_id AND x.food_id = f.id);
INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_lunch_meal_id, f.id, f.name, 100, 'g', 34.00, 2.80, 6.60, 0.40, 2.60, '清蒸或焯水', 2
FROM food f
WHERE f.name = '西兰花' AND f.source = 'SMARTGYM_SEED'
  AND @demo_lunch_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_lunch_meal_id AND x.food_id = f.id);

INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_dinner_meal_id, f.id, f.name, 120, 'g', 106.80, 1.32, 27.36, 0.36, 3.12, '训练后补充水果', 0
FROM food f
WHERE f.name = '香蕉' AND f.source = 'SMARTGYM_SEED'
  AND @demo_dinner_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_dinner_meal_id AND x.food_id = f.id);
INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_dinner_meal_id, f.id, f.name, 150, 'g', 247.50, 46.50, 0.00, 5.40, 0.00, '少油烹饪', 1
FROM food f
WHERE f.name = '鸡胸肉' AND f.source = 'SMARTGYM_SEED'
  AND @demo_dinner_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_dinner_meal_id AND x.food_id = f.id);
INSERT INTO meal_plan_item (
    meal_plan_meal_id, food_id, food_name_snapshot, amount, unit,
    estimated_calories, estimated_protein_g, estimated_carbohydrate_g,
    estimated_fat_g, estimated_fiber_g, note, sort_order
)
SELECT @demo_dinner_meal_id, f.id, f.name, 200, 'g', 68.00, 5.60, 13.20, 0.80, 5.20, '增加蔬菜体积', 2
FROM food f
WHERE f.name = '西兰花' AND f.source = 'SMARTGYM_SEED'
  AND @demo_dinner_meal_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM meal_plan_item x WHERE x.meal_plan_meal_id = @demo_dinner_meal_id AND x.food_id = f.id);
