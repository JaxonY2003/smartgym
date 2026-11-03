<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import * as exerciseApi from '@/api/exercise'
import ExerciseImageFrame from '@/components/ExerciseImageFrame.vue'
import {
  DIFFICULTY_LABELS,
  DIFFICULTY_TAG_TYPES,
  EQUIPMENT_CATEGORY_LABELS,
  EQUIPMENT_STATUS_LABELS,
  EQUIPMENT_STATUS_TAG_TYPES,
  MUSCLE_GROUP_LABELS,
} from '@/constants/dictionaries'
import type { Equipment, Exercise, MuscleGroup, MuscleGroupCode } from '@/types/api/exercise'

/* ==================== 动作库 ==================== */

const exercises = ref<Exercise[]>([])
const listLoading = ref(false)
const total = ref(0)
const PAGE_SIZE = 12

const filters = reactive({
  keyword: '',
  muscleGroup: undefined as MuscleGroupCode | undefined,
  page: 1,
})

const muscleGroups = ref<MuscleGroup[]>([])
const muscleOptions = computed(() => {
  const source = muscleGroups.value.length
    ? muscleGroups.value
    : Object.entries(MUSCLE_GROUP_LABELS).map(([code, name]) => ({ code, name }))
  return source.map((group) => ({ label: group.name, value: group.code }))
})
const muscleLabelMap = computed<Record<string, string>>(() => ({
  ...MUSCLE_GROUP_LABELS,
  ...Object.fromEntries(muscleGroups.value.map((group) => [group.code, group.name])),
}))

async function loadMuscleGroups() {
  try {
    const data = await exerciseApi.getMuscleGroups({ page: 1, pageSize: 100 })
    muscleGroups.value = data.items
  } catch {
    muscleGroups.value = []
  }
}

/** 输入防抖：停止输入 400ms 后再查询 */
let keywordTimer: ReturnType<typeof setTimeout> | null = null
function onKeywordInput() {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    filters.page = 1
    loadExercises()
  }, 400)
}

function onFilterChange() {
  filters.page = 1
  loadExercises()
}

async function loadExercises() {
  listLoading.value = true
  try {
    const data = await exerciseApi.getExercises({
      keyword: filters.keyword || undefined,
      muscleGroup: filters.muscleGroup,
      page: filters.page,
      pageSize: PAGE_SIZE,
    })
    exercises.value = data.items
    total.value = data.total
  } catch {
    exercises.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

/* ==================== 详情抽屉 ==================== */

const drawerVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<Exercise | null>(null)

async function openDetail(ex: Exercise) {
  currentDetail.value = ex
  drawerVisible.value = true
  detailLoading.value = true
  try {
    // 以列表数据先行渲染，再拉取详情覆盖（详情可能含更多字段）
    currentDetail.value = await exerciseApi.getExercise(ex.id)
  } catch {
    // 保留列表数据展示
  } finally {
    detailLoading.value = false
  }
}

/* ==================== 器械 ==================== */

const equipmentList = ref<Equipment[]>([])
const equipmentLoading = ref(false)
const equipmentTotal = ref(0)
const equipmentPage = ref(1)
const EQUIPMENT_PAGE_SIZE = 20

async function loadEquipment() {
  equipmentLoading.value = true
  try {
    const data = await exerciseApi.getEquipment({
      page: equipmentPage.value,
      pageSize: EQUIPMENT_PAGE_SIZE,
    })
    equipmentList.value = data.items
    equipmentTotal.value = data.total
  } catch {
    equipmentList.value = []
    equipmentTotal.value = 0
  } finally {
    equipmentLoading.value = false
  }
}

/* ==================== 页面骨架 ==================== */

const activeTab = ref('exercises')
const loadedTabs = new Set<string>(['exercises'])

function handleTabChange(name: string | number) {
  const key = String(name)
  if (loadedTabs.has(key)) return
  loadedTabs.add(key)
  if (key === 'equipment') loadEquipment()
}

const hasFilter = computed(
  () => filters.keyword.trim() !== '' || filters.muscleGroup !== undefined,
)

onMounted(() => {
  loadMuscleGroups()
  loadExercises()
})
</script>

<template>
  <div class="exercises-page">
    <header class="page-head">
      <div>
        <h2>动作库</h2>
        <p>浏览训练动作与健身器械，了解目标肌群和注意事项</p>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="page-tabs" @tab-change="handleTabChange">
      <!-- 动作库 -->
      <el-tab-pane label="训练动作" name="exercises">
        <div class="filter-bar">
          <el-input
            v-model="filters.keyword"
            class="keyword-input"
            placeholder="搜索动作名称，如：深蹲"
            clearable
            :prefix-icon="Search"
            @input="onKeywordInput"
            @clear="onFilterChange"
          />
          <el-select
            v-model="filters.muscleGroup"
            class="muscle-select"
            placeholder="目标肌群"
            clearable
            @change="onFilterChange"
          >
            <el-option
              v-for="o in muscleOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </div>

        <div v-loading="listLoading" class="grid-wrap">
          <div v-if="!listLoading && exercises.length === 0" class="empty-block">
            <el-empty :description="hasFilter ? '没有匹配的动作，试试其他关键词或肌群' : '动作库暂无数据'" />
          </div>

          <div v-else class="card-grid">
            <article
              v-for="ex in exercises"
              :key="ex.id"
              class="exercise-card"
              tabindex="0"
              role="button"
              :aria-label="`查看 ${ex.name} 详情`"
              @click="openDetail(ex)"
              @keyup.enter="openDetail(ex)"
            >
              <ExerciseImageFrame
                :src="ex.thumbnailUrl"
                :alt="ex.name"
                ratio="4 / 3"
                radius="10px 10px 0 0"
              />
              <div class="card-body">
                <div class="card-title-row">
                  <h3>{{ ex.name }}</h3>
                  <el-tag :type="DIFFICULTY_TAG_TYPES[ex.difficulty]" size="small" effect="light">
                    {{ DIFFICULTY_LABELS[ex.difficulty] }}
                  </el-tag>
                </div>
                <p v-if="ex.englishName" class="english-name">{{ ex.englishName }}</p>
                <div class="muscle-tags">
                  <el-tag
                    v-for="m in ex.primaryMuscles"
                    :key="m"
                    size="small"
                    type="primary"
                    effect="plain"
                  >
                    {{ muscleLabelMap[m] ?? m }}
                  </el-tag>
                </div>
                <div v-if="ex.equipment.length" class="equipment-line">
                  <span
                    v-for="(e, i) in ex.equipment.slice(0, 3)"
                    :key="e.id"
                    class="equipment-item"
                  >
                    {{ e.name }}<template v-if="i < Math.min(ex.equipment.length, 3) - 1">、</template>
                  </span>
                  <span v-if="ex.equipment.length > 3" class="equipment-more">
                    等 {{ ex.equipment.length }} 项
                  </span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="total > PAGE_SIZE" class="pager">
            <el-pagination
              v-model:current-page="filters.page"
              :page-size="PAGE_SIZE"
              :total="total"
              layout="total, prev, pager, next"
              background
              @current-change="loadExercises"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 器械 -->
      <el-tab-pane label="健身器械" name="equipment" lazy>
        <el-card v-loading="equipmentLoading" shadow="never">
          <el-table :data="equipmentList" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="name" label="器械名称" min-width="140" />
            <el-table-column label="类别" width="120">
              <template #default="{ row }: { row: Equipment }">
                {{ EQUIPMENT_CATEGORY_LABELS[row.category] ?? row.category }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }: { row: Equipment }">
                <el-tag :type="EQUIPMENT_STATUS_TAG_TYPES[row.status]" size="small">
                  {{ EQUIPMENT_STATUS_LABELS[row.status] ?? row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="brand" label="品牌" min-width="130" show-overflow-tooltip />
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <template #empty>
              <el-empty description="暂无器械数据" :image-size="72" />
            </template>
          </el-table>

          <div v-if="equipmentTotal > EQUIPMENT_PAGE_SIZE" class="pager">
            <el-pagination
              v-model:current-page="equipmentPage"
              :page-size="EQUIPMENT_PAGE_SIZE"
              :total="equipmentTotal"
              layout="total, prev, pager, next"
              background
              @current-change="loadEquipment"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 动作详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="currentDetail?.name ?? '动作详情'"
      size="400px"
      :with-header="true"
    >
      <div v-if="currentDetail" v-loading="detailLoading" class="detail">
        <ExerciseImageFrame
          :src="currentDetail.thumbnailUrl"
          :alt="currentDetail.name"
          ratio="16 / 10"
          radius="10px"
        />

        <div class="detail-section">
          <div class="detail-title-row">
            <h3>{{ currentDetail.name }}</h3>
            <el-tag :type="DIFFICULTY_TAG_TYPES[currentDetail.difficulty]" effect="light">
              {{ DIFFICULTY_LABELS[currentDetail.difficulty] }}
            </el-tag>
          </div>
          <p v-if="currentDetail.englishName" class="english-name">
            {{ currentDetail.englishName }}
          </p>
          <p v-if="currentDetail.description" class="detail-desc">
            {{ currentDetail.description }}
          </p>
        </div>

        <div v-if="currentDetail.instructions" class="detail-section">
          <h4>动作步骤</h4>
          <p class="detail-instructions">{{ currentDetail.instructions }}</p>
        </div>

        <div class="detail-section">
          <h4>目标肌群</h4>
          <div class="muscle-tags">
            <el-tag
              v-for="m in currentDetail.primaryMuscles"
              :key="m"
              type="primary"
              effect="plain"
            >
              {{ muscleLabelMap[m] ?? m }}
            </el-tag>
            <span v-if="!currentDetail.primaryMuscles.length" class="muted">—</span>
          </div>
        </div>

        <div class="detail-section">
          <h4>所需器械</h4>
          <div v-if="currentDetail.equipment.length" class="muscle-tags">
            <el-tag v-for="e in currentDetail.equipment" :key="e.id" effect="plain">
              {{ e.name }}
            </el-tag>
          </div>
          <p v-else class="muted">徒手动作，无需器械</p>
        </div>

        <div class="detail-section">
          <h4>组间休息</h4>
          <p>{{ currentDetail.defaultRestSeconds ? `${currentDetail.defaultRestSeconds} 秒` : '—' }}</p>
        </div>

        <div class="detail-section">
          <h4>注意事项</h4>
          <ul v-if="currentDetail.contraindications.length" class="contra-list">
            <li v-for="(c, i) in currentDetail.contraindications" :key="i">{{ c }}</li>
          </ul>
          <p v-else class="muted">暂无特别禁忌说明</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.exercises-page {
  max-width: 1200px;
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

.page-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.keyword-input {
  max-width: 320px;
}
.muscle-select {
  width: 160px;
}

.grid-wrap {
  min-height: 200px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 16px;
}

.exercise-card {
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.exercise-card:hover,
.exercise-card:focus-visible {
  transform: translateY(-3px);
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
  outline: none;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px 14px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.card-title-row h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.english-name {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muscle-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.equipment-line {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.equipment-more {
  color: var(--el-text-color-placeholder);
}

.empty-block {
  padding: 40px 0;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 详情抽屉 */
.detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.detail-title-row h3 {
  margin: 0;
  font-size: 18px;
}

.detail-section h4 {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.muted {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

.detail-desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
}

.detail-instructions {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}

.contra-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}

@media (max-width: 640px) {
  .filter-bar {
    flex-direction: column;
  }
  .keyword-input,
  .muscle-select {
    max-width: none;
    width: 100%;
  }
}
</style>
