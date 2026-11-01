<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import * as memberApi from '@/api/member'
import {
  ALLERGEN_LABELS,
  BODY_PART_LABELS,
  CONSTRAINT_TYPE_LABELS,
  DIET_TYPE_LABELS,
  EXPERIENCE_LABELS,
  GENDER_LABELS,
  GOAL_LABELS,
  RESTRICTION_LABELS,
  SEVERITY_LABELS,
  toOptions,
} from '@/constants/dictionaries'
import { formatDateTime, toLocalIso } from '@/utils/datetime'
import type {
  AllergenCode,
  BodyMetric,
  DietType,
  DietaryRestriction,
  FitnessGoal,
  Gender,
  HealthConstraint,
  TrainingExperience,
} from '@/types/api/member'

/* ==================== 基础档案 ==================== */

const profileFormRef = ref<FormInstance>()
const profileLoading = ref(false)
const profileSaving = ref(false)
const profileExists = ref(false)

const profileForm = reactive({
  gender: 'UNKNOWN' as Gender,
  birthDate: '',
  heightCm: 170,
  weightKg: 65,
  trainingExperience: 'BEGINNER' as TrainingExperience,
  weeklyTrainingDays: 3,
  maxSessionMinutes: 60,
  goals: [] as FitnessGoal[],
})

const profileRules: FormRules = {
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  heightCm: [
    { required: true, message: '请输入身高', trigger: 'blur' },
    { type: 'number', min: 50, max: 250, message: '身高范围为 50～250 cm', trigger: 'blur' },
  ],
  weightKg: [
    { required: true, message: '请输入体重', trigger: 'blur' },
    { type: 'number', min: 25, max: 300, message: '体重范围为 25～300 kg', trigger: 'blur' },
  ],
  weeklyTrainingDays: [
    { required: true, message: '请选择每周训练天数', trigger: 'change' },
    { type: 'number', min: 1, max: 7, message: '每周 1～7 天', trigger: 'change' },
  ],
  maxSessionMinutes: [
    { required: true, message: '请输入单次训练时长', trigger: 'blur' },
    { type: 'number', min: 15, max: 240, message: '单次 15～240 分钟', trigger: 'blur' },
  ],
  goals: [
    { type: 'array', required: true, min: 1, message: '至少选择一个训练目标', trigger: 'change' },
  ],
}

const goalOptions = toOptions(GOAL_LABELS)
const genderOptions = toOptions(GENDER_LABELS)
const experienceOptions = toOptions(EXPERIENCE_LABELS)
const dayOptions = Array.from({ length: 7 }, (_, i) => ({ label: `${i + 1} 天`, value: i + 1 }))

async function loadProfile() {
  profileLoading.value = true
  try {
    const data = await memberApi.getProfile()
    profileExists.value = true
    Object.assign(profileForm, {
      gender: data.gender ?? 'UNKNOWN',
      birthDate: data.birthDate ?? '',
      heightCm: data.heightCm ?? 170,
      weightKg: data.weightKg ?? 65,
      trainingExperience: data.trainingExperience ?? 'BEGINNER',
      weeklyTrainingDays: data.weeklyTrainingDays ?? 3,
      maxSessionMinutes: data.maxSessionMinutes ?? 60,
      goals: data.goals ?? [],
    })
  } catch {
    profileExists.value = false
  } finally {
    profileLoading.value = false
  }
}

async function saveProfile() {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  profileSaving.value = true
  try {
    await memberApi.updateProfile({
      gender: profileForm.gender,
      birthDate: profileForm.birthDate,
      heightCm: profileForm.heightCm,
      weightKg: profileForm.weightKg,
      trainingExperience: profileForm.trainingExperience,
      weeklyTrainingDays: profileForm.weeklyTrainingDays,
      maxSessionMinutes: profileForm.maxSessionMinutes,
      goals: profileForm.goals,
    })
    profileExists.value = true
    ElMessage.success('健康档案已保存')
  } catch {
    // 拦截器已弹出错误提示
  } finally {
    profileSaving.value = false
  }
}

/* ==================== 身体指标 ==================== */

const metrics = ref<BodyMetric[]>([])
const metricsLoading = ref(false)
const metricsTotal = ref(0)
const metricsPage = ref(1)
const METRICS_PAGE_SIZE = 10

const metricDialogVisible = ref(false)
const metricFormRef = ref<FormInstance>()
const metricSaving = ref(false)

const metricForm = reactive({
  measuredAt: '',
  weightKg: null as number | null,
  bodyFatPercent: null as number | null,
  waistCm: null as number | null,
  note: '',
})

const metricRules: FormRules = {
  measuredAt: [{ required: true, message: '请选择测量时间', trigger: 'change' }],
  weightKg: [
    { required: true, message: '请输入体重', trigger: 'blur' },
    { type: 'number', min: 25, max: 300, message: '体重范围为 25～300 kg', trigger: 'blur' },
  ],
  bodyFatPercent: [{ type: 'number', min: 1, max: 60, message: '体脂率范围为 1～60 %', trigger: 'blur' }],
  waistCm: [{ type: 'number', min: 40, max: 200, message: '腰围范围为 40～200 cm', trigger: 'blur' }],
}

async function loadMetrics() {
  metricsLoading.value = true
  try {
    const data = await memberApi.getBodyMetrics({
      page: metricsPage.value,
      pageSize: METRICS_PAGE_SIZE,
    })
    metrics.value = data.items
    metricsTotal.value = data.total
  } catch {
    metrics.value = []
    metricsTotal.value = 0
  } finally {
    metricsLoading.value = false
  }
}

function openMetricDialog() {
  metricForm.measuredAt = ''
  metricForm.weightKg = profileForm.weightKg ?? null
  metricForm.bodyFatPercent = null
  metricForm.waistCm = null
  metricForm.note = ''
  metricDialogVisible.value = true
}

async function submitMetric() {
  const valid = await metricFormRef.value?.validate().catch(() => false)
  if (!valid) return
  metricSaving.value = true
  try {
    await memberApi.createBodyMetric({
      measuredAt: toLocalIso(new Date(metricForm.measuredAt)),
      weightKg: metricForm.weightKg!,
      bodyFatPercent: metricForm.bodyFatPercent,
      waistCm: metricForm.waistCm,
      note: metricForm.note || undefined,
    })
    ElMessage.success('记录已添加')
    metricDialogVisible.value = false
    metricsPage.value = 1
    await loadMetrics()
  } catch {
    // 拦截器已提示
  } finally {
    metricSaving.value = false
  }
}

/* ==================== 健康限制 ==================== */

const constraintsLoading = ref(false)
const constraintsSaving = ref(false)

const constraintsForm = reactive({
  constraints: [] as HealthConstraint[],
  medicalNotice: '',
})

const constraintTypeOptions = toOptions(CONSTRAINT_TYPE_LABELS)
const severityOptions = toOptions(SEVERITY_LABELS)
const bodyPartOptions = toOptions(BODY_PART_LABELS)

function addConstraint() {
  constraintsForm.constraints.push({
    type: 'PAIN',
    bodyPart: 'KNEE',
    severity: 'MILD',
    description: '',
    active: true,
  })
}

function removeConstraint(index: number) {
  constraintsForm.constraints.splice(index, 1)
}

async function loadConstraints() {
  constraintsLoading.value = true
  try {
    const data = await memberApi.getHealthConstraints()
    constraintsForm.constraints = data.constraints ?? []
    constraintsForm.medicalNotice = data.medicalNotice ?? ''
  } catch {
    constraintsForm.constraints = []
    constraintsForm.medicalNotice = ''
  } finally {
    constraintsLoading.value = false
  }
}

async function saveConstraints() {
  constraintsSaving.value = true
  try {
    await memberApi.updateHealthConstraints({
      constraints: constraintsForm.constraints,
      medicalNotice: constraintsForm.medicalNotice || null,
    })
    ElMessage.success('健康限制已保存')
  } catch {
    // 拦截器已提示
  } finally {
    constraintsSaving.value = false
  }
}

/* ==================== 饮食档案 ==================== */

const nutritionLoading = ref(false)
const nutritionSaving = ref(false)
const nutritionFormRef = ref<FormInstance>()

const nutritionForm = reactive({
  dietType: 'OMNIVORE' as DietType,
  allergies: [] as AllergenCode[],
  restrictions: [] as DietaryRestriction[],
  dislikedFoodsText: '',
  preferredCuisinesText: '',
  dailyWaterMlTarget: 2000,
})

const nutritionRules: FormRules = {
  dailyWaterMlTarget: [
    { required: true, message: '请输入每日饮水目标', trigger: 'blur' },
    { type: 'number', min: 500, max: 5000, message: '饮水目标为 500～5000 ml', trigger: 'blur' },
  ],
}

const dietTypeOptions = toOptions(DIET_TYPE_LABELS)
const allergenOptions = toOptions(ALLERGEN_LABELS)
const restrictionOptions = toOptions(RESTRICTION_LABELS)

async function loadNutrition() {
  nutritionLoading.value = true
  try {
    const data = await memberApi.getNutritionProfile()
    nutritionForm.dietType = data.dietType ?? 'OMNIVORE'
    nutritionForm.allergies = data.allergies ?? []
    nutritionForm.restrictions = data.restrictions ?? []
    nutritionForm.dislikedFoodsText = (data.dislikedFoods ?? []).join('、')
    nutritionForm.preferredCuisinesText = (data.preferredCuisines ?? []).join('、')
    nutritionForm.dailyWaterMlTarget = data.dailyWaterMlTarget ?? 2000
  } catch {
    // 保留默认值供首次填写
  } finally {
    nutritionLoading.value = false
  }
}

async function saveNutrition() {
  const valid = await nutritionFormRef.value?.validate().catch(() => false)
  if (!valid) return
  nutritionSaving.value = true
  try {
    await memberApi.updateNutritionProfile({
      dietType: nutritionForm.dietType,
      allergies: nutritionForm.allergies,
      restrictions: nutritionForm.restrictions,
      dislikedFoods: splitByDelimiter(nutritionForm.dislikedFoodsText),
      preferredCuisines: splitByDelimiter(nutritionForm.preferredCuisinesText),
      dailyWaterMlTarget: nutritionForm.dailyWaterMlTarget,
    })
    ElMessage.success('饮食档案已保存')
  } catch {
    // 拦截器已提示
  } finally {
    nutritionSaving.value = false
  }
}

function splitByDelimiter(text: string): string[] {
  return text
    .split(/[,，、\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
}

/* ==================== 页面骨架 ==================== */

const activeTab = ref('basic')

/** 首次切换到某 tab 时才加载其数据 */
const loadedTabs = new Set<string>(['basic'])

function handleTabChange(name: string | number) {
  const key = String(name)
  if (loadedTabs.has(key)) return
  loadedTabs.add(key)
  if (key === 'metrics') loadMetrics()
  else if (key === 'constraints') loadConstraints()
  else if (key === 'nutrition') loadNutrition()
}

const profileTitle = computed(() => (profileExists.value ? '编辑健康档案' : '建立健康档案'))

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="profile-page">
    <header class="page-head">
      <div>
        <h2>健康档案</h2>
        <p>档案数据将作为 AI 教练生成个性化训练与饮食方案的依据</p>
      </div>
    </header>

    <el-alert
      class="privacy-alert"
      type="info"
      :closable="false"
      show-icon
      title="健康限制等敏感信息仅用于安全判断，不会被 AI 用作医疗诊断依据"
    />

    <el-tabs v-model="activeTab" class="profile-tabs" @tab-change="handleTabChange">
      <!-- 基础档案 -->
      <el-tab-pane label="基础档案" name="basic">
        <el-card v-loading="profileLoading" shadow="never" class="section-card">
          <template #header>
            <div class="card-head">
              <span>{{ profileTitle }}</span>
              <el-tag v-if="profileExists" type="success" size="small">已建档</el-tag>
            </div>
          </template>

          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-position="top"
          >
            <el-row :gutter="24">
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="性别" prop="gender">
                  <el-select v-model="profileForm.gender" style="width: 100%">
                    <el-option
                      v-for="o in genderOptions"
                      :key="o.value"
                      :label="o.label"
                      :value="o.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="出生日期" prop="birthDate">
                  <el-date-picker
                    v-model="profileForm.birthDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="选择日期"
                    :disabled-date="(d: Date) => d.getTime() > Date.now()"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="训练经验" prop="trainingExperience">
                  <el-select v-model="profileForm.trainingExperience" style="width: 100%">
                    <el-option
                      v-for="o in experienceOptions"
                      :key="o.value"
                      :label="o.label"
                      :value="o.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="身高（cm）" prop="heightCm">
                  <el-input-number
                    v-model="profileForm.heightCm"
                    :min="50"
                    :max="250"
                    :step="1"
                    :precision="1"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="体重（kg）" prop="weightKg">
                  <el-input-number
                    v-model="profileForm.weightKg"
                    :min="25"
                    :max="300"
                    :step="0.5"
                    :precision="1"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="训练目标（可多选）" prop="goals">
                  <el-select v-model="profileForm.goals" multiple style="width: 100%">
                    <el-option
                      v-for="o in goalOptions"
                      :key="o.value"
                      :label="o.label"
                      :value="o.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="每周训练天数" prop="weeklyTrainingDays">
                  <el-select v-model="profileForm.weeklyTrainingDays" style="width: 100%">
                    <el-option
                      v-for="o in dayOptions"
                      :key="o.value"
                      :label="o.label"
                      :value="o.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="单次最长训练时长（分钟）" prop="maxSessionMinutes">
                  <el-input-number
                    v-model="profileForm.maxSessionMinutes"
                    :min="15"
                    :max="240"
                    :step="5"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <div class="form-actions">
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">
                {{ profileExists ? '保存修改' : '建立档案' }}
              </el-button>
            </div>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 身体指标 -->
      <el-tab-pane label="身体指标" name="metrics" lazy>
        <el-card v-loading="metricsLoading" shadow="never" class="section-card">
          <template #header>
            <div class="card-head">
              <span>身体指标记录</span>
              <el-button type="primary" size="small" :icon="Plus" @click="openMetricDialog">
                添加记录
              </el-button>
            </div>
          </template>

          <el-table :data="metrics" stripe>
            <el-table-column label="测量时间" min-width="150">
              <template #default="{ row }">{{ formatDateTime(row.measuredAt) }}</template>
            </el-table-column>
            <el-table-column prop="weightKg" label="体重（kg）" width="110" align="right" />
            <el-table-column label="体脂率（%）" width="110" align="right">
              <template #default="{ row }">{{ row.bodyFatPercent ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="腰围（cm）" width="110" align="right">
              <template #default="{ row }">{{ row.waistCm ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="note" label="备注" min-width="140" show-overflow-tooltip />
            <template #empty>
              <el-empty description="暂无记录，点击右上角添加第一条身体指标" :image-size="72" />
            </template>
          </el-table>

          <div v-if="metricsTotal > METRICS_PAGE_SIZE" class="pager">
            <el-pagination
              v-model:current-page="metricsPage"
              :page-size="METRICS_PAGE_SIZE"
              :total="metricsTotal"
              layout="total, prev, pager, next"
              background
              @current-change="loadMetrics"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 健康限制 -->
      <el-tab-pane label="健康限制" name="constraints" lazy>
        <el-card v-loading="constraintsLoading" shadow="never" class="section-card">
          <template #header>
            <div class="card-head">
              <span>身体限制与伤病情况</span>
              <el-button size="small" :icon="Plus" @click="addConstraint">添加限制</el-button>
            </div>
          </template>

          <el-form label-position="top">
            <el-empty
              v-if="constraintsForm.constraints.length === 0"
              description="没有身体限制记录。如有疼痛、伤病或特殊情况，请添加，AI 将据此调整训练方案"
              :image-size="72"
            />

            <div
              v-for="(c, index) in constraintsForm.constraints"
              :key="index"
              class="constraint-row"
            >
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12" :md="5">
                  <el-form-item label="类型">
                    <el-select v-model="c.type" style="width: 100%">
                      <el-option
                        v-for="o in constraintTypeOptions"
                        :key="o.value"
                        :label="o.label"
                        :value="o.value"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12" :md="5">
                  <el-form-item label="部位">
                    <el-select v-model="c.bodyPart" style="width: 100%">
                      <el-option
                        v-for="o in bodyPartOptions"
                        :key="o.value"
                        :label="o.label"
                        :value="o.value"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12" :md="5">
                  <el-form-item label="严重程度">
                    <el-select v-model="c.severity" style="width: 100%">
                      <el-option
                        v-for="o in severityOptions"
                        :key="o.value"
                        :label="o.label"
                        :value="o.value"
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="7">
                  <el-form-item label="说明">
                    <el-input
                      v-model="c.description"
                      maxlength="100"
                      placeholder="如：深蹲时膝盖不适"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="12" :sm="6" :md="1">
                  <el-form-item label="生效">
                    <el-switch v-model="c.active" />
                  </el-form-item>
                </el-col>
                <el-col :xs="12" :sm="6" :md="1" class="remove-col">
                  <el-button
                    type="danger"
                    :icon="Delete"
                    circle
                    plain
                    size="small"
                    @click="removeConstraint(index)"
                  />
                </el-col>
              </el-row>
            </div>

            <el-form-item label="医疗情况说明（选填）">
              <el-input
                v-model="constraintsForm.medicalNotice"
                type="textarea"
                :rows="2"
                maxlength="200"
                show-word-limit
                placeholder="如已知慢性疾病、手术史等。该信息仅用于安全判断"
              />
            </el-form-item>

            <div class="form-actions">
              <el-button type="primary" :loading="constraintsSaving" @click="saveConstraints">
                保存健康限制
              </el-button>
            </div>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- 饮食档案 -->
      <el-tab-pane label="饮食档案" name="nutrition" lazy>
        <el-card v-loading="nutritionLoading" shadow="never" class="section-card">
          <template #header>
            <div class="card-head"><span>饮食偏好与过敏信息</span></div>
          </template>

          <el-alert
            type="warning"
            :closable="false"
            show-icon
            class="allergy-alert"
            title="过敏原信息至关重要：AI 生成饮食方案时会严格排除相关食物"
          />

          <el-form
            ref="nutritionFormRef"
            :model="nutritionForm"
            :rules="nutritionRules"
            label-position="top"
          >
            <el-row :gutter="24">
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="饮食类型" prop="dietType">
                  <el-select v-model="nutritionForm.dietType" style="width: 100%">
                    <el-option
                      v-for="o in dietTypeOptions"
                      :key="o.value"
                      :label="o.label"
                      :value="o.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8">
                <el-form-item label="每日饮水目标（ml）" prop="dailyWaterMlTarget">
                  <el-input-number
                    v-model="nutritionForm.dailyWaterMlTarget"
                    :min="500"
                    :max="5000"
                    :step="100"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="食物过敏（可多选）" prop="allergies">
              <el-select v-model="nutritionForm.allergies" multiple style="width: 100%">
                <el-option
                  v-for="o in allergenOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="饮食限制（可多选）" prop="restrictions">
              <el-select v-model="nutritionForm.restrictions" multiple style="width: 100%">
                <el-option
                  v-for="o in restrictionOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>

            <el-row :gutter="24">
              <el-col :xs="24" :md="12">
                <el-form-item label="不喜欢的食物（用顿号、逗号或空格分隔）">
                  <el-input
                    v-model="nutritionForm.dislikedFoodsText"
                    placeholder="如：香菜、动物内脏、芹菜"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="偏好的菜系（用顿号、逗号或空格分隔）">
                  <el-input
                    v-model="nutritionForm.preferredCuisinesText"
                    placeholder="如：家常菜、粤菜、日料"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <div class="form-actions">
              <el-button type="primary" :loading="nutritionSaving" @click="saveNutrition">
                保存饮食档案
              </el-button>
            </div>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加身体指标对话框 -->
    <el-dialog
      v-model="metricDialogVisible"
      title="添加身体指标记录"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="metricFormRef" :model="metricForm" :rules="metricRules" label-position="top">
        <el-form-item label="测量时间" prop="measuredAt">
          <el-date-picker
            v-model="metricForm.measuredAt"
            type="datetime"
            placeholder="选择时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="体重（kg）" prop="weightKg">
          <el-input-number
            v-model="metricForm.weightKg"
            :min="25"
            :max="300"
            :step="0.1"
            :precision="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="体脂率（%）" prop="bodyFatPercent">
              <el-input-number
                v-model="metricForm.bodyFatPercent"
                :min="1"
                :max="60"
                :step="0.1"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="腰围（cm）" prop="waistCm">
              <el-input-number
                v-model="metricForm.waistCm"
                :min="40"
                :max="200"
                :step="0.5"
                :precision="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="metricForm.note" maxlength="50" placeholder="如：晨起空腹" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metricDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="metricSaving" @click="submitMetric">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
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

.privacy-alert {
  margin-bottom: 16px;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.section-card {
  margin-bottom: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.constraint-row {
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 12px;
  background: var(--el-fill-color-blank);
}
.constraint-row:hover {
  border-color: var(--el-color-primary-light-5);
}
.remove-col {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.allergy-alert {
  margin-bottom: 20px;
}
</style>
