/**
 * 后端枚举值 → 前端展示文案映射（接口文档 12.1：枚举值由后端返回，前端展示文案自行维护）
 * 仅维护本页面用到的枚举，后续页面按需在此追加。
 */
import type {
  AllergenCode,
  BodyPart,
  ConstraintSeverity,
  DietType,
  DietaryRestriction,
  FitnessGoal,
  Gender,
  HealthConstraintType,
  TrainingExperience,
} from '@/types/api/member'
import type {
  EquipmentCategory,
  EquipmentStatus,
  ExerciseDifficulty,
  MuscleGroupCode,
} from '@/types/api/exercise'

export const GENDER_LABELS: Record<Gender, string> = {
  MALE: '男',
  FEMALE: '女',
  OTHER: '其他',
  UNKNOWN: '保密',
}

export const GOAL_LABELS: Record<FitnessGoal, string> = {
  MUSCLE_GAIN: '增肌',
  FAT_LOSS: '减脂',
  BODY_SHAPING: '塑形',
  MAINTENANCE: '维持健康',
}

export const EXPERIENCE_LABELS: Record<TrainingExperience, string> = {
  BEGINNER: '初学者（< 1 年）',
  INTERMEDIATE: '进阶（1～3 年）',
  ADVANCED: '高级（3 年以上）',
}

export const CONSTRAINT_TYPE_LABELS: Record<HealthConstraintType, string> = {
  PAIN: '疼痛',
  INJURY: '伤病',
  CHRONIC_DISEASE: '慢性疾病',
  PREGNANCY: '孕期',
  OTHER: '其他',
}

export const SEVERITY_LABELS: Record<ConstraintSeverity, string> = {
  MILD: '轻度',
  MODERATE: '中度',
  SEVERE: '重度',
}

export const BODY_PART_LABELS: Record<BodyPart, string> = {
  KNEE: '膝盖',
  SHOULDER: '肩部',
  LOWER_BACK: '下背部',
  HIP: '髋部',
  ANKLE: '脚踝',
  WRIST: '手腕',
  ELBOW: '手肘',
  NECK: '颈部',
  OTHER: '其他部位',
}

export const DIET_TYPE_LABELS: Record<DietType, string> = {
  OMNIVORE: '正常饮食（荤素不限）',
  VEGETARIAN: '素食',
  VEGAN: '纯素',
  PESCATARIAN: '鱼素',
  KETO: '生酮',
  OTHER: '其他',
}

export const ALLERGEN_LABELS: Record<AllergenCode, string> = {
  PEANUT: '花生',
  TREE_NUT: '坚果',
  MILK: '乳制品',
  EGG: '鸡蛋',
  SOY: '大豆',
  WHEAT: '小麦/麸质',
  FISH: '鱼类',
  SHELLFISH: '甲壳类海鲜',
  SESAME: '芝麻',
}

export const RESTRICTION_LABELS: Record<DietaryRestriction, string> = {
  LOW_SUGAR: '低糖',
  LOW_SODIUM: '低钠',
  LOW_FAT: '低脂',
  HIGH_PROTEIN: '高蛋白',
  HALAL: '清真',
  VEGAN_ONLY: '仅素食',
}

/** el-tag 的类型映射：健康限制用颜色区分严重度 */
export const SEVERITY_TAG_TYPES: Record<ConstraintSeverity, 'success' | 'warning' | 'danger'> = {
  MILD: 'success',
  MODERATE: 'warning',
  SEVERE: 'danger',
}

/* ---------- 动作与器械（接口文档 §4） ---------- */

export const MUSCLE_GROUP_LABELS: Record<MuscleGroupCode, string> = {
  CHEST: '胸部',
  BACK: '背部',
  SHOULDERS: '肩部',
  QUADRICEPS: '股四头肌',
  HAMSTRINGS: '腘绳肌',
  GLUTES: '臀部',
  CALVES: '小腿',
  BICEPS: '肱二头肌',
  TRICEPS: '肱三头肌',
  FOREARMS: '前臂',
  CORE: '核心',
  FULL_BODY: '全身',
}

export const DIFFICULTY_LABELS: Record<ExerciseDifficulty, string> = {
  BEGINNER: '初级',
  INTERMEDIATE: '中级',
  ADVANCED: '高级',
}

export const DIFFICULTY_TAG_TYPES: Record<ExerciseDifficulty, 'success' | 'warning' | 'danger'> = {
  BEGINNER: 'success',
  INTERMEDIATE: 'warning',
  ADVANCED: 'danger',
}

export const EQUIPMENT_CATEGORY_LABELS: Record<EquipmentCategory, string> = {
  FREE_WEIGHT: '自由重量',
  MACHINE: '固定器械',
  CARDIO: '有氧器械',
  BODYWEIGHT: '自重',
  ACCESSORY: '小器械',
  OTHER: '其他',
}

export const EQUIPMENT_STATUS_LABELS: Record<EquipmentStatus, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用',
}

export const EQUIPMENT_STATUS_TAG_TYPES: Record<EquipmentStatus, 'success' | 'danger'> = {
  ACTIVE: 'success',
  INACTIVE: 'danger',
}

/* ---------- 训练计划与记录（接口文档 §5） ---------- */

export const PLAN_SOURCE_LABELS: Record<string, string> = {
  AGENT: 'AI 生成',
  MANUAL: '手动创建',
  TEMPLATE: '模板',
}

export const PLAN_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  ACTIVE: '进行中',
  PAUSED: '已暂停',
  ARCHIVED: '已归档',
}

export const PLAN_STATUS_TAG_TYPES: Record<string, 'info' | 'success' | 'warning' | 'info'> = {
  DRAFT: 'info',
  ACTIVE: 'success',
  PAUSED: 'warning',
  ARCHIVED: 'info',
}

/** 星期映射：dayOfWeek 1～7 → 周一～周日 */
export const WEEKDAY_LABELS: Record<number, string> = {
  1: '周一',
  2: '周二',
  3: '周三',
  4: '周四',
  5: '周五',
  6: '周六',
  7: '周日',
}

export const SESSION_STATUS_LABELS: Record<string, string> = {
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  ABANDONED: '已中止',
}

/* ---------- 饮食（接口文档 §6） ---------- */

export const MEAL_TYPE_LABELS: Record<string, string> = {
  BREAKFAST: '早餐',
  LUNCH: '午餐',
  DINNER: '晚餐',
  SNACK: '加餐',
}

export const FOOD_CATEGORY_LABELS: Record<string, string> = {
  PROTEIN: '蛋白质',
  GRAIN: '主食谷物',
  VEGETABLE: '蔬菜',
  FRUIT: '水果',
  DAIRY: '乳制品',
  NUT: '坚果',
  BEVERAGE: '饮品',
  OTHER: '其他',
}

export const NUTRITION_SOURCE_LABELS: Record<string, string> = {
  USER_CONFIRMED: '用户确认',
  AGENT_SUGGESTED: 'AI 建议',
  SYSTEM_DEFAULT: '系统默认',
}

/** 供 el-select 使用的 options 数组（label/value） */
export function toOptions<K extends string>(labels: Record<K, string>): { label: string; value: K }[] {
  return (Object.entries(labels) as [K, string][]).map(([value, label]) => ({ label, value }))
}
