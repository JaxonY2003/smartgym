/**
 * 首页与趋势看板相关类型（接口文档 §7）
 */

/** 今日训练概览（7.1） */
export interface TodayWorkout {
  planId: number
  title: string
  estimatedMinutes: number | null
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REST_DAY'
}

/** 概览响应（7.1） */
export interface DashboardOverview {
  date: string
  todayWorkout: TodayWorkout | null
  nutrition: {
    caloriesConsumed: number
    caloriesTarget: number
    proteinConsumedG: number
    proteinTargetG: number
  }
  weeklyWorkoutCompletionRate: number
  latestWeightKg: number | null
  weightChangeKg7d: number | null
  quickQuestions: string[]
}

/** 趋势指标（7.2） */
export type TrendMetric = 'WEIGHT' | 'TRAINING_VOLUME' | 'NUTRITION'

export interface TrendPoint {
  date: string
  value: number
}

export interface TrendData {
  metric: TrendMetric
  unit: string
  points: TrendPoint[]
}
