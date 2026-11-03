<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormRules } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import * as workoutApi from '@/api/workout'
import * as exerciseApi from '@/api/exercise'
import {
  GOAL_LABELS,
  PLAN_SOURCE_LABELS,
  PLAN_STATUS_LABELS,
  PLAN_STATUS_TAG_TYPES,
  WEEKDAY_LABELS,
  toOptions,
} from '@/constants/dictionaries'
import { formatDate } from '@/utils/datetime'
import type { Exercise } from '@/types/api/exercise'
import type { FitnessGoal } from '@/types/api/member'
import type { PlanExercise, PlanExerciseInput, WorkoutPlan } from '@/types/api/workout'

/* ==================== 计划列表 ==================== */

const plans = ref<WorkoutPlan[]>([])
const listLoading = ref(false)
const statusFilter = ref<string | undefined>(undefined)
const PAGE_SIZE = 10

const statusOptions = [
  { label: '全部状态', value: '' },
  ...toOptions(PLAN_STATUS_LABELS),
]

async function loadPlans() {
  listLoading.value = true
  try {
    const data = await workoutApi.getWorkoutPlans({
      status: statusFilter.value || undefined,
      page: 1,
      pageSize: PAGE_SIZE,
    })
    plans.value = data.items
  } catch {
    plans.value = []
  } finally {
    listLoading.value = false
  }
}

async function handleActivate(plan: WorkoutPlan) {
  try {
    await ElMessageBox.confirm(
      `激活「${plan.name}」后，其他进行中的计划将被暂停。确定激活吗？`,
      '激活计划',
      { confirmButtonText: '激活', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  try {
    await workoutApi.activateWorkoutPlan(plan.id)
    ElMessage.success('计划已激活')
    await loadPlans()
  } catch {
    // 拦截器已提示
  }
}

async function handleTogglePause(plan: WorkoutPlan) {
  const pausing = plan.status === 'ACTIVE'
  try {
    await workoutApi.patchWorkoutPlan(plan.id, { status: pausing ? 'PAUSED' : 'ACTIVE' })
    ElMessage.success(pausing ? '计划已暂停' : '计划已恢复')
    await loadPlans()
  } catch {
    // 拦截器已提示
  }
}

async function handleDelete(plan: WorkoutPlan) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${plan.name}」吗？已产生训练记录的计划将归档处理。`,
      '删除计划',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await workoutApi.deleteWorkoutPlan(plan.id)
    ElMessage.success('计划已删除')
    await loadPlans()
  } catch {
    // 拦截器已提示
  }
}

/* ==================== 详情抽屉 ==================== */

const drawerVisible = ref(false)
const currentPlan = ref<WorkoutPlan | null>(null)

function openDetail(plan: WorkoutPlan) {
  currentPlan.value = plan
  drawerVisible.value = true
}

/* ==================== 新建向导 ==================== */

const wizardVisible = ref(false)
const wizardStep = ref(0)
const wizardSubmitting = ref(false)

const wizardForm = reactive({
  name: '',
  goal: 'FAT_LOSS' as FitnessGoal,
  durationWeeks: 4,
  trainingDaysPerWeek: 3,
  days: [] as {
    dayOfWeek: number
    title: string
    estimatedMinutes: number
    exercises: PlanExerciseInput[]
  }[],
})

const basicRules: FormRules = {
  name: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称为 2～50 个字符', trigger: 'blur' },
  ],
  durationWeeks: [
    { required: true, message: '请填写周期', trigger: 'blur' },
    { type: 'number', min: 1, max: 24, message: '周期为 1～24 周', trigger: 'blur' },
  ],
  trainingDaysPerWeek: [
    { required: true, message: '请选择训练频率', trigger: 'change' },
    { type: 'number', min: 1, max: 7, message: '每周 1～7 天', trigger: 'change' },
  ],
}

const goalOptions = toOptions(GOAL_LABELS)

/** 动作搜索缓存：向导里远程搜索 */
const exerciseOptions = ref<Exercise[]>([])
const exerciseSearching = ref(false)

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

/** 第一步确认时，按频率自动生成 N 个空训练日 */
function confirmBasic() {
  const n = wizardForm.trainingDaysPerWeek
  const usedDays = new Set(wizardForm.days.map((d) => d.dayOfWeek))
  // 补足缺少的天（默认周一/三/五/二/四/六/日顺序）
  const preferred = [1, 3, 5, 2, 4, 6, 7]
  for (const dow of preferred) {
    if (wizardForm.days.length >= n) break
    if (!usedDays.has(dow)) {
      wizardForm.days.push({
        dayOfWeek: dow,
        title: `训练日 ${wizardForm.days.length + 1}`,
        estimatedMinutes: 45,
        exercises: [],
      })
      usedDays.add(dow)
    }
  }
  // 多余的裁掉
  wizardForm.days = wizardForm.days.slice(0, n)
  wizardStep.value = 1
}

function addExercise(day: (typeof wizardForm.days)[number]) {
  day.exercises.push({
    exerciseId: undefined as unknown as number,
    sets: 3,
    targetRepsMin: 10,
    targetRepsMax: 12,
    restSeconds: 90,
    notes: '',
  })
}

function removeExercise(day: (typeof wizardForm.days)[number], index: number) {
  day.exercises.splice(index, 1)
}

const daysValid = computed(() =>
  wizardForm.days.length > 0 &&
  wizardForm.days.every(
    (d) =>
      d.title.trim() !== '' &&
      d.exercises.length > 0 &&
      d.exercises.every((e) => e.exerciseId && e.sets > 0 && e.targetRepsMin > 0 && e.targetRepsMax >= e.targetRepsMin),
  ),
)

function openWizard() {
  wizardStep.value = 0
  Object.assign(wizardForm, {
    name: '',
    goal: 'FAT_LOSS',
    durationWeeks: 4,
    trainingDaysPerWeek: 3,
    days: [],
  })
  searchExercises('')
  wizardVisible.value = true
}

async function submitWizard() {
  if (!daysValid.value) {
    ElMessage.warning('请为每个训练日填写名称，并至少安排一个有效动作')
    return
  }
  wizardSubmitting.value = true
  try {
    await workoutApi.createWorkoutPlan({
      name: wizardForm.name,
      goal: wizardForm.goal,
      durationWeeks: wizardForm.durationWeeks,
      trainingDaysPerWeek: wizardForm.trainingDaysPerWeek,
      days: wizardForm.days.map((d) => ({
        dayOfWeek: d.dayOfWeek,
        title: d.title,
        estimatedMinutes: d.estimatedMinutes,
        exercises: d.exercises.map((e) => ({
          exerciseId: e.exerciseId,
          sets: e.sets,
          targetRepsMin: e.targetRepsMin,
          targetRepsMax: e.targetRepsMax,
          restSeconds: e.restSeconds,
          notes: e.notes || undefined,
        })),
      })),
    })
    ElMessage.success('计划创建成功')
    wizardVisible.value = false
    await loadPlans()
  } catch {
    // 拦截器已提示
  } finally {
    wizardSubmitting.value = false
  }
}

onMounted(() => {
  loadPlans()
})
</script>

<template>
  <div class="plan-page">
    <header class="page-head">
      <div>
        <h2>训练计划</h2>
        <p>管理你的周期化训练安排，或让 AI 根据档案自动生成</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openWizard">新建计划</el-button>
    </header>

    <div class="filter-bar">
      <el-select v-model="statusFilter" class="status-select" @change="loadPlans">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
    </div>

    <div v-loading="listLoading" class="plan-list-wrap">
      <el-empty v-if="!listLoading && plans.length === 0" description="暂无训练计划，点击右上角新建或前往 AI 助理生成" />

      <div v-else class="plan-list">
        <el-card
          v-for="plan in plans"
          :key="plan.id"
          shadow="never"
          class="plan-card"
          :class="{ 'is-active': plan.status === 'ACTIVE' }"
        >
          <div class="plan-card-head">
            <div class="plan-title-block" role="button" tabindex="0" @click="openDetail(plan)" @keyup.enter="openDetail(plan)">
              <h3>{{ plan.name }}</h3>
              <div class="plan-meta">
                <el-tag :type="PLAN_STATUS_TAG_TYPES[plan.status] ?? 'info'" size="small">
                  {{ PLAN_STATUS_LABELS[plan.status] ?? plan.status }}
                </el-tag>
                <el-tag size="small" effect="plain" type="info">
                  {{ PLAN_SOURCE_LABELS[plan.source] ?? plan.source }}
                </el-tag>
                <span v-if="plan.goal" class="meta-text">{{ GOAL_LABELS[plan.goal] }}</span>
                <span class="meta-text">{{ plan.durationWeeks }} 周 · 每周 {{ plan.trainingDaysPerWeek }} 练</span>
                <span class="meta-text">创建于 {{ formatDate(plan.createdAt) }}</span>
              </div>
            </div>
            <div class="plan-actions">
              <el-button
                v-if="plan.status !== 'ACTIVE'"
                size="small"
                type="success"
                plain
                @click="handleActivate(plan)"
              >
                激活
              </el-button>
              <el-button
                v-if="plan.status === 'ACTIVE' || plan.status === 'PAUSED'"
                size="small"
                plain
                @click="handleTogglePause(plan)"
              >
                {{ plan.status === 'ACTIVE' ? '暂停' : '恢复' }}
              </el-button>
              <el-button size="small" type="danger" plain :icon="Delete" @click="handleDelete(plan)" />
            </div>
          </div>

          <div v-if="plan.warnings?.length" class="plan-warnings">
            <el-alert
              v-for="(w, i) in plan.warnings"
              :key="i"
              type="warning"
              :title="w"
              :closable="false"
              show-icon
            />
          </div>

          <div class="day-chips">
            <span v-for="d in plan.days" :key="d.id" class="day-chip">
              <b>{{ WEEKDAY_LABELS[d.dayOfWeek] ?? d.dayOfWeek }}</b>
              {{ d.title }} · {{ d.exercises.length }} 个动作
            </span>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="currentPlan?.name ?? '计划详情'" size="480px">
      <div v-if="currentPlan" class="detail">
        <div class="detail-meta">
          <el-tag :type="PLAN_STATUS_TAG_TYPES[currentPlan.status] ?? 'info'">
            {{ PLAN_STATUS_LABELS[currentPlan.status] ?? currentPlan.status }}
          </el-tag>
          <span v-if="currentPlan.goal">{{ GOAL_LABELS[currentPlan.goal] }}</span>
          <span>{{ currentPlan.durationWeeks }} 周</span>
          <span>每周 {{ currentPlan.trainingDaysPerWeek }} 练</span>
        </div>

        <el-alert
          v-for="(w, i) in currentPlan.warnings"
          :key="i"
          type="warning"
          :title="w"
          :closable="false"
          show-icon
          class="detail-warning"
        />

        <section v-for="day in currentPlan.days" :key="day.id" class="day-section">
          <div class="day-head">
            <h4>{{ WEEKDAY_LABELS[day.dayOfWeek] }} · {{ day.title }}</h4>
            <span v-if="day.estimatedMinutes">约 {{ day.estimatedMinutes }} 分钟</span>
          </div>
          <el-table :data="day.exercises" size="small" border>
            <el-table-column prop="exerciseName" label="动作" min-width="110" />
            <el-table-column prop="sets" label="组数" width="55" align="center" />
            <el-table-column label="次数" width="80" align="center">
              <template #default="{ row }: { row: PlanExercise }">
                {{ row.targetReps ?? (row.targetRepsMin && row.targetRepsMax ? `${row.targetRepsMin}-${row.targetRepsMax}` : '—') }}
              </template>
            </el-table-column>
            <el-table-column label="休息" width="70" align="center">
              <template #default="{ row }: { row: PlanExercise }">
                {{ row.restSeconds ? `${row.restSeconds}s` : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="notes" label="备注" min-width="100" show-overflow-tooltip>
              <template #default="{ row }: { row: PlanExercise }">
                {{ row.notes ?? '—' }}
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </el-drawer>

    <!-- 新建向导 -->
    <el-dialog
      v-model="wizardVisible"
      title="新建训练计划"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-steps :active="wizardStep" simple>
        <el-step title="基本信息" />
        <el-step title="训练日安排" />
      </el-steps>

      <!-- 第一步：基本信息 -->
      <div v-if="wizardStep === 0" class="wizard-step">
        <el-form ref="basicFormRef" :model="wizardForm" :rules="basicRules" label-position="top">
          <el-form-item label="计划名称" prop="name">
            <el-input v-model="wizardForm.name" maxlength="50" placeholder="如：减脂四日训练计划" />
          </el-form-item>
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="训练目标">
                <el-select v-model="wizardForm.goal" style="width: 100%">
                  <el-option v-for="o in goalOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="周期（周）" prop="durationWeeks">
                <el-input-number v-model="wizardForm.durationWeeks" :min="1" :max="24" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="每周训练天数" prop="trainingDaysPerWeek">
                <el-input-number v-model="wizardForm.trainingDaysPerWeek" :min="1" :max="7" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 第二步：训练日 -->
      <div v-else class="wizard-step">
        <p class="wizard-tip">每个训练日至少安排一个动作；次数为区间（如 10～12 次）</p>
        <div v-for="(day, di) in wizardForm.days" :key="di" class="wizard-day">
          <div class="wizard-day-head">
            <el-select v-model="day.dayOfWeek" class="dow-select" size="small">
              <el-option v-for="(label, value) in WEEKDAY_LABELS" :key="value" :label="label" :value="Number(value)" />
            </el-select>
            <el-input v-model="day.title" size="small" class="day-title" maxlength="30" placeholder="训练日标题，如：腿部训练" />
            <el-input-number v-model="day.estimatedMinutes" size="small" :min="15" :max="180" :step="5" controls-position="right" class="day-minutes" />
            <span class="minutes-label">分钟</span>
            <el-button size="small" type="primary" plain :icon="Plus" @click="addExercise(day)">动作</el-button>
          </div>

          <div v-for="(ex, ei) in day.exercises" :key="ei" class="wizard-ex-row">
            <el-select
              v-model="ex.exerciseId"
              filterable
              remote
              :remote-method="searchExercises"
              :loading="exerciseSearching"
              placeholder="搜索并选择动作"
              class="ex-select"
              size="small"
            >
              <el-option v-for="e in exerciseOptions" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-input-number v-model="ex.sets" size="small" :min="1" :max="10" controls-position="right" class="num" />
            <span class="num-label">组</span>
            <el-input-number v-model="ex.targetRepsMin" size="small" :min="1" :max="50" controls-position="right" class="num" />
            <span class="num-label">～</span>
            <el-input-number v-model="ex.targetRepsMax" size="small" :min="1" :max="50" controls-position="right" class="num" />
            <span class="num-label">次</span>
            <el-input-number v-model="ex.restSeconds" size="small" :min="15" :max="300" :step="15" controls-position="right" class="num" />
            <span class="num-label">s 休</span>
            <el-button size="small" type="danger" plain :icon="Delete" circle @click="removeExercise(day, ei)" />
          </div>
          <p v-if="day.exercises.length === 0" class="day-empty">尚未安排动作</p>
        </div>
      </div>

      <template #footer>
        <el-button v-if="wizardStep === 1" @click="wizardStep = 0">上一步</el-button>
        <el-button v-if="wizardStep === 0" type="primary" @click="confirmBasic">下一步</el-button>
        <el-button v-else type="primary" :loading="wizardSubmitting" :disabled="!daysValid" @click="submitWizard">
          创建计划
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.plan-page {
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

.filter-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.status-select {
  width: 140px;
}

.plan-list-wrap {
  min-height: 200px;
}

.plan-card {
  margin-bottom: 14px;
  border: 1px solid var(--el-border-color-lighter);
  transition: border-color 0.2s ease;
}
.plan-card.is-active {
  border-color: var(--el-color-success-light-5);
}

.plan-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.plan-title-block {
  cursor: pointer;
  min-width: 0;
  flex: 1;
}
.plan-title-block h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}
.plan-title-block:hover h3 {
  color: var(--el-color-primary);
}

.plan-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}
.meta-text {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.plan-actions {
  display: flex;
  gap: 0;
  flex: none;
}

.plan-warnings {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}
.day-chip {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
}
.day-chip b {
  color: var(--el-color-primary);
  font-weight: 600;
  margin-right: 4px;
}

/* 详情抽屉 */
.detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.detail-warning {
  margin-top: -4px;
}
.day-section .day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.day-section h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}
.day-section > span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.day-section {
  margin-bottom: 4px;
}

/* 新建向导 */
.wizard-step {
  margin-top: 20px;
}
.wizard-tip {
  margin: 0 0 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.wizard-day {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 12px;
}
.wizard-day-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.dow-select {
  width: 90px;
}
.day-title {
  flex: 1;
  min-width: 140px;
}
.day-minutes {
  width: 100px;
}
.minutes-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.wizard-ex-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.ex-select {
  width: 180px;
}
.num {
  width: 84px;
}
.num-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.day-empty {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

@media (max-width: 640px) {
  .plan-card-head {
    flex-direction: column;
  }
  .plan-actions {
    align-self: flex-end;
  }
  .ex-select {
    width: 100%;
  }
}
</style>
