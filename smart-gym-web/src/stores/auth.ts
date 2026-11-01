import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { tokenStorage } from '@/utils/token'
import type { CurrentUser, LoginRequest, RegisterRequest } from '@/types/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(tokenStorage.getAccessToken())
  const user = ref<CurrentUser | null>(null)

  const isLoggedIn = computed(() => accessToken.value !== null)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)

  async function login(payload: LoginRequest): Promise<void> {
    const result = await authApi.login(payload)
    tokenStorage.set(result.accessToken, result.refreshToken)
    accessToken.value = result.accessToken
    user.value = { ...result.user, avatarUrl: null, createdAt: '' }
  }

  async function register(payload: RegisterRequest): Promise<void> {
    await authApi.register(payload)
  }

  async function fetchCurrentUser(): Promise<void> {
    // 延迟导入避免 request.ts → store 循环（request 不依赖 store，反向也保持无环）
    const { default: request } = await import('@/utils/request')
    const { data: body } = await request.get<{ success: boolean; data: CurrentUser }>('/auth/me', {
      silent: true,
    })
    user.value = body.data
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      tokenStorage.clear()
      accessToken.value = null
      user.value = null
    }
  }

  /** 仅清除本地态（401 刷新失败的兜底路径，request.ts 里已做硬跳转） */
  function clearLocal() {
    tokenStorage.clear()
    accessToken.value = null
    user.value = null
  }

  return {
    accessToken,
    user,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchCurrentUser,
    logout,
    clearLocal,
  }
})
