/**
 * 食物、饮食记录、营养汇总、营养目标与饮水 API（接口文档 §6）
 */
import request from '@/utils/request'
import type { ApiEnvelope, PageData } from '@/types/api/common'
import type { MealPlan, MealPlanInput, ReplaceItemInput } from '@/types/api/meal-plan'
import type {
  DailySummary,
  Food,
  FoodQuery,
  MealRecord,
  MealRecordInput,
  NutritionTarget,
} from '@/types/api/nutrition'

/** 分页查询食物（6.1） */
export async function getFoods(query: FoodQuery): Promise<PageData<Food>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<Food>>>('/foods', {
    params: query,
    silent: true,
  })
  return body.data
}

/** 食物详情（6.1） */
export async function getFood(foodId: number): Promise<Food> {
  const { data: body } = await request.get<ApiEnvelope<Food>>(`/foods/${foodId}`, {
    silent: true,
  })
  return body.data
}

/** 分页查询饮食记录（6.4） */
export async function getMealRecords(
  query: { date?: string; mealType?: string } & { page: number; pageSize: number },
): Promise<PageData<MealRecord>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<MealRecord>>>('/meal-records', {
    params: query,
    silent: true,
  })
  return body.data
}

/** 创建饮食记录（6.3） */
export async function createMealRecord(input: MealRecordInput): Promise<MealRecord> {
  const { data: body } = await request.post<ApiEnvelope<MealRecord>>('/meal-records', input)
  return body.data
}

/** 每日营养汇总（6.4） */
export async function getDailySummary(date: string): Promise<DailySummary> {
  const { data: body } = await request.get<ApiEnvelope<DailySummary>>(
    '/nutrition/daily-summary',
    { params: { date }, silent: true },
  )
  return body.data
}

/** 查询营养目标（6.2） */
export async function getNutritionTarget(): Promise<NutritionTarget> {
  const { data: body } = await request.get<ApiEnvelope<NutritionTarget>>(
    '/nutrition-targets/current',
    { silent: true },
  )
  return body.data
}

/** 更新营养目标（6.2，PUT 全量） */
export async function updateNutritionTarget(input: NutritionTarget): Promise<NutritionTarget> {
  const { data: body } = await request.put<ApiEnvelope<NutritionTarget>>(
    '/nutrition-targets/current',
    input,
  )
  return body.data
}

/** 记录饮水（6.6） */
export async function createWaterRecord(recordedAt: string, amountMl: number): Promise<void> {
  await request.post('/water-records', { recordedAt, amountMl })
}

/** 查询某日饮水记录（6.6）。后端返回记录裸数组，前端聚合出总量 */
export async function getWaterRecords(date: string): Promise<{ totalMl: number; records: { recordedAt: string; amountMl: number }[] }> {
  const { data: body } = await request.get<ApiEnvelope<{ recordedAt: string; amountMl: number }[] | { totalMl: number; records: { recordedAt: string; amountMl: number }[] }>>(
    '/water-records',
    { params: { date }, silent: true },
  )
  const data = body.data
  if (Array.isArray(data)) {
    return {
      totalMl: data.reduce((sum, r) => sum + (r.amountMl ?? 0), 0),
      records: data,
    }
  }
  return data
}

/* ---------- 饮食计划（6.5） ---------- */

/** 分页查询饮食计划 */
export async function getMealPlans(
  query: { status?: string } & { page: number; pageSize: number },
): Promise<PageData<MealPlan>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<MealPlan>>>('/meal-plans', {
    params: query,
    silent: true,
  })
  return body.data
}

/** 饮食计划详情 */
export async function getMealPlan(planId: number): Promise<MealPlan> {
  const { data: body } = await request.get<ApiEnvelope<MealPlan>>(`/meal-plans/${planId}`, {
    silent: true,
  })
  return body.data
}

/** 创建饮食计划 */
export async function createMealPlan(input: MealPlanInput): Promise<MealPlan> {
  const { data: body } = await request.post<ApiEnvelope<MealPlan>>('/meal-plans', input)
  return body.data
}

/** 修改饮食计划（部分字段） */
export async function patchMealPlan(
  planId: number,
  changes: Partial<Pick<MealPlan, 'name' | 'status'>>,
): Promise<MealPlan> {
  const { data: body } = await request.patch<ApiEnvelope<MealPlan>>(`/meal-plans/${planId}`, changes)
  return body.data
}

/** 激活饮食计划 */
export async function activateMealPlan(planId: number): Promise<MealPlan> {
  const { data: body } = await request.post<ApiEnvelope<MealPlan>>(`/meal-plans/${planId}/activate`)
  return body.data
}

/** 替换计划中的食物（服务端重算营养并做过敏原检查） */
export async function replaceMealPlanItem(
  planId: number,
  input: ReplaceItemInput,
): Promise<MealPlan> {
  const { data: body } = await request.post<ApiEnvelope<MealPlan>>(
    `/meal-plans/${planId}/replace-item`,
    input,
  )
  return body.data
}
