/**
 * 饮食计划相关类型（接口文档 6.5）
 */
import type { FitnessGoal } from './member'
import type { ItemNutrition, MealType } from './nutrition'

/** 计划内餐次的食物项（6.5 创建请求） */
export interface MealPlanItem {
  id?: number
  foodId: number
  foodName?: string
  amount: number
  unit: string
}

/** 计划内餐次 */
export interface MealPlanMeal {
  id?: number
  mealType: MealType
  time: string
  items: MealPlanItem[]
  estimatedNutrition?: ItemNutrition
}

/** 饮食计划 */
export interface MealPlan {
  id: number
  name: string
  date: string
  goal: FitnessGoal | null
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  source?: 'AGENT' | 'MANUAL'
  meals: MealPlanMeal[]
  totalNutrition?: ItemNutrition
  warnings: string[]
  assumptions: string[]
  createdAt?: string
}

export interface MealPlanInput {
  name: string
  date: string
  goal: FitnessGoal
  meals: { mealType: MealType; time: string; items: { foodId: number; amount: number; unit: string }[] }[]
  assumptions?: string[]
}

/** 替换食物请求（6.5 replace-item） */
export interface ReplaceItemInput {
  itemId: number
  replacementFoodId: number
  reason?: string
}
