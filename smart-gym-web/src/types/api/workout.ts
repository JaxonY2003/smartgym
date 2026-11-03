/**
 * 训练计划与训练记录相关类型（接口文档 §5）
 */
import type { FitnessGoal } from './member'

/** 计划来源（5.1 示例值 AGENT） */
export type WorkoutPlanSource = 'AGENT' | 'MANUAL' | 'TEMPLATE'

/** 计划状态（5.3 PATCH 示例值 PAUSED；DRAFT/ACTIVE/PAUSED/ARCHIVED 为常见全集，后端定稿后对齐） */
export type WorkoutPlanStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED'

/** 计划内单个动作安排（5.1）。后端返回 targetRepsMin/Max + 可选 targetReps 拼接串 */
export interface PlanExercise {
  exerciseId: number
  exerciseName: string
  sets: number
  /** 详情可能返回拼接串；创建时用 min/max 上送 */
  targetReps?: string
  targetRepsMin?: number
  targetRepsMax?: number
  restSeconds: number | null
  tempo: string | null
  notes: string | null
}

/** 计划内训练日（5.1） */
export interface WorkoutPlanDay {
  id: number
  dayOfWeek: number
  title: string
  estimatedMinutes: number | null
  exercises: PlanExercise[]
}

/** 训练计划（5.1 / 5.2） */
export interface WorkoutPlan {
  id: number
  name: string
  source: WorkoutPlanSource
  status: WorkoutPlanStatus
  goal: FitnessGoal | null
  durationWeeks: number
  trainingDaysPerWeek: number
  days: WorkoutPlanDay[]
  warnings: string[]
  createdAt: string
}

/** 手动创建计划的请求（5.2）：动作用拆开的 reps min/max，天数不带 id */
export interface PlanExerciseInput {
  exerciseId: number
  sets: number
  targetRepsMin: number
  targetRepsMax: number
  restSeconds: number
  notes?: string
}

export interface PlanDayInput {
  dayOfWeek: number
  title: string
  estimatedMinutes: number
  exercises: PlanExerciseInput[]
}

export interface WorkoutPlanInput {
  name: string
  goal: FitnessGoal
  durationWeeks: number
  trainingDaysPerWeek: number
  days: PlanDayInput[]
}

/** 训练执行记录（5.4 / 5.5） */
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'

export interface SessionSet {
  exerciseId: number
  setIndex: number
  weightKg: number | null
  reps: number | null
  completed: boolean
  restSeconds: number | null
}

export interface WorkoutSession {
  id: number
  planId: number | null
  planDayId: number | null
  startedAt: string
  endedAt: string | null
  status: SessionStatus
  sets: SessionSet[]
  fatigueLevel: number | null
  painBodyPart: string | null
  painLevel: number | null
  note: string | null
  /** 汇总字段（创建响应示例） */
  durationMinutes?: number
  totalVolumeKg?: number
  createdAt?: string
}

/** 训练后反馈（5.5） */
export interface WorkoutFeedbackInput {
  fatigueLevel: number
  painBodyPart: string | null
  painLevel: number
  feeling: string
  shouldAdjustNextPlan: boolean
}
