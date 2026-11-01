/**
 * 会员健康档案相关类型（接口文档 §3）
 */

/** 训练目标（3.2） */
export type FitnessGoal = 'MUSCLE_GAIN' | 'FAT_LOSS' | 'BODY_SHAPING' | 'MAINTENANCE'

/** 性别（3.2） */
export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'UNKNOWN'

/** 训练经验（3.2） */
export type TrainingExperience = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

/** 基础健康档案（3.1 / 3.2） */
export interface MemberProfile {
  userId: number
  gender: Gender
  birthDate: string | null
  heightCm: number | null
  weightKg: number | null
  trainingExperience: TrainingExperience | null
  weeklyTrainingDays: number | null
  maxSessionMinutes: number | null
  goals: FitnessGoal[]
  updatedAt: string
}

export interface MemberProfileInput {
  gender: Gender
  birthDate: string
  heightCm: number
  weightKg: number
  trainingExperience: TrainingExperience
  weeklyTrainingDays: number
  maxSessionMinutes: number
  goals: FitnessGoal[]
}

/** 身体指标记录（3.3） */
export interface BodyMetric {
  id: number
  measuredAt: string
  weightKg: number
  bodyFatPercent: number | null
  waistCm: number | null
  note: string | null
}

export interface BodyMetricInput {
  measuredAt: string
  weightKg: number
  bodyFatPercent?: number | null
  waistCm?: number | null
  note?: string
}

/** 健康限制（3.4） */
export type HealthConstraintType = 'PAIN' | 'INJURY' | 'CHRONIC_DISEASE' | 'PREGNANCY' | 'OTHER'

export type ConstraintSeverity = 'MILD' | 'MODERATE' | 'SEVERE'

/** 身体部位：后端枚举全集由接口下发，前端展示文案在此维护 */
export type BodyPart =
  | 'KNEE'
  | 'SHOULDER'
  | 'LOWER_BACK'
  | 'HIP'
  | 'ANKLE'
  | 'WRIST'
  | 'ELBOW'
  | 'NECK'
  | 'OTHER'

export interface HealthConstraint {
  id?: number
  type: HealthConstraintType
  bodyPart: BodyPart
  severity: ConstraintSeverity
  description: string
  active: boolean
}

export interface HealthConstraintsData {
  constraints: HealthConstraint[]
  medicalNotice: string | null
}

/** 饮食档案（3.4） */
export type DietType = 'OMNIVORE' | 'VEGETARIAN' | 'VEGAN' | 'PESCATARIAN' | 'KETO' | 'OTHER'

export type AllergenCode =
  | 'PEANUT'
  | 'TREE_NUT'
  | 'MILK'
  | 'EGG'
  | 'SOY'
  | 'WHEAT'
  | 'FISH'
  | 'SHELLFISH'
  | 'SESAME'

export type DietaryRestriction =
  | 'LOW_SUGAR'
  | 'LOW_SODIUM'
  | 'LOW_FAT'
  | 'HIGH_PROTEIN'
  | 'HALAL'
  | 'VEGAN_ONLY'

export interface NutritionProfileData {
  dietType: DietType
  allergies: AllergenCode[]
  restrictions: DietaryRestriction[]
  dislikedFoods: string[]
  preferredCuisines: string[]
  dailyWaterMlTarget: number
}
