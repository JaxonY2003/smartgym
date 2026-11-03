/**
 * 会员健康档案 API（接口文档 §3）
 * 前缀 /api/v1 由 baseURL 提供；Idempotency-Key 由 request.ts 拦截器自动注入。
 */
import request from '@/utils/request'
import type { ApiEnvelope, PageData } from '@/types/api/common'
import type {
  BodyMetric,
  BodyMetricInput,
  HealthConstraint,
  HealthConstraintsData,
  MemberProfile,
  MemberProfileInput,
  NutritionProfileData,
} from '@/types/api/member'

/** 查询健康档案（3.1）。404 RESOURCE_NOT_FOUND 表示尚未建档 */
export async function getProfile(): Promise<MemberProfile> {
  const { data: body } = await request.get<ApiEnvelope<MemberProfile>>('/member/profile', {
    silent: true,
  })
  return body.data
}

/** 新建或更新健康档案（3.2，PUT 全量） */
export async function updateProfile(data: MemberProfileInput): Promise<MemberProfile> {
  const { data: body } = await request.put<ApiEnvelope<MemberProfile>>('/member/profile', data)
  return body.data
}

/** 分页查询身体指标记录（3.3） */
export async function getBodyMetrics(
  params: { from?: string; to?: string } & { page: number; pageSize: number },
): Promise<PageData<BodyMetric>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<BodyMetric>>>(
    '/member/body-metrics',
    { params, silent: true },
  )
  return body.data
}

/** 新建身体指标记录（3.3） */
export async function createBodyMetric(data: BodyMetricInput): Promise<BodyMetric> {
  const { data: body } = await request.post<ApiEnvelope<BodyMetric>>('/member/body-metrics', data)
  return body.data
}

/** 查询健康限制（3.4）。后端 data 为裸数组（无独立 medicalNotice），转为前端结构 */
export async function getHealthConstraints(): Promise<HealthConstraintsData> {
  const { data: body } = await request.get<ApiEnvelope<HealthConstraint[] | HealthConstraintsData>>(
    '/member/health-constraints',
    { silent: true },
  )
  const data = body.data
  if (Array.isArray(data)) return { constraints: data, medicalNotice: null }
  return data
}

/** 更新健康限制（3.4，PUT 全量） */
export async function updateHealthConstraints(data: HealthConstraintsData): Promise<HealthConstraintsData> {
  const { data: body } = await request.put<ApiEnvelope<HealthConstraintsData>>(
    '/member/health-constraints',
    data,
  )
  return body.data
}

/** 查询饮食档案（3.4） */
export async function getNutritionProfile(): Promise<NutritionProfileData> {
  const { data: body } = await request.get<ApiEnvelope<NutritionProfileData>>(
    '/member/nutrition-profile',
    { silent: true },
  )
  return body.data
}

/** 更新饮食档案（3.4，PUT 全量） */
export async function updateNutritionProfile(data: NutritionProfileData): Promise<NutritionProfileData> {
  const { data: body } = await request.put<ApiEnvelope<NutritionProfileData>>(
    '/member/nutrition-profile',
    data,
  )
  return body.data
}
