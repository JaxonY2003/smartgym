import { tokenStorage } from './token'
import type {
  AgentErrorEvent,
  AgentStatusEvent,
  DoneEvent,
  PlanPreviewEvent,
  SendMessageRequest,
  SourceEvent,
  TokenEvent,
  ToolCallEvent,
} from '@/types/api/agent'

export interface AgentStreamHandlers {
  onStatus?: (e: AgentStatusEvent) => void
  onToolCall?: (e: ToolCallEvent) => void
  onSource?: (e: SourceEvent) => void
  onPlanPreview?: (e: PlanPreviewEvent) => void
  onToken?: (text: string) => void
  onError?: (e: AgentErrorEvent) => void
  onDone?: (e: DoneEvent) => void
}

/** 单条 SSE 事件解析结果 */
interface ParsedSseEvent {
  event: string
  data: string
}

/** 解析一段完整的 SSE 文本（event:/data: 行），chunk 边界已由调用方处理 */
function parseSseEvent(raw: string): ParsedSseEvent | null {
  let event = ''
  const dataLines: string[] = []
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
  }
  if (!event || dataLines.length === 0) return null
  return { event, data: dataLines.join('\n') }
}

function dispatch(parsed: ParsedSseEvent, handlers: AgentStreamHandlers): boolean {
  // 返回 false 表示流已终结（done / error），调用方可停止读取
  let payload: unknown
  try {
    payload = JSON.parse(parsed.data)
  } catch {
    return true // 无法解析的 data 跳过，不终止流
  }
  switch (parsed.event) {
    case 'status':
      handlers.onStatus?.(payload as AgentStatusEvent)
      return true
    case 'tool_call':
      handlers.onToolCall?.(payload as ToolCallEvent)
      return true
    case 'source':
      handlers.onSource?.(payload as SourceEvent)
      return true
    case 'plan_preview':
      handlers.onPlanPreview?.(payload as PlanPreviewEvent)
      return true
    case 'token':
      handlers.onToken?.((payload as TokenEvent).content)
      return true
    case 'error':
      handlers.onError?.(payload as AgentErrorEvent)
      return false
    case 'done':
      handlers.onDone?.(payload as DoneEvent)
      return false
    default:
      return true
  }
}

export interface AgentStreamResult {
  /** true 表示收到 done/error 事件正常终结；false 表示流中断，上层应走恢复策略（文档 12.2） */
  completed: boolean
}

/**
 * 发送 Agent 消息并消费 SSE 流（接口文档 8.3）。
 *
 * 为什么不用 EventSource：该接口为 POST 且需带 Authorization 头与 JSON body，
 * 原生 EventSource 只支持匿名 GET。
 */
export async function streamAgentMessage(
  conversationId: string,
  body: SendMessageRequest,
  handlers: AgentStreamHandlers,
  signal: AbortSignal,
): Promise<AgentStreamResult> {
  const res = await fetch(
    `${import.meta.env.VITE_API_BASE_URL}/agent/conversations/${conversationId}/messages`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: `Bearer ${tokenStorage.getAccessToken() ?? ''}`,
      },
      body: JSON.stringify(body),
      signal,
    },
  )

  // 流未建立时后端返回普通 JSON 错误信封
  if (!res.ok) {
    let code = 'NETWORK_ERROR'
    let message = '连接失败'
    try {
      const err = (await res.json()) as { code?: string; message?: string }
      code = err.code ?? code
      message = err.message ?? message
    } catch {
      /* 保持默认值 */
    }
    throw Object.assign(new Error(message), { code })
  }

  const reader = res.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let finished = false

  try {
    while (!finished) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 事件以空行分隔；chunk 边界可能切断事件，必须攒够再解析
      let sep: number
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const rawEvent = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        const parsed = parseSseEvent(rawEvent)
        if (parsed) finished = !dispatch(parsed, handlers)
      }
    }
  } finally {
    reader.cancel().catch(() => {})
  }

  // 流自然结束但从未收到 done/error：视为异常中断，交给上层走恢复策略
  return { completed: finished }
}
