/**
 * 食物、饮食记录与营养汇总相关类型（接口文档 §6）
 */

/** 餐次（6.3 示例值 BREAKFAST） */
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK'

/** 食物类别（6.1 示例值 PROTEIN；按常见分类补全，后端定稿后对齐） */
export type FoodCategory =
  | 'PROTEIN'
  | 'GRAIN'
  | 'VEGETABLE'
  | 'FRUIT'
  | 'DAIRY'
  | 'NUT'
  | 'BEVERAGE'
  | 'OTHER'

/** 每 100g 营养（6.1） */
export interface NutritionPer100g {
  calories: number
  protein: number
  carbohydrate: number
  fat: number
  fiber: number | null
  sugar: number | null
  sodium: number | null
}

/** 食物（6.1） */
export interface Food {
  id: number
  name: string
  category: FoodCategory
  defaultServingAmount: number
  defaultServingUnit: string
  nutritionPer100g: NutritionPer100g
  allergens: string[]
  source: string | null
  sourceVersion: string | null
}

export interface FoodQuery {
  keyword?: string
  category?: FoodCategory
  excludeAllergens?: string
  page: number
  pageSize: number
}

/** 记录条目的营养估算（6.3） */
export interface ItemNutrition {
  calories: number
  proteinG: number
  carbohydrateG: number
  fatG: number
}

export interface MealRecordItem {
  foodId: number
  foodNameSnapshot: string
  amount: number
  unit: string
  estimatedNutrition: ItemNutrition
  confidence: number
}

/** 饮食记录（6.3 / 6.4） */
export interface MealRecord {
  id: number
  eatenAt: string
  mealType: MealType
  items: MealRecordItem[]
  totalNutrition: ItemNutrition
  inputSource: string | null
  note: string | null
  createdAt: string
}

export interface MealRecordInput {
  eatenAt: string
  mealType: MealType
  items: { foodId: number; amount: number; unit: string; confirmedByUser: boolean }[]
  inputSource: 'MANUAL' | 'AGENT'
  note?: string
}

/** 每日汇总（6.4） */
export interface DailySummary {
  date: string
  target: { calories: number; proteinG: number; carbohydrateG: number; fatG: number }
  consumed: { calories: number; proteinG: number; carbohydrateG: number; fatG: number }
  completionRate: {
    calories: number
    protein: number
    carbohydrate: number
    fat: number
  }
  mealCount: number
  recordCompleteness: number
}

/** 营养目标（6.2） */
export interface NutritionTarget {
  calories: number
  proteinG: number
  carbohydrateG: number
  fatG: number
  fiberG: number | null
  source: string
}

/** 饮水记录（6.6） */
export interface WaterRecord {
  id: number
  recordedAt: string
  amountMl: number
}
