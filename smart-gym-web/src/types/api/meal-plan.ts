/**
 * 饮食计划相关类型（接口文档 6.5）
 */
import type { FitnessGoal } from './member'
import type { ItemNutrition, MealType } from './nutrition'

/** 计划内餐次的食物项（6.5）。后端用 foodNameSnapshot 存名称快照 */
export interface MealPlanItem {
  id?: number
  foodId: number
  foodName?: string
  /** 后端返回字段名 */
  foodNameSnapshot?: string
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

/** 计划日：后端详情按日期分日（6.5），单日计划时 days 长度为 1 */
export interface MealPlanDay {
  id?: number
  date?: string
  dayIndex?: number
  meals: MealPlanMeal[]
}

/** 饮食计划 */
export interface MealPlan {
  id: number
  name: string
  date: string
  goal: FitnessGoal | null
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  source?: 'AGENT' | 'MANUAL' | 'SMARTGYM_SEED'
  /** 列表接口不返回；详情接口为 days→meals 嵌套 */
  meals?: MealPlanMeal[]
  days?: MealPlanDay[]
  estimatedNutrition?: ItemNutrition
  totalNutrition?: ItemNutrition
  warnings: string[]
  assumptions: string[]
  createdAt?: string
}

/** 归一化视图：把 days→meals 拍平成 meals，供组件渲染 */
export function mealsOf(plan: MealPlan): MealPlanMeal[] {
  if (plan.meals?.length) return plan.meals
  return (plan.days ?? []).flatMap((d) => d.meals ?? [])
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
