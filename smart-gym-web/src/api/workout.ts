/**
 * 训练计划与训练记录 API（接口文档 §5）
 */
import request from '@/utils/request'
import type { ApiEnvelope, PageData } from '@/types/api/common'
import type {
  WorkoutFeedbackInput,
  WorkoutPlan,
  WorkoutPlanInput,
  WorkoutSession,
} from '@/types/api/workout'

/** 分页查询训练计划（5.1） */
export async function getWorkoutPlans(
  query: { status?: string } & { page: number; pageSize: number },
): Promise<PageData<WorkoutPlan>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<WorkoutPlan>>>('/workout-plans', {
    params: query,
    silent: true,
  })
  return body.data
}

/** 查询某日期应执行的训练日（5.1 today） */
export async function getTodayPlan(date?: string): Promise<WorkoutPlanDayView | null> {
  const { data: body } = await request.get<ApiEnvelope<WorkoutPlanDayView | null>>(
    '/workout-plans/today',
    { params: date ? { date } : {}, silent: true },
  )
  return body.data
}

/** 计划详情（5.1） */
export async function getWorkoutPlan(planId: number): Promise<WorkoutPlan> {
  const { data: body } = await request.get<ApiEnvelope<WorkoutPlan>>(`/workout-plans/${planId}`, {
    silent: true,
  })
  return body.data
}

/** 手动创建计划（5.2） */
export async function createWorkoutPlan(input: WorkoutPlanInput): Promise<WorkoutPlan> {
  const { data: body } = await request.post<ApiEnvelope<WorkoutPlan>>('/workout-plans', input)
  return body.data
}

/** 修改计划（5.3，支持部分字段） */
export async function patchWorkoutPlan(
  planId: number,
  changes: Partial<Pick<WorkoutPlan, 'name' | 'status'>>,
): Promise<WorkoutPlan> {
  const { data: body } = await request.patch<ApiEnvelope<WorkoutPlan>>(
    `/workout-plans/${planId}`,
    changes,
  )
  return body.data
}

/** 激活计划（5.3） */
export async function activateWorkoutPlan(planId: number): Promise<WorkoutPlan> {
  const { data: body } = await request.post<ApiEnvelope<WorkoutPlan>>(
    `/workout-plans/${planId}/activate`,
  )
  return body.data
}

/** 删除计划（5.3，逻辑删除） */
export async function deleteWorkoutPlan(planId: number): Promise<void> {
  await request.delete(`/workout-plans/${planId}`)
}

/** 分页查询训练记录（5.5） */
export async function getWorkoutSessions(
  query: { from?: string; to?: string; status?: string } & { page: number; pageSize: number },
): Promise<PageData<WorkoutSession>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<WorkoutSession>>>(
    '/workout-sessions',
    { params: query, silent: true },
  )
  return body.data
}

/** 创建训练执行记录（5.4） */
export async function createWorkoutSession(input: WorkoutSession): Promise<WorkoutSession> {
  const { data: body } = await request.post<ApiEnvelope<WorkoutSession>>('/workout-sessions', input)
  return body.data
}

/** 提交训练反馈（5.5） */
export async function submitWorkoutFeedback(
  sessionId: number,
  input: WorkoutFeedbackInput,
): Promise<void> {
  await request.post(`/workout-sessions/${sessionId}/feedback`, input)
}

/** today 接口返回的训练日视图（计划字段 + 日字段平铺） */
export interface WorkoutPlanDayView {
  planId: number
  planName: string
  dayId: number
  dayOfWeek: number
  title: string
  estimatedMinutes: number | null
  exercises: {
    exerciseId: number
    exerciseName: string
    sets: number
    targetReps: string
    restSeconds: number | null
    notes: string | null
  }[]
}
