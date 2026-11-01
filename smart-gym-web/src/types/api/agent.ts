import type { PageData } from './common'

/** Agent 会话（接口文档 8.1 / 8.2） */
export interface AgentConversation {
  id: string
  title: string
  status: 'ACTIVE' | 'ARCHIVED'
  createdAt: string
}

export interface AgentMessage {
  id: string
  conversationId: string
  role: 'USER' | 'ASSISTANT' | 'SYSTEM' | 'TOOL'
  content: string
  createdAt: string
}

/** 发送消息请求（接口文档 8.3） */
export interface SendMessageRequest {
  content: string
  clientMessageId: string
  context?: {
    date: string
    timezone: string
  }
}

/** Agent 运行状态（接口文档 8.4） */
export type AgentRunStatus =
  | 'RUNNING'
  | 'COMPLETED'
  | 'WAITING_CONFIRMATION'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'FAILED'

export type AgentDraftType =
  | 'WORKOUT_PLAN'
  | 'MEAL_PLAN'
  | 'WORKOUT_AND_MEAL_PLAN'
  | 'MEAL_RECORD'

/** 知识来源引用（source 事件 / run 详情） */
export interface KnowledgeSourceRef {
  documentId: number
  title: string
  version: string
  sourceUrl: string | null
  relevance: number
}

export interface AgentRunStep {
  node: string
  status: 'SUCCESS' | 'FAILED' | 'RUNNING'
  startedAt: string
  durationMs: number
}

export interface AgentRunDetail {
  runId: string
  conversationId: string
  status: AgentRunStatus
  intent: string
  steps: AgentRunStep[]
  draftType: AgentDraftType | null
  draft: Record<string, unknown>
  createdAt: string
}

export interface CreatedResource {
  type: 'WORKOUT_PLAN' | 'MEAL_PLAN'
  id: number
}

/** ---- SSE 事件负载（接口文档 8.3），与事件名一一对应 ---- */
export interface AgentStatusEvent {
  runId: string
  node: string
  message: string
}

export interface ToolCallEvent {
  runId: string
  toolCallId: string
  tool: string
  status: 'running' | 'success' | 'failed'
  durationMs?: number
  summary?: string
}

export interface SourceEvent {
  runId: string
  sources: KnowledgeSourceRef[]
}

export interface PlanPreviewEvent {
  runId: string
  draftType: AgentDraftType
  requiresConfirmation: boolean
  draft: {
    workoutPlan?: Record<string, unknown>
    mealPlan?: Record<string, unknown>
    warnings: string[]
    assumptions: string[]
  }
  expiresAt: string
}

export interface TokenEvent {
  runId: string
  content: string
}

export interface AgentErrorEvent {
  runId: string
  code: string
  message: string
}

export interface DoneEvent {
  runId: string
  messageId: string
  status: AgentRunStatus
  usage: {
    inputTokens: number
    outputTokens: number
  }
}

/** 分页消息列表复用统一分页结构 */
export type AgentMessagePage = PageData<AgentMessage>
