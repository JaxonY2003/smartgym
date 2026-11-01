export type UserRole = 'MEMBER' | 'TRAINER' | 'ADMIN'

export interface CurrentUser {
  id: number
  account: string
  displayName: string
  avatarUrl: string | null
  roles: UserRole[]
  createdAt: string
}

export interface LoginRequest {
  account: string
  password: string
}

export interface RegisterRequest {
  account: string
  password: string
  displayName: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: {
    id: number
    account: string
    displayName: string
    roles: UserRole[]
  }
}

export interface RefreshResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}
