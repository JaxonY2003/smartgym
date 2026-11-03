/**
 * 动作、肌群和器械相关类型（接口文档 §4）
 */

/** 难度（4.1 示例值为 BEGINNER；按三档惯例补全，后端定稿后对齐） */
export type ExerciseDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'

/** 肌群代码（4.1 示例值 QUADRICEPS/GLUTES；按常见分区补全，后端定稿后对齐） */
export type MuscleGroupCode =
  | 'CHEST'
  | 'BACK'
  | 'SHOULDERS'
  | 'QUADRICEPS'
  | 'HAMSTRINGS'
  | 'GLUTES'
  | 'CALVES'
  | 'BICEPS'
  | 'TRICEPS'
  | 'FOREARMS'
  | 'CORE'
  | 'FULL_BODY'

export type MuscleGroupStatus = 'ACTIVE' | 'INACTIVE'

export interface MuscleGroup {
  id: number
  code: MuscleGroupCode | string
  name: string
  parentId: number | null
  sortOrder: number
  status: MuscleGroupStatus
}

export interface MuscleGroupQuery {
  status?: MuscleGroupStatus
  parentId?: number
  page: number
  pageSize: number
}

export interface EquipmentRef {
  id: number
  name: string
}

/** 动作列表项 / 详情（4.1） */
export interface Exercise {
  id: number
  name: string
  englishName: string | null
  /** 动作描述（详情接口返回） */
  description?: string | null
  /** 动作步骤说明（详情接口返回） */
  instructions?: string | null
  primaryMuscles: MuscleGroupCode[]
  equipment: EquipmentRef[]
  difficulty: ExerciseDifficulty
  defaultRestSeconds: number | null
  /** 禁忌与注意事项，如"严重膝关节疼痛" */
  contraindications: string[]
  /** 图像 URL：当前为 null，后续接入 MinIO 对象存储后下发 */
  thumbnailUrl: string | null
}

export interface ExerciseQuery {
  keyword?: string
  muscleGroup?: MuscleGroupCode
  equipmentId?: number
  page: number
  pageSize: number
}

/** 器械类别（4.2 示例值 FREE_WEIGHT；按常见类别补全） */
export type EquipmentCategory =
  | 'FREE_WEIGHT'
  | 'MACHINE'
  | 'CARDIO'
  | 'BODYWEIGHT'
  | 'ACCESSORY'
  | 'OTHER'

/** 器械状态（4.3） */
export type EquipmentStatus = 'ACTIVE' | 'INACTIVE'

export interface Equipment {
  id: number
  name: string
  category: EquipmentCategory
  status: EquipmentStatus
  brand: string | null
  description: string | null
}
