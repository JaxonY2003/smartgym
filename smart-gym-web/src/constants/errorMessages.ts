/**
 * 错误码 → 用户可见文案映射（接口文档 1.6）
 * 前端不根据后端 message 文案判断业务（文档 12.1），展示文案统一维护在这里。
 */
export const ERROR_MESSAGES: Record<string, string> = {
  AUTH_INVALID_CREDENTIALS: '账号或密码错误',
  AUTH_TOKEN_INVALID: '登录状态无效，请重新登录',
  AUTH_TOKEN_EXPIRED: '登录已过期，请重新登录',
  ACCOUNT_ALREADY_EXISTS: '该账号已被注册',
  ACCOUNT_DISABLED: '账号已被禁用，请联系管理员',
  DEFAULT_ROLE_NOT_FOUND: '注册服务暂时不可用，请联系管理员',
  FORBIDDEN: '没有权限执行此操作',
  RESOURCE_NOT_FOUND: '资源不存在',
  ALLERGEN_NOT_FOUND: '过敏原不存在，请刷新后重新选择',
  MUSCLE_GROUP_NOT_FOUND: '肌群不存在，请刷新后重新选择',
  EXERCISE_NOT_FOUND: '训练动作不存在，请刷新后重新选择',
  EQUIPMENT_NOT_FOUND: '器械不存在，请刷新后重新选择',
  PLAN_NOT_FOUND: '训练计划不存在，请刷新列表',
  PLAN_DAY_NOT_FOUND: '训练日不存在，请刷新训练计划',
  SESSION_NOT_FOUND: '训练记录不存在，请刷新列表',
  FOOD_NOT_FOUND: '食物不存在，请刷新后重新选择',
  MEAL_PLAN_NOT_FOUND: '饮食计划不存在，请刷新列表',
  MEAL_PLAN_ITEM_NOT_FOUND: '饮食计划中的食物项不存在，请刷新计划',
  MALFORMED_REQUEST: '请求内容格式不正确，请检查后重试',
  INVALID_REQUEST: '请求参数格式不正确，请检查后重试',
  METHOD_NOT_ALLOWED: '请求方式不受支持，请联系管理员',
  UNSUPPORTED_MEDIA_TYPE: '请求内容类型不受支持，请联系管理员',
  VALIDATION_ERROR: '请求参数校验失败',
  DATA_CONFLICT: '数据冲突：数据已存在或不满足保存约束，请刷新后检查',
  PLAN_DAY_MISMATCH: '所选训练日不属于当前训练计划，请重新选择',
  DUPLICATE_SET_INDEX: '同一动作的组序号不能重复，请检查训练记录',
  FOOD_UNIT_NOT_SUPPORTED: '该食物不支持所选单位，请重新选择',
  FOOD_ALLERGEN_CONFLICT: '所选食物与健康档案中的过敏原冲突，请更换食物',
  BUSINESS_RULE_VIOLATION: '不满足训练、营养或安全规则',
  CONFIRMATION_REQUIRED: '该操作需要先确认',
  IDEMPOTENCY_CONFLICT: '重复提交且请求内容不一致',
  AGENT_RUN_NOT_FOUND: 'Agent 运行记录不存在',
  AGENT_OUTPUT_INVALID: '模型输出无法解析，请重试',
  SAFETY_BLOCKED: '该请求存在健康风险，已被拦截',
  MODEL_UNAVAILABLE: '智能服务暂时不可用，请稍后重试',
  RATE_LIMITED: '操作过于频繁，请稍后再试',
  INTERNAL_ERROR: '服务开小差了，请稍后重试',
  // 请求层生成的本地错误码，不是后端业务错误码。
  NETWORK_ERROR: '网络异常或请求超时，请检查网络后重试',
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
