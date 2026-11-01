/**
 * Token 纯存储层：只依赖 localStorage，不依赖 Pinia / Router / API，
 * 供 request.ts 和 auth store 共同使用，避免循环依赖。
 */
const ACCESS_KEY = 'sg_access_token'
const REFRESH_KEY = 'sg_refresh_token'

export const tokenStorage = {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY)
  },
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY)
  },
  set(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_KEY, accessToken)
    localStorage.setItem(REFRESH_KEY, refreshToken)
  },
  clear(): void {
    localStorage.removeItem(ACCESS_KEY)
    localStorage.removeItem(REFRESH_KEY)
  },
}
