import { rawHttp } from '@/utils/request'
import type { ApiEnvelope } from '@/types/api/common'
import type { LoginRequest, LoginResult, RefreshResult, RegisterRequest } from '@/types/api/auth'

/** 登录/注册/刷新走裸实例：不注入 token、不参与 401 重试 */
export async function login(data: LoginRequest): Promise<LoginResult> {
  const { data: body } = await rawHttp.post<ApiEnvelope<LoginResult>>('/auth/login', data)
  if (!body.success) throw new Error(body.message)
  return body.data
}

export async function register(data: RegisterRequest): Promise<{ userId: number; account: string; displayName: string }> {
  const { data: body } = await rawHttp.post<ApiEnvelope<{ userId: number; account: string; displayName: string }>>(
    '/auth/register',
    data,
  )
  if (!body.success) throw new Error(body.message)
  return body.data
}

export async function refreshToken(refreshToken: string): Promise<RefreshResult> {
  const { data: body } = await rawHttp.post<ApiEnvelope<RefreshResult>>('/auth/refresh', { refreshToken })
  if (!body.success) throw new Error(body.message)
  return body.data
}

export async function logout(): Promise<void> {
  await rawHttp.post('/auth/logout')
}
