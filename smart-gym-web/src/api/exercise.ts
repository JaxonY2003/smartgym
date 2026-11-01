/**
 * 动作、肌群和器械 API（接口文档 §4）
 */
import request from '@/utils/request'
import type { ApiEnvelope, PageData } from '@/types/api/common'
import type { Equipment, Exercise, ExerciseQuery } from '@/types/api/exercise'

/** 分页查询动作（4.1），支持关键词/肌群/器械筛选 */
export async function getExercises(query: ExerciseQuery): Promise<PageData<Exercise>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<Exercise>>>('/exercises', {
    params: query,
    silent: true,
  })
  return body.data
}

/** 动作详情（4.1） */
export async function getExercise(id: number): Promise<Exercise> {
  const { data: body } = await request.get<ApiEnvelope<Exercise>>(`/exercises/${id}`, {
    silent: true,
  })
  return body.data
}

/** 分页查询器械（4.2） */
export async function getEquipment(
  query: { status?: string } & { page: number; pageSize: number },
): Promise<PageData<Equipment>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<Equipment>>>('/equipment', {
    params: query,
    silent: true,
  })
  return body.data
}
