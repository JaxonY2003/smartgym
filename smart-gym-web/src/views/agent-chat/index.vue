<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Promotion } from '@element-plus/icons-vue'
import * as agentApi from '@/api/agent'
import { streamAgentMessage } from '@/utils/agent-stream'
import { errorMessageOf } from '@/constants/errorMessages'
import type {
  AgentConversation,
  AgentDraftType,
  DoneEvent,
  PlanPreviewEvent,
  ToolCallEvent,
} from '@/types/api/agent'

/* ==================== 会话管理 ==================== */

const conversations = ref<AgentConversation[]>([])
const currentConversationId = ref<string | null>(null)
const historyLoading = ref(false)

async function loadConversations() {
  try {
    const data = await agentApi.getConversations({ page: 1, pageSize: 50 })
    conversations.value = data.items
  } catch {
    conversations.value = []
  }
}

async function selectConversation(id: string) {
  if (streaming.value) {
    ElMessage.warning('当前对话正在进行中，请稍候切换')
    return
  }
  currentConversationId.value = id
  await loadHistory(id)
}

async function loadHistory(conversationId: string) {
  historyLoading.value = true
  messages.value = []
  try {
    const data = await agentApi.getMessages(conversationId, { page: 1, pageSize: 50 })
    for (const m of data.items) {
      if (m.role !== 'USER' && m.role !== 'ASSISTANT') continue
      messages.value.push({
        id: m.id,
        role: m.role,
        text: m.content,
        done: true,
      })
    }
  } catch {
    // 历史加载失败保持空列表
  } finally {
    historyLoading.value = false
  }
}

async function newConversation() {
  if (streaming.value) {
    ElMessage.warning('当前对话正在进行中，请稍候再开新会话')
    return
  }
  try {
    const conv = await agentApi.createConversation()
    conversations.value.unshift(conv)
    currentConversationId.value = conv.id
    messages.value = []
  } catch {
    // 拦截器已提示
  }
}

/* ==================== 聊天消息模型 ==================== */

interface ToolCallView {
  toolCallId: string
  tool: string
  status: 'running' | 'success' | 'failed'
  durationMs?: number
  summary?: string
}

interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  text: string
  done: boolean
  /** 流式过程中的展示数据 */
  statusMessage?: string
  toolCalls?: ToolCallView[]
  sources?: PlanPreviewEvent['draft'] extends never ? never : { documentId: number; title: string; version: string; relevance: number }[]
  /** 计划预览（写操作确认闭环） */
  preview?: {
    runId: string
    draftType: AgentDraftType
    warnings: string[]
    assumptions: string[]
    workoutPlanSummary?: string
    mealPlanSummary?: string
    expiresAt: string
  }
  /** 预览处置结果 */
  previewOutcome?: 'CONFIRMED' | 'REJECTED' | 'EXPIRED'
  /** 反馈 */
  rating?: number
  messageId?: string
}

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const streaming = ref(false)
let abortController: AbortController | null = null
const messagesEl = ref<HTMLDivElement>()

/** 本次流中最近的 runId（断线恢复用，文档 12.2） */
let lastRunId: string | null = null

function scrollToEnd() {
  nextTick(() => {
    if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  })
}

/* ==================== 发送与 SSE 消费 ==================== */

async function ensureConversation(): Promise<string | null> {
  if (currentConversationId.value) return currentConversationId.value
  try {
    const conv = await agentApi.createConversation()
    conversations.value.unshift(conv)
    currentConversationId.value = conv.id
    return conv.id
  } catch {
    return null
  }
}

async function sendMessage(text?: string) {
  const content = (text ?? inputText.value).trim()
  if (!content || streaming.value) return

  const conversationId = await ensureConversation()
  if (!conversationId) return

  messages.value.push({ id: 'local_' + Date.now(), role: 'USER', text: content, done: true })
  const assistantMsg: ChatMessage = {
    id: 'stream_' + Date.now(),
    role: 'ASSISTANT',
    text: '',
    done: false,
    toolCalls: [],
  }
  messages.value.push(assistantMsg)
  inputText.value = ''
  streaming.value = true
  scrollToEnd()

  abortController = new AbortController()
  try {
    const result = await streamAgentMessage(
      conversationId,
      {
        content,
        clientMessageId: 'msg_client_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8),
        context: {
          date: new Date().toISOString().slice(0, 10),
          timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        },
      },
      {
        onStatus: (e) => {
          lastRunId = e.runId
          assistantMsg.statusMessage = e.message
          scrollToEnd()
        },
        onToolCall: (e: ToolCallEvent) => {
          const list = assistantMsg.toolCalls ?? []
          const exist = list.find((t) => t.toolCallId === e.toolCallId)
          if (exist) Object.assign(exist, e)
          else list.push({ ...e })
          scrollToEnd()
        },
        onSource: (e) => {
          assistantMsg.sources = e.sources.map((s) => ({
            documentId: s.documentId,
            title: s.title,
            version: s.version,
            relevance: s.relevance,
          }))
          scrollToEnd()
        },
        onPlanPreview: (e) => {
          assistantMsg.preview = {
            runId: e.runId,
            draftType: e.draftType,
            warnings: e.draft.warnings,
            assumptions: e.draft.assumptions,
            workoutPlanSummary: summarizeDraft(e, 'workoutPlan'),
            mealPlanSummary: summarizeDraft(e, 'mealPlan'),
            expiresAt: e.expiresAt,
          }
          scrollToEnd()
        },
        onToken: (t) => {
          assistantMsg.text += t
          scrollToEnd()
        },
        onError: (e) => {
          assistantMsg.text += assistantMsg.text ? '\n\n' : ''
          assistantMsg.text += `⚠️ ${errorMessageOf(e.code)}`
          assistantMsg.done = true
          ElMessage.error(errorMessageOf(e.code))
        },
        onDone: (e: DoneEvent) => {
          lastRunId = e.runId
          assistantMsg.messageId = e.messageId
          assistantMsg.done = true
          assistantMsg.statusMessage = undefined
        },
      },
      abortController.signal,
    )

    assistantMsg.done = true

    // 流中断且未收到 done：按文档 12.2 先查最终状态再决定是否提示恢复
    if (!result.completed && lastRunId) {
      try {
        const run = await agentApi.getRunDetail(lastRunId)
        if (run.status === 'WAITING_CONFIRMATION') {
          assistantMsg.statusMessage = '连接中断，已从服务端恢复状态'
        } else {
          assistantMsg.text += (assistantMsg.text ? '\n\n' : '') + '⚠️ 回复中断，请重试'
        }
      } catch {
        assistantMsg.text += (assistantMsg.text ? '\n\n' : '') + '⚠️ 连接中断，请检查网络后重试'
      }
    }
  } catch (err) {
    const code = (err as { code?: string }).code
    assistantMsg.text += `⚠️ ${errorMessageOf(code)}`
    assistantMsg.done = true
  } finally {
    streaming.value = false
    assistantMsg.statusMessage = undefined
    abortController = null
    scrollToEnd()
  }
}

function summarizeDraft(e: PlanPreviewEvent, key: 'workoutPlan' | 'mealPlan'): string | undefined {
  const d = e.draft[key]
  if (!d) return undefined
  const name = (d as { name?: string }).name
  const meals = (d as { meals?: unknown[] }).meals
  const days = (d as { days?: unknown[] }).days
  const parts: string[] = []
  if (name) parts.push(String(name))
  if (days?.length) parts.push(`${days.length} 个训练日`)
  if (meals?.length) parts.push(`${meals.length} 餐`)
  return parts.join(' · ') || '已生成方案'
}

function stopStreaming() {
  abortController?.abort()
}

/* ==================== 计划确认闭环（8.5） ==================== */

const confirming = ref(false)

async function handleConfirm(msg: ChatMessage, accepted: boolean) {
  if (!msg.preview) return
  confirming.value = true
  try {
    const result = await agentApi.confirmRun(msg.preview.runId, {
      accepted,
      draftType: msg.preview.draftType,
    })
    msg.previewOutcome = accepted ? 'CONFIRMED' : 'REJECTED'
    if (accepted) {
      const created = result.createdResources.map((r) => r.type).join('、')
      ElMessage.success(`方案已保存（${created}），可在对应模块查看`)
    } else {
      ElMessage.info('已拒绝该方案，可继续对话让 AI 调整')
    }
  } catch {
    // 拦截器已提示；预览卡保持可操作
  } finally {
    confirming.value = false
  }
}

/* ==================== 消息反馈（8.6） ==================== */

const feedbackVisible = ref(false)
const feedbackTarget = ref<ChatMessage | null>(null)
const feedbackRating = ref(5)
const feedbackComment = ref('')
const feedbackSaving = ref(false)

function openFeedback(msg: ChatMessage) {
  feedbackTarget.value = msg
  feedbackRating.value = 5
  feedbackComment.value = ''
  feedbackVisible.value = true
}

async function submitFeedback() {
  if (!feedbackTarget.value?.messageId || !currentConversationId.value) return
  feedbackSaving.value = true
  try {
    await agentApi.submitFeedback(currentConversationId.value, {
      messageId: feedbackTarget.value.messageId,
      rating: feedbackRating.value,
      comment: feedbackComment.value || undefined,
    })
    feedbackTarget.value.rating = feedbackRating.value
    ElMessage.success('感谢你的反馈')
    feedbackVisible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    feedbackSaving.value = false
  }
}

/* ==================== 入口：dashboard 快捷提问 ==================== */

const route = useRoute()
const suggestedQuestions = [
  '我想减脂，帮我制定一份四周训练计划',
  '今天练腿，训练前应该吃什么？',
  '我早餐吃了两个鸡蛋和一杯牛奶，帮我记录',
]

const draftTypeLabels: Record<string, string> = {
  WORKOUT_PLAN: '训练计划',
  MEAL_PLAN: '饮食计划',
  WORKOUT_AND_MEAL_PLAN: '训练 + 饮食计划',
  MEAL_RECORD: '饮食记录',
}

onMounted(async () => {
  loadConversations()
  const q = route.query.q as string | undefined
  if (q) {
    // 从 dashboard 快捷提问跳转而来，自动发送
    await sendMessage(q)
  }
})

onBeforeUnmount(() => {
  abortController?.abort()
})
</script>

<template>
  <div class="agent-page">
    <!-- 会话侧栏 -->
    <aside class="conv-panel">
      <el-button type="primary" class="new-btn" @click="newConversation">新的咨询</el-button>
      <div class="conv-list">
        <button
          v-for="c in conversations"
          :key="c.id"
          class="conv-item"
          :class="{ active: c.id === currentConversationId }"
          @click="selectConversation(c.id)"
        >
          <span class="conv-title">{{ c.title }}</span>
        </button>
        <p v-if="conversations.length === 0" class="conv-empty">暂无历史会话</p>
      </div>
    </aside>

    <!-- 聊天主区 -->
    <main class="chat-panel">
      <div ref="messagesEl" class="messages" v-loading="historyLoading">
        <!-- 空状态：快捷提问 -->
        <div v-if="messages.length === 0 && !historyLoading" class="welcome">
          <div class="welcome-icon">💪</div>
          <h3>我是你的 AI 健康教练</h3>
          <p>可以帮你制定训练和饮食计划、解答健康饮食问题、记录每日饮食</p>
          <div class="suggest-list">
            <button v-for="q in suggestedQuestions" :key="q" class="suggest-item" @click="sendMessage(q)">
              {{ q }}
            </button>
          </div>
        </div>

        <template v-for="msg in messages" :key="msg.id">
          <!-- 用户消息 -->
          <div v-if="msg.role === 'USER'" class="bubble user">
            <div class="bubble-body">{{ msg.text }}</div>
          </div>

          <!-- AI 消息 -->
          <div v-else class="bubble assistant">
            <!-- 执行状态 -->
            <div v-if="!msg.done && msg.statusMessage" class="status-line">
              <span class="spinner"></span>{{ msg.statusMessage }}
            </div>

            <!-- 工具调用卡片 -->
            <div v-if="msg.toolCalls?.length" class="tool-calls">
              <div v-for="t in msg.toolCalls" :key="t.toolCallId" class="tool-chip">
                <span class="tool-dot" :class="t.status"></span>
                <span class="tool-name">{{ t.tool }}</span>
                <span v-if="t.summary" class="tool-summary">{{ t.summary }}</span>
                <span v-if="t.durationMs" class="tool-ms">{{ t.durationMs }}ms</span>
              </div>
            </div>

            <!-- 文本内容 -->
            <div v-if="msg.text" class="bubble-body">{{ msg.text }}</div>
            <div v-else-if="!msg.done && !msg.statusMessage" class="status-line">
              <span class="spinner"></span>正在思考…
            </div>

            <!-- 计划预览卡 -->
            <div v-if="msg.preview" class="preview-card" :class="{ settled: msg.previewOutcome }">
              <div class="preview-head">
                <b>📋 {{ draftTypeLabels[msg.preview.draftType] ?? msg.preview.draftType }}</b>
                <el-tag v-if="msg.previewOutcome === 'CONFIRMED'" type="success" size="small">已保存</el-tag>
                <el-tag v-else-if="msg.previewOutcome === 'REJECTED'" type="info" size="small">已拒绝</el-tag>
                <el-tag v-else type="warning" size="small">待确认</el-tag>
              </div>

              <div v-if="msg.preview.workoutPlanSummary" class="preview-line">
                🏋️ {{ msg.preview.workoutPlanSummary }}
              </div>
              <div v-if="msg.preview.mealPlanSummary" class="preview-line">
                🍱 {{ msg.preview.mealPlanSummary }}
              </div>

              <div v-if="msg.preview.warnings.length" class="preview-warnings">
                <div v-for="(w, i) in msg.preview.warnings" :key="i">⚠️ {{ w }}</div>
              </div>
              <div v-if="msg.preview.assumptions.length" class="preview-assumptions">
                <span v-for="(a, i) in msg.preview.assumptions" :key="i">· {{ a }}</span>
              </div>

              <!-- 文档 12.2：已收到 done 的运行不重复确认；确认后按钮组隐藏 -->
              <div v-if="!msg.previewOutcome" class="preview-actions">
                <el-button
                  type="primary"
                  size="small"
                  :loading="confirming"
                  @click="handleConfirm(msg, true)"
                >
                  确认保存
                </el-button>
                <el-button size="small" :loading="confirming" @click="handleConfirm(msg, false)">
                  拒绝 / 要求调整
                </el-button>
              </div>
            </div>

            <!-- 知识来源引用 -->
            <div v-if="msg.sources?.length" class="sources">
              <span class="sources-label">参考来源：</span>
              <span v-for="s in msg.sources" :key="s.documentId" class="source-chip">
                {{ s.title }}（{{ s.version }}，相关度 {{ Math.round(s.relevance * 100) }}%）
              </span>
            </div>

            <!-- 消息操作 -->
            <div v-if="msg.done && msg.messageId" class="msg-actions">
              <button class="action-btn" @click="openFeedback(msg)">
                {{ msg.rating ? `已评价 ${msg.rating}★` : '👍 评价' }}
              </button>
            </div>
          </div>
        </template>
      </div>

      <!-- 输入区 -->
      <div class="input-bar">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入你的问题，如：我想减脂，每周可以练四次，帮我安排训练和饮食"
          :disabled="streaming"
          @keydown.enter.exact.prevent="sendMessage()"
        />
        <el-button
          v-if="!streaming"
          type="primary"
          :icon="Promotion"
          :disabled="!inputText.trim()"
          @click="sendMessage()"
        >
          发送
        </el-button>
        <el-button v-else type="danger" plain @click="stopStreaming">停止</el-button>
      </div>
    </main>

    <!-- 反馈弹窗 -->
    <el-dialog v-model="feedbackVisible" title="评价这条回答" width="380px">
      <div class="rating-row">
        <el-rate v-model="feedbackRating" show-text />
      </div>
      <el-input
        v-model="feedbackComment"
        type="textarea"
        :rows="3"
        maxlength="200"
        placeholder="补充说明（选填）"
      />
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSaving" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.agent-page {
  display: flex;
  gap: 16px;
  height: calc(100vh - 60px - 40px);
  min-height: 480px;
}

/* 会话侧栏 */
.conv-panel {
  width: 220px;
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.new-btn {
  width: 100%;
}
.conv-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.conv-item {
  text-align: left;
  font-family: inherit;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: all 0.15s ease;
}
.conv-item:hover {
  border-color: var(--el-color-primary-light-5);
}
.conv-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.conv-title {
  font-size: 13px;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-empty {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: center;
  margin-top: 20px;
}

/* 聊天区 */
.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color);
  overflow: hidden;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 欢迎空态 */
.welcome {
  margin: auto;
  text-align: center;
  max-width: 420px;
}
.welcome-icon {
  font-size: 40px;
  margin-bottom: 8px;
}
.welcome h3 {
  margin: 0 0 8px;
}
.welcome p {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.suggest-item {
  font-family: inherit;
  font-size: 13px;
  padding: 11px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: all 0.15s ease;
}
.suggest-item:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

/* 气泡 */
.bubble {
  display: flex;
  flex-direction: column;
  max-width: 82%;
}
.bubble.user {
  align-self: flex-end;
}
.bubble.user .bubble-body {
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 14px 14px 4px 14px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.bubble.assistant {
  align-self: flex-start;
  width: 82%;
}
.bubble.assistant .bubble-body {
  background: var(--el-fill-color-light);
  border-radius: 14px 14px 14px 4px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 状态行 */
.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  padding: 4px 2px;
}
.spinner {
  width: 13px;
  height: 13px;
  border: 2px solid var(--el-color-primary-light-7);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex: none;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 工具调用 */
.tool-calls {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 8px;
}
.tool-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
}
.tool-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
  background: var(--el-color-warning);
}
.tool-dot.success { background: var(--el-color-success); }
.tool-dot.failed { background: var(--el-color-danger); }
.tool-name {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  color: var(--el-text-color-regular);
}
.tool-summary {
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tool-ms {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
}

/* 计划预览卡 */
.preview-card {
  margin-top: 10px;
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 12px;
  padding: 14px;
  background: var(--el-color-warning-light-9);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview-card.settled {
  border-color: var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}
.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
}
.preview-line {
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.preview-warnings {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--el-color-danger);
}
.preview-assumptions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.preview-actions {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

/* 来源引用 */
.sources {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}
.sources-label {
  color: var(--el-text-color-placeholder);
}
.source-chip {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-secondary);
}

/* 消息操作 */
.msg-actions {
  margin-top: 6px;
}
.action-btn {
  font-family: inherit;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 4px;
}
.action-btn:hover {
  color: var(--el-color-primary);
}

/* 输入区 */
.input-bar {
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 14px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}
.input-bar .el-textarea {
  flex: 1;
}

/* 反馈弹窗 */
.rating-row {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

@media (max-width: 768px) {
  .agent-page {
    flex-direction: column;
    height: auto;
  }
  .conv-panel {
    width: 100%;
    flex-direction: row;
    align-items: center;
  }
  .new-btn {
    width: auto;
    flex: none;
  }
  .conv-list {
    flex-direction: row;
    overflow-x: auto;
  }
  .conv-item {
    flex: none;
  }
  .chat-panel {
    height: 70vh;
  }
  .bubble.assistant {
    width: 100%;
  }
}
</style>
