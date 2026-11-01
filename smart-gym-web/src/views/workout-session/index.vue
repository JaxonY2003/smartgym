<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as workoutApi from '@/api/workout'
import * as exerciseApi from '@/api/exercise'
import {
  BODY_PART_LABELS,
  SESSION_STATUS_LABELS,
  WEEKDAY_LABELS,
  toOptions,
} from '@/constants/dictionaries'
import { formatDateTime, toLocalIso } from '@/utils/datetime'
import type { Exercise } from '@/types/api/exercise'
import type { WorkoutSession } from '@/types/api/workout'

/* ==================== 今日训练 ==================== */

const todayView = ref<workoutApi.WorkoutPlanDayView | null>(null)
const todayLoading = ref(false)
const todayDate = new Date().toISOString().slice(0, 10)

/** 录入中的组：按动作分组展开 */
interface EntryRow {
  exerciseId: number
  exerciseName: string
  targetSets: number
  targetReps: string
  restSeconds: number | null
  setEntries: { weightKg: number | null; reps: number | null; completed: boolean }[]
}

const entryRows = ref<EntryRow[]>([])
const sessionSaving = ref(false)
const fatigueLevel = ref(5)
const painLevel = ref(0)
const sessionNote = ref('')

async function loadToday() {
  todayLoading.value = true
  try {
    const data = await workoutApi.getTodayPlan()
    todayView.value = data
    entryRows.value = (data?.exercises ?? []).map((e) => ({
      exerciseId: e.exerciseId,
      exerciseName: e.exerciseName,
      targetSets: e.sets,
      targetReps: e.targetReps,
      restSeconds: e.restSeconds,
      setEntries: Array.from({ length: e.sets }, () => ({
        weightKg: null,
        reps: null,
        completed: false,
      })),
    }))
  } catch {
    todayView.value = null
    entryRows.value = []
  } finally {
    todayLoading.value = false
  }
}

const weekdayLabel = computed(() => {
  const dow = new Date().getDay()
  return WEEKDAY_LABELS[dow === 0 ? 7 : dow] ?? ''
})

/** 完成度：已勾选的组 / 总组数 */
const completion = computed(() => {
  const total = entryRows.value.reduce((s, r) => s + r.setEntries.length, 0)
  const done = entryRows.value.reduce(
    (s, r) => s + r.setEntries.filter((e) => e.completed).length,
    0,
  )
  return { total, done, rate: total === 0 ? 0 : done / total }
})

/** 总容量 = Σ 重量 × 次数（仅已完成的组） */
const totalVolume = computed(() =>
  entryRows.value.reduce(
    (sum, r) =>
      sum +
      r.setEntries.reduce(
        (s, e) => s + (e.completed && e.weightKg && e.reps ? e.weightKg * e.reps : 0),
        0,
      ),
    0,
  ),
)

async function saveSession() {
  const hasAnySet = entryRows.value.some((r) => r.setEntries.some((e) => e.completed))
  if (!hasAnySet) {
    ElMessage.warning('请至少勾选完成一组训练')
    return
  }
  sessionSaving.value = true
  const now = new Date()
  try {
    await workoutApi.createWorkoutSession({
      id: 0,
      planId: todayView.value?.planId ?? null,
      planDayId: todayView.value?.dayId ?? null,
      startedAt: toLocalIso(now),
      endedAt: toLocalIso(now),
      status: 'COMPLETED',
      sets: entryRows.value.flatMap((r) =>
        r.setEntries
          .filter((e) => e.completed)
          .map((e, i) => ({
            exerciseId: r.exerciseId,
            setIndex: i + 1,
            weightKg: e.weightKg,
            reps: e.reps,
            completed: true,
            restSeconds: r.restSeconds,
          })),
      ),
      fatigueLevel: fatigueLevel.value,
      painBodyPart: painLevel.value > 0 ? 'OTHER' : null,
      painLevel: painLevel.value,
      note: sessionNote.value || null,
    })
    ElMessage.success('训练记录已保存')
    sessionNote.value = ''
    await loadToday()
    await loadHistory()
  } catch {
    // 拦截器已提示
  } finally {
    sessionSaving.value = false
  }
}

/* ==================== 自由训练（无计划） ==================== */

const freeDialogVisible = ref(false)
const freeFormRef = ref<FormInstance>()
const freeSaving = ref(false)
const exerciseOptions = ref<Exercise[]>([])
const exerciseSearching = ref(false)

const freeForm = reactive({
  exerciseId: undefined as number | undefined,
  exerciseName: '',
  sets: 3,
  entries: [
    { weightKg: null as number | null, reps: null as number | null, completed: true },
    { weightKg: null as number | null, reps: null as number | null, completed: true },
    { weightKg: null as number | null, reps: null as number | null, completed: true },
  ],
  fatigueLevel: 5,
  note: '',
})

const freeRules: FormRules = {
  exerciseId: [{ required: true, message: '请选择动作', trigger: 'change' }],
}

async function searchExercises(keyword: string) {
  exerciseSearching.value = true
  try {
    const data = await exerciseApi.getExercises({ keyword: keyword || undefined, page: 1, pageSize: 20 })
    exerciseOptions.value = data.items
  } catch {
    exerciseOptions.value = []
  } finally {
    exerciseSearching.value = false
  }
}

function syncFreeSets() {
  const n = freeForm.sets
  while (freeForm.entries.length < n)
    freeForm.entries.push({ weightKg: null, reps: null, completed: true })
  freeForm.entries = freeForm.entries.slice(0, n)
}

function onFreeExerciseChange(id: number | undefined) {
  freeForm.exerciseName = exerciseOptions.value.find((e) => e.id === id)?.name ?? ''
}

function openFreeDialog() {
  Object.assign(freeForm, {
    exerciseId: undefined,
    exerciseName: '',
    sets: 3,
    entries: [
      { weightKg: null, reps: null, completed: true },
      { weightKg: null, reps: null, completed: true },
      { weightKg: null, reps: null, completed: true },
    ],
    fatigueLevel: 5,
    note: '',
  })
  searchExercises('')
  freeDialogVisible.value = true
}

async function submitFreeSession() {
  const valid = await freeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  freeSaving.value = true
  const now = new Date()
  try {
    await workoutApi.createWorkoutSession({
      id: 0,
      planId: null,
      planDayId: null,
      startedAt: toLocalIso(now),
      endedAt: toLocalIso(now),
      status: 'COMPLETED',
      sets: freeForm.entries
        .filter((e) => e.completed)
        .map((e, i) => ({
          exerciseId: freeForm.exerciseId!,
          setIndex: i + 1,
          weightKg: e.weightKg,
          reps: e.reps,
          completed: true,
          restSeconds: null,
        })),
      fatigueLevel: freeForm.fatigueLevel,
      painBodyPart: null,
      painLevel: 0,
      note: freeForm.note || null,
    })
    ElMessage.success('自由训练已记录')
    freeDialogVisible.value = false
    await loadHistory()
  } catch {
    // 拦截器已提示
  } finally {
    freeSaving.value = false
  }
}

/* ==================== 历史记录 ==================== */

const sessions = ref<WorkoutSession[]>([])
const historyLoading = ref(false)
const historyTotal = ref(0)
const historyPage = ref(1)
const HISTORY_PAGE_SIZE = 10

async function loadHistory() {
  historyLoading.value = true
  try {
    const data = await workoutApi.getWorkoutSessions({
      page: historyPage.value,
      pageSize: HISTORY_PAGE_SIZE,
    })
    sessions.value = data.items
    historyTotal.value = data.total
  } catch {
    sessions.value = []
    historyTotal.value = 0
  } finally {
    historyLoading.value = false
  }
}

function sessionVolume(s: WorkoutSession): number {
  return (s.sets ?? []).reduce(
    (sum, e) => sum + (e.completed && e.weightKg && e.reps ? e.weightKg * e.reps : 0),
    0,
  )
}

/* ==================== 训练反馈弹窗 ==================== */

const feedbackVisible = ref(false)
const feedbackSaving = ref(false)
const feedbackSessionId = ref<number>(0)
const feedbackFormRef = ref<FormInstance>()

const feedbackForm = reactive({
  fatigueLevel: 5,
  painBodyPart: 'KNEE' as string,
  painLevel: 0,
  feeling: '',
  shouldAdjustNextPlan: false,
})

const feedbackRules: FormRules = {
  feeling: [{ required: true, message: '请描述训练感受', trigger: 'blur' }],
}

const bodyPartOptions = toOptions(BODY_PART_LABELS)

function openFeedback(session: WorkoutSession) {
  feedbackSessionId.value = session.id
  Object.assign(feedbackForm, {
    fatigueLevel: session.fatigueLevel ?? 5,
    painBodyPart: 'KNEE',
    painLevel: 0,
    feeling: '',
    shouldAdjustNextPlan: false,
  })
  feedbackVisible.value = true
}

async function submitFeedback() {
  const valid = await feedbackFormRef.value?.validate().catch(() => false)
  if (!valid) return
  feedbackSaving.value = true
  try {
    await workoutApi.submitWorkoutFeedback(feedbackSessionId.value, {
      fatigueLevel: feedbackForm.fatigueLevel,
      painBodyPart: feedbackForm.painLevel > 0 ? feedbackForm.painBodyPart : null,
      painLevel: feedbackForm.painLevel,
      feeling: feedbackForm.feeling,
      shouldAdjustNextPlan: feedbackForm.shouldAdjustNextPlan,
    })
    ElMessage.success('反馈已提交')
    feedbackVisible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    feedbackSaving.value = false
  }
}

onMounted(() => {
  loadToday()
  loadHistory()
})
</script>

<template>
  <div class="session-page">
    <header class="page-head">
      <div>
        <h2>训练记录</h2>
        <p>执行今日训练并记录每组数据，AI 将根据完成情况调整后续计划</p>
      </div>
      <el-button :icon="Plus" @click="openFreeDialog">记一笔自由训练</el-button>
    </header>

    <!-- 今日训练 -->
    <el-card v-loading="todayLoading" shadow="never" class="today-card">
      <template #header>
        <div class="card-head">
          <span>今日训练 · {{ weekdayLabel }} {{ todayDate }}</span>
          <div v-if="todayView" class="head-meta">
            <el-tag type="primary" effect="plain" size="small">{{ todayView.planName }}</el-tag>
            <span class="meta-text">{{ todayView.title }}</span>
            <span v-if="todayView.estimatedMinutes" class="meta-text">
              约 {{ todayView.estimatedMinutes }} 分钟
            </span>
          </div>
        </div>
      </template>

      <el-empty
        v-if="!todayLoading && !todayView"
        description="今天没有安排训练。可以休息，或点击右上角记一笔自由训练"
        :image-size="72"
      />

      <template v-else-if="todayView">
        <!-- 完成度统计 -->
        <div class="stats-bar">
          <div class="stat">
            <span class="stat-num">{{ completion.done }}/{{ completion.total }}</span>
            <span class="stat-label">已完成组数</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ Math.round(completion.rate * 100) }}%</span>
            <span class="stat-label">完成度</span>
          </div>
          <div class="stat">
            <span class="stat-num">{{ totalVolume > 0 ? Math.round(totalVolume) : '—' }}</span>
            <span class="stat-label">训练容量（kg）</span>
          </div>
          <el-progress
            :percentage="Math.round(completion.rate * 100)"
            :stroke-width="10"
            class="stats-progress"
          />
        </div>

        <!-- 逐动作录入 -->
        <div v-for="(row, ri) in entryRows" :key="ri" class="entry-block">
          <div class="entry-head">
            <b>{{ row.exerciseName }}</b>
            <span class="entry-target">{{ row.targetSets }} 组 × {{ row.targetReps }} 次</span>
            <span v-if="row.restSeconds" class="entry-target">组间休息 {{ row.restSeconds }}s</span>
          </div>
          <div v-for="(entry, si) in row.setEntries" :key="si" class="set-row">
            <el-checkbox v-model="entry.completed">第 {{ si + 1 }} 组</el-checkbox>
            <el-input-number
              v-model="entry.weightKg"
              :min="0"
              :max="500"
              :step="2.5"
              :precision="1"
              size="small"
              controls-position="right"
              placeholder="重量"
              :disabled="!entry.completed"
            />
            <span class="unit">kg ×</span>
            <el-input-number
              v-model="entry.reps"
              :min="0"
              :max="100"
              size="small"
              controls-position="right"
              placeholder="次数"
              :disabled="!entry.completed"
            />
            <span class="unit">次</span>
          </div>
        </div>

        <!-- 整体感受 -->
        <div class="feeling-bar">
          <div class="feeling-item">
            <span class="feeling-label">疲劳程度（1～10）</span>
            <el-slider v-model="fatigueLevel" :min="1" :max="10" class="feeling-slider" />
            <span class="feeling-value">{{ fatigueLevel }}</span>
          </div>
          <div class="feeling-item">
            <span class="feeling-label">疼痛程度（0～10，0 为无痛）</span>
            <el-slider v-model="painLevel" :min="0" :max="10" class="feeling-slider" />
            <span class="feeling-value">{{ painLevel }}</span>
          </div>
          <el-input
            v-model="sessionNote"
            maxlength="100"
            placeholder="整体感受备注（选填）"
            class="feeling-note"
          />
        </div>

        <div class="save-bar">
          <el-button type="primary" :loading="sessionSaving" @click="saveSession">
            保存今日训练记录
          </el-button>
        </div>
      </template>
    </el-card>

    <!-- 历史记录 -->
    <el-card v-loading="historyLoading" shadow="never" class="history-card">
      <template #header>
        <div class="card-head"><span>历史训练记录</span></div>
      </template>

      <el-table :data="sessions" stripe>
        <el-table-column label="时间" min-width="140">
          <template #default="{ row }: { row: WorkoutSession }">
            {{ formatDateTime(row.startedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="动作数" width="80" align="center">
          <template #default="{ row }: { row: WorkoutSession }">
            {{ new Set(row.sets.map((s) => s.exerciseId)).size }}
          </template>
        </el-table-column>
        <el-table-column label="总组数" width="80" align="center">
          <template #default="{ row }: { row: WorkoutSession }">{{ row.sets.length }}</template>
        </el-table-column>
        <el-table-column label="容量（kg）" width="100" align="right">
          <template #default="{ row }: { row: WorkoutSession }">
            {{ sessionVolume(row) > 0 ? Math.round(sessionVolume(row)) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: WorkoutSession }">
            <el-tag size="small" :type="row.status === 'COMPLETED' ? 'success' : 'info'">
              {{ SESSION_STATUS_LABELS[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="疲劳" width="70" align="center">
          <template #default="{ row }: { row: WorkoutSession }">
            {{ row.fatigueLevel ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }: { row: WorkoutSession }">{{ row.note ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }: { row: WorkoutSession }">
            <el-button size="small" text type="primary" @click="openFeedback(row)">
              反馈
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无历史记录，完成今日训练后自动生成" :image-size="72" />
        </template>
      </el-table>

      <div v-if="historyTotal > HISTORY_PAGE_SIZE" class="pager">
        <el-pagination
          v-model:current-page="historyPage"
          :page-size="HISTORY_PAGE_SIZE"
          :total="historyTotal"
          layout="total, prev, pager, next"
          background
          @current-change="loadHistory"
        />
      </div>
    </el-card>

    <!-- 自由训练弹窗 -->
    <el-dialog
      v-model="freeDialogVisible"
      title="记录自由训练"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="freeFormRef" :model="freeForm" :rules="freeRules" label-position="top">
        <el-form-item label="动作" prop="exerciseId">
          <el-select
            v-model="freeForm.exerciseId"
            filterable
            remote
            :remote-method="searchExercises"
            :loading="exerciseSearching"
            placeholder="搜索并选择动作"
            style="width: 100%"
            @change="onFreeExerciseChange"
          >
            <el-option v-for="e in exerciseOptions" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="组数">
          <el-input-number
            v-model="freeForm.sets"
            :min="1"
            :max="10"
            @change="syncFreeSets"
            style="width: 140px"
          />
        </el-form-item>
        <div v-for="(e, i) in freeForm.entries" :key="i" class="set-row">
          <el-checkbox v-model="e.completed">第 {{ i + 1 }} 组</el-checkbox>
          <el-input-number
            v-model="e.weightKg"
            :min="0"
            :max="500"
            :step="2.5"
            :precision="1"
            size="small"
            controls-position="right"
          />
          <span class="unit">kg ×</span>
          <el-input-number
            v-model="e.reps"
            :min="0"
            :max="100"
            size="small"
            controls-position="right"
          />
          <span class="unit">次</span>
        </div>
        <el-form-item label="疲劳程度（1～10）">
          <el-slider v-model="freeForm.fatigueLevel" :min="1" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="备注（选填）">
          <el-input v-model="freeForm.note" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="freeSaving" @click="submitFreeSession">保存</el-button>
      </template>
    </el-dialog>

    <!-- 训练反馈弹窗 -->
    <el-dialog
      v-model="feedbackVisible"
      title="训练反馈"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="feedbackFormRef" :model="feedbackForm" :rules="feedbackRules" label-position="top">
        <el-form-item label="疲劳程度（1～10）">
          <el-slider v-model="feedbackForm.fatigueLevel" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="疼痛部位与程度">
          <div class="pain-row">
            <el-select v-model="feedbackForm.painBodyPart" :disabled="feedbackForm.painLevel === 0">
              <el-option v-for="o in bodyPartOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-slider
              v-model="feedbackForm.painLevel"
              :min="0"
              :max="10"
              class="pain-slider"
            />
            <span class="feeling-value">{{ feedbackForm.painLevel }}</span>
          </div>
        </el-form-item>
        <el-form-item label="训练感受" prop="feeling">
          <el-input
            v-model="feedbackForm.feeling"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="如：右膝在下蹲时轻微不适，整体状态不错"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="feedbackForm.shouldAdjustNextPlan">
            希望下一阶段计划据此调整
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackSaving" @click="submitFeedback">提交反馈</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.session-page {
  max-width: 1080px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-head h2 {
  margin: 0 0 4px;
  font-size: 22px;
}
.page-head p {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  flex-wrap: wrap;
  gap: 8px;
}
.head-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 400;
}
.meta-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.today-card {
  margin-bottom: 16px;
}

/* 完成度统计 */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.stats-progress {
  flex: 1;
  min-width: 140px;
}

/* 逐动作录入 */
.entry-block {
  margin-bottom: 18px;
}
.entry-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}
.entry-target {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.set-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.set-row .el-checkbox {
  width: 84px;
}
.set-row .el-input-number {
  width: 110px;
}
.unit {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

/* 感受 */
.feeling-bar {
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.feeling-item {
  display: flex;
  align-items: center;
  gap: 14px;
}
.feeling-label {
  width: 200px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  flex: none;
}
.feeling-slider {
  flex: 1;
}
.feeling-value {
  width: 24px;
  text-align: center;
  font-weight: 600;
  color: var(--el-color-primary);
}
.feeling-note {
  max-width: 420px;
}

.save-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

/* 历史 */
.history-card {
  margin-bottom: 16px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 反馈弹窗 */
.pain-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}
.pain-row .el-select {
  width: 120px;
  flex: none;
}
.pain-slider {
  flex: 1;
}

@media (max-width: 640px) {
  .feeling-label {
    width: 100%;
  }
  .feeling-item {
    flex-wrap: wrap;
  }
  .stats-bar {
    gap: 20px;
  }
}
</style>
