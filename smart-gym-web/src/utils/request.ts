import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { tokenStorage } from './token'
import { errorMessageOf } from '@/constants/errorMessages'
import type { ApiEnvelope } from '@/types/api/common'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 外部指定或 401 刷新后重放时复用的幂等键 */
    idempotencyKey?: string
    /** 防止 401 重试死循环 */
    retried?: boolean
    /** 静默请求：失败不弹全局提示，由调用方自行处理 */
    silent?: boolean
    /** 跳过自动注入 Authorization（登录、刷新等接口用） */
    skipAuth?: boolean
  }
}

/** 业务错误：携带后端错误码，VALIDATION_ERROR 时 data 为 { fieldErrors } */
export class ApiError extends Error {
  code: string
  data?: unknown
  status?: number

  constructor(code: string, message: string, data?: unknown, status?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
    this.status = status
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL

/** 裸实例：仅供 login / refresh 等无拦截器请求使用，天然不会递归 401。
 *  仅做错误信封 → ApiError 的归一化（登录页需要业务错误码显示"账号或密码错误"等文案），
 *  不注入 token、不参与 401 重试、不弹全局提示。 */
const rawHttp = axios.create({ baseURL: BASE_URL, timeout: 15_000 })

rawHttp.interceptors.response.use(
  (res) => {
    const body = res.data as ApiEnvelope<unknown> | undefined
    if (body && typeof body.success === 'boolean' && !body.success) {
      throw new ApiError(body.code, body.message, body.data, res.status)
    }
    return res
  },
  (error) => {
    const body = error?.response?.data as ApiEnvelope<unknown> | undefined
    if (body && typeof body.success === 'boolean') {
      return Promise.reject(new ApiError(body.code, body.message, body.data, error.response?.status))
    }
    return Promise.reject(error)
  },
)

export { rawHttp }

const request = axios.create({ baseURL: BASE_URL, timeout: 15_000 })

const WRITE_METHODS = new Set(['post', 'put', 'patch', 'delete'])
const AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh']

/** crypto.randomUUID 仅在 secure context 可用，内网 http 环境需兜底 */
function uuid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return '10000000-1000-4000-8000-100000000000'.replace(/[018]/g, (c) =>
    (+c ^ (crypto.getRandomValues(new Uint8Array(1))[0] & (15 >> (+c / 4)))).toString(16),
  )
}

function isAuthPath(url?: string): boolean {
  return !!url && AUTH_PATHS.some((p) => url.startsWith(p))
}

request.interceptors.request.use((config) => {
  if (!config.skipAuth && !isAuthPath(config.url)) {
    const token = tokenStorage.getAccessToken()
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  if (WRITE_METHODS.has(config.method?.toLowerCase() ?? '')) {
    config.idempotencyKey ??= uuid()
    config.headers['Idempotency-Key'] = config.idempotencyKey
  }
  return config
})

/** 并发 401 时只发一次 refresh，其余请求挂起等待同一 Promise */
let refreshing: Promise<void> | null = null

request.interceptors.response.use(
  (res) => {
    // 204 无响应体；actuator 等非信封结构原样放行
    const body = res.data as ApiEnvelope<unknown> | undefined
    if (body && typeof body.success === 'boolean') {
      if (!body.success) throw new ApiError(body.code, body.message, body.data, res.status)
    }
    return res
  },
  async (error) => {
    const { response, config } = error

    // ---- 401：尝试刷新后重放一次 ----
    if (
      response?.status === 401 &&
      config &&
      !config.retried &&
      !config.skipAuth &&
      !isAuthPath(config.url)
    ) {
      config.retried = true
      refreshing ??= (async () => {
        const refreshToken = tokenStorage.getRefreshToken()
        if (!refreshToken) throw new ApiError('AUTH_TOKEN_INVALID', '缺少刷新令牌')
        const { data } = await rawHttp.post<ApiEnvelope<{ accessToken: string; refreshToken: string }>>(
          '/auth/refresh',
          { refreshToken },
        )
        tokenStorage.set(data.data.accessToken, data.data.refreshToken)
      })().finally(() => {
        refreshing = null
      })

      try {
        await refreshing
        return request(config) // 复用同一 config → 同一个 Idempotency-Key
      } catch {
        tokenStorage.clear()
        // 硬跳转绕开 router 循环导入，顺带终止所有 in-flight 请求
        if (!location.pathname.startsWith('/login')) location.href = '/login'
        return new Promise(() => {}) // 挂起，不再向下抛
      }
    }

    // ---- 有响应：统一解析信封（400/409/422/500 等同样返回统一失败结构） ----
    if (response) {
      const body = response.data as ApiEnvelope<unknown> | undefined
      if (body && typeof body.success === 'boolean') {
        const apiError = new ApiError(body.code, body.message, body.data, response.status)
        notify(apiError, config)
        return Promise.reject(apiError)
      }
      const httpError = new ApiError(`HTTP_${response.status}`, error.message, undefined, response.status)
      notify(httpError, config)
      return Promise.reject(httpError)
    }

    // ---- 无响应：网络/超时 ----
    const networkError = new ApiError('NETWORK_ERROR', '网络异常，请检查网络后重试')
    notify(networkError, config)
    return Promise.reject(networkError)
  },
)

function notify(error: ApiError, config?: AxiosRequestConfig) {
  if (config?.silent) return
  ElMessage({ type: 'error', message: errorMessageOf(error.code), grouping: true })
}

export default request
