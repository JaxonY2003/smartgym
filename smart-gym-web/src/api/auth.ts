import request, { rawHttp } from '@/utils/request'
import { errorMessageOf } from '@/constants/errorMessages'
import type { ApiEnvelope } from '@/types/api/common'
import type { LoginRequest, LoginResult, RefreshResult, RegisterRequest } from '@/types/api/auth'

/** 登录/注册/刷新走裸实例：不注入 token、不参与 401 重试；失败信封转为带错误码的异常 */
function unwrap<T>(body: ApiEnvelope<T>): T {
  if (!body.success) throw Object.assign(new Error(body.message || errorMessageOf(body.code)), { code: body.code })
  return body.data
}

export async function login(data: LoginRequest): Promise<LoginResult> {
  const { data: body } = await rawHttp.post<ApiEnvelope<LoginResult>>('/auth/login', data)
  return unwrap(body)
}

export async function register(data: RegisterRequest): Promise<{ userId: number; account: string; displayName: string }> {
  const { data: body } = await rawHttp.post<ApiEnvelope<{ userId: number; account: string; displayName: string }>>(
    '/auth/register',
    data,
  )
  return unwrap(body)
}

export async function refreshToken(refreshToken: string): Promise<RefreshResult> {
  const { data: body } = await rawHttp.post<ApiEnvelope<RefreshResult>>('/auth/refresh', { refreshToken })
  return unwrap(body)
}

export async function logout(): Promise<void> {
  await request.post('/auth/logout')
}
