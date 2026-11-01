/**
 * 错误码 → 用户可见文案映射（接口文档 1.6）
 * 前端不根据后端 message 文案判断业务（文档 12.1），展示文案统一维护在这里。
 */
export const ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: '账号或密码错误',
  AUTH_TOKEN_INVALID: '登录状态无效，请重新登录',
  AUTH_TOKEN_EXPIRED: '登录已过期，请重新登录',
  FORBIDDEN: '没有权限执行此操作',
  RESOURCE_NOT_FOUND: '资源不存在',
  VALIDATION_ERROR: '请求参数校验失败',
  BUSINESS_RULE_VIOLATION: '不满足训练、营养或安全规则',
  CONFIRMATION_REQUIRED: '该操作需要先确认',
  IDEMPOTENCY_CONFLICT: '重复提交且请求内容不一致',
  AGENT_RUN_NOT_FOUND: 'Agent 运行记录不存在',
  AGENT_OUTPUT_INVALID: '模型输出无法解析，请重试',
  SAFETY_BLOCKED: '该请求存在健康风险，已被拦截',
  MODEL_UNAVAILABLE: '智能服务暂时不可用，请稍后重试',
  RATE_LIMITED: '操作过于频繁，请稍后再试',
  INTERNAL_ERROR: '服务开小差了，请稍后重试',
}

export const UNKNOWN_ERROR_MESSAGE = '请求失败，请稍后重试'

export function errorMessageOf(code?: string): string {
  if (code && ERROR_MESSAGES[code]) return ERROR_MESSAGES[code]
  if (code?.startsWith('HTTP_')) return `请求失败（${code.slice(5)}）`
  return UNKNOWN_ERROR_MESSAGE
}

/** VALIDATION_ERROR 时 ApiError.data 的结构（接口文档 1.3） */
export interface FieldError {
  field: string
  reason: string
}
