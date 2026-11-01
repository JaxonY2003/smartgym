/**
 * 首页与趋势看板 API（接口文档 §7）
 */
import request from '@/utils/request'
import type { ApiEnvelope } from '@/types/api/common'
import type { DashboardOverview, TrendData, TrendMetric } from '@/types/api/dashboard'

/** 首页概览（7.1） */
export async function getOverview(date?: string): Promise<DashboardOverview> {
  const { data: body } = await request.get<ApiEnvelope<DashboardOverview>>('/dashboard/overview', {
    params: date ? { date } : {},
    silent: true,
  })
  return body.data
}

/** 趋势数据（7.2）：metric 为 WEIGHT / TRAINING_VOLUME / NUTRITION */
export async function getTrend(
  metric: TrendMetric,
  from: string,
  to: string,
): Promise<TrendData> {
  const { data: body } = await request.get<ApiEnvelope<TrendData>>('/dashboard/trends', {
    params: { metric, from, to },
    silent: true,
  })
  return body.data
}
