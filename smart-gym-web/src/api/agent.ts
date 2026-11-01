/**
 * Agent 会话与运行 API（接口文档 §8）
 * SSE 发送消息走 utils/agent-stream.ts（fetch 流式读取），此处只封装普通 REST。
 */
import request from '@/utils/request'
import type { ApiEnvelope, PageData } from '@/types/api/common'
import type {
  AgentConversation,
  AgentMessage,
  AgentRunDetail,
  CreatedResource,
} from '@/types/api/agent'

/** 创建会话（8.1） */
export async function createConversation(title?: string): Promise<AgentConversation> {
  const { data: body } = await request.post<ApiEnvelope<AgentConversation>>(
    '/agent/conversations',
    { title: title || '新的咨询' },
  )
  return body.data
}

/** 分页查询会话（8.2） */
export async function getConversations(
  query: { page: number; pageSize: number },
): Promise<PageData<AgentConversation>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<AgentConversation>>>(
    '/agent/conversations',
    { params: query, silent: true },
  )
  return body.data
}

/** 查询会话内消息历史（8.2） */
export async function getMessages(
  conversationId: string,
  query: { page: number; pageSize: number },
): Promise<PageData<AgentMessage>> {
  const { data: body } = await request.get<ApiEnvelope<PageData<AgentMessage>>>(
    `/agent/conversations/${conversationId}/messages`,
    { params: query, silent: true },
  )
  return body.data
}

/** 查询 Agent 运行详情（8.4）。断线恢复时先调它确认最终状态（文档 12.2） */
export async function getRunDetail(runId: string): Promise<AgentRunDetail> {
  const { data: body } = await request.get<ApiEnvelope<AgentRunDetail>>(`/agent/runs/${runId}`, {
    silent: true,
  })
  return body.data
}

/** 确认或拒绝草稿（8.5）。必须携带 Idempotency-Key（拦截器自动注入） */
export async function confirmRun(
  runId: string,
  payload: { accepted: boolean; draftType: string; comment?: string },
): Promise<{ runId: string; status: string; createdResources: CreatedResource[] }> {
  const { data: body } = await request.post<
    ApiEnvelope<{ runId: string; status: string; createdResources: CreatedResource[] }>
  >(`/agent/runs/${runId}/confirm`, payload)
  return body.data
}

/** 消息反馈（8.6）。同一消息仅一次，幂等键由拦截器注入 */
export async function submitFeedback(
  conversationId: string,
  payload: { messageId: string; rating: number; tags?: string[]; comment?: string },
): Promise<void> {
  await request.post(`/agent/conversations/${conversationId}/feedback`, payload)
}
