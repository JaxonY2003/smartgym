<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as nutritionApi from '@/api/nutrition'
import MealPlanPanel from './MealPlanPanel.vue'
import {
  FOOD_CATEGORY_LABELS,
  MEAL_TYPE_LABELS,
  NUTRITION_SOURCE_LABELS,
  toOptions,
} from '@/constants/dictionaries'
import { formatDateTime, toLocalIso } from '@/utils/datetime'
import type {
  DailySummary,
  Food,
  FoodCategory,
  MealRecord,
  MealType,
} from '@/types/api/nutrition'

/* ==================== 今日饮食 ==================== */

const today = new Date().toISOString().slice(0, 10)
const selectedDate = ref(today)
const summary = ref<DailySummary | null>(null)
const records = ref<MealRecord[]>([])
const todayLoading = ref(false)

const waterTotalMl = ref(0)
const waterTargetMl = ref(2000)

async function loadTodayData() {
  todayLoading.value = true
  try {
    const [s, r, w] = await Promise.all([
      nutritionApi.getDailySummary(selectedDate.value),
      nutritionApi.getMealRecords({ date: selectedDate.value, page: 1, pageSize: 50 }),
      nutritionApi.getWaterRecords(selectedDate.value),
    ])
    summary.value = s
    records.value = r.items
    waterTotalMl.value = w.totalMl ?? 0
  } catch {
    summary.value = null
    records.value = []
    waterTotalMl.value = 0
  } finally {
    todayLoading.value = false
  }
}

watch(selectedDate, loadTodayData)

/** 按餐次分组 */
const groupedRecords = computed(() => {
  const order: MealType[] = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK']
  return order
    .map((t) => ({ type: t, items: records.value.filter((r) => r.mealType === t) }))
    .filter((g) => g.items.length > 0)
})

/** 汇总卡的四个指标 */
const summaryCards = computed(() => {
  if (!summary.value) return []
  const s = summary.value
  return [
    { label: '热量（kcal）', consumed: s.consumed.calories, target: s.target.calories, rate: s.completionRate.calories },
    { label: '蛋白质（g）', consumed: s.consumed.proteinG, target: s.target.proteinG, rate: s.completionRate.protein },
    { label: '碳水（g）', consumed: s.consumed.carbohydrateG, target: s.target.carbohydrateG, rate: s.completionRate.carbohydrate },
    { label: '脂肪（g）', consumed: s.consumed.fatG, target: s.target.fatG, rate: s.completionRate.fat },
  ]
})

/* ==================== 添加饮食记录 ==================== */

const recordDialogVisible = ref(false)
const recordFormRef = ref<FormInstance>()
const recordSaving = ref(false)

const recordForm = reactive({
  mealType: 'BREAKFAST' as MealType,
  eatenAt: '',
  note: '',
  items: [] as { foodId: number | undefined; foodName: string; amount: number; unit: string }[],
})

const recordRules: FormRules = {
  mealType: [{ required: true, message: '请选择餐次', trigger: 'change' }],
  eatenAt: [{ required: true, message: '请选择时间', trigger: 'change' }],
}

/** 弹窗内食物远程搜索 */
const foodOptions = ref<Food[]>([])
const foodSearching = ref(false)
let foodTimer: ReturnType<typeof setTimeout> | null = null

async function searchFoods(keyword: string) {
  foodSearching.value = true
  try {
    const data = await nutritionApi.getFoods({ keyword: keyword || undefined, page: 1, pageSize: 20 })
    foodOptions.value = data.items
  } catch {
    foodOptions.value = []
  } finally {
    foodSearching.value = false
  }
}

function debouncedSearchFoods(keyword: string) {
  if (foodTimer) clearTimeout(foodTimer)
  foodTimer = setTimeout(() => searchFoods(keyword), 300)
}

function addRecordItem() {
  recordForm.items.push({ foodId: undefined, foodName: '', amount: 100, unit: 'g' })
}

function removeRecordItem(index: number) {
  recordForm.items.splice(index, 1)
}

function onFoodSelect(index: number, foodId: number | undefined) {
  const food = foodOptions.value.find((f) => f.id === foodId)
  const item = recordForm.items[index]
  if (food && item) {
    item.foodName = food.name
    item.unit = food.defaultServingUnit
  }
}

function openRecordDialog(presetMeal?: MealType) {
  const now = new Date()
  Object.assign(recordForm, {
    mealType: presetMeal ?? 'BREAKFAST',
    eatenAt: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}T${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`,
    note: '',
    items: [],
  })
  searchFoods('')
  recordDialogVisible.value = true
}

async function submitRecord() {
  const valid = await recordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  const validItems = recordForm.items.filter((i) => i.foodId && i.amount > 0)
  if (validItems.length === 0) {
    ElMessage.warning('请至少添加一个有效食物条目')
    return
  }
  recordSaving.value = true
  try {
    await nutritionApi.createMealRecord({
      eatenAt: toLocalIso(new Date(recordForm.eatenAt)),
      mealType: recordForm.mealType,
      items: validItems.map((i) => ({
        foodId: i.foodId!,
        amount: i.amount,
        unit: i.unit,
        confirmedByUser: true,
      })),
      inputSource: 'MANUAL',
      note: recordForm.note || undefined,
    })
    ElMessage.success('饮食记录已保存')
    recordDialogVisible.value = false
    await loadTodayData()
  } catch {
    // 拦截器已提示
  } finally {
    recordSaving.value = false
  }
}

/* ==================== 饮水 ==================== */

const waterSaving = ref(false)

const quickWaterAmounts = [200, 300, 500]

async function addWater(amountMl: number) {
  waterSaving.value = true
  try {
    await nutritionApi.createWaterRecord(toLocalIso(new Date()), amountMl)
    ElMessage.success(`已记录 ${amountMl}ml 饮水`)
    const w = await nutritionApi.getWaterRecords(selectedDate.value)
    waterTotalMl.value = w.totalMl ?? waterTotalMl.value + amountMl
  } catch {
    // 拦截器已提示
  } finally {
    waterSaving.value = false
  }
}

/* ==================== 食物库 tab ==================== */

const foods = ref<Food[]>([])
const foodsLoading = ref(false)
const foodsTotal = ref(0)
const foodsPage = ref(1)
const FOODS_PAGE_SIZE = 12

const foodFilters = reactive({
  keyword: '',
  category: undefined as FoodCategory | undefined,
})

let foodKeywordTimer: ReturnType<typeof setTimeout> | null = null
function onFoodKeywordInput() {
  if (foodKeywordTimer) clearTimeout(foodKeywordTimer)
  foodKeywordTimer = setTimeout(() => {
    foodsPage.value = 1
    loadFoods()
  }, 400)
}

function onFoodFilterChange() {
  foodsPage.value = 1
  loadFoods()
}

async function loadFoods() {
  foodsLoading.value = true
  try {
    const data = await nutritionApi.getFoods({
      keyword: foodFilters.keyword || undefined,
      category: foodFilters.category,
      page: foodsPage.value,
      pageSize: FOODS_PAGE_SIZE,
    })
    foods.value = data.items
    foodsTotal.value = data.total
  } catch {
    foods.value = []
    foodsTotal.value = 0
  } finally {
    foodsLoading.value = false
  }
}

/** 食物详情 + 记录抽屉 */
const foodDrawerVisible = ref(false)
const currentFood = ref<Food | null>(null)
const foodRecordAmount = ref(100)

function openFoodDetail(food: Food) {
  currentFood.value = food
  foodRecordAmount.value = food.defaultServingAmount
  foodDrawerVisible.value = true
}

async function recordThisFood(mealType: MealType) {
  if (!currentFood.value) return
  try {
    const now = new Date()
    await nutritionApi.createMealRecord({
      eatenAt: toLocalIso(now),
      mealType,
      items: [
        {
          foodId: currentFood.value.id,
          amount: foodRecordAmount.value,
          unit: currentFood.value.defaultServingUnit,
          confirmedByUser: true,
        },
      ],
      inputSource: 'MANUAL',
    })
    ElMessage.success(`已记录到${MEAL_TYPE_LABELS[mealType]}`)
    foodDrawerVisible.value = false
    if (selectedDate.value === today) await loadTodayData()
  } catch {
    // 拦截器已提示
  }
}

/** 按 amount 换算 100g 营养 */
function scaledNutrition(food: Food, amount: number) {
  const n = food.nutritionPer100g
  const factor = amount / 100
  return {
    calories: Math.round(n.calories * factor),
    protein: +(n.protein * factor).toFixed(1),
    carbohydrate: +(n.carbohydrate * factor).toFixed(1),
    fat: +(n.fat * factor).toFixed(1),
  }
}

/* ==================== 营养目标 tab ==================== */

const targetLoading = ref(false)
const targetSaving = ref(false)
const targetFormRef = ref<FormInstance>()

const targetForm = reactive({
  calories: 2000,
  proteinG: 120,
  carbohydrateG: 220,
  fatG: 60,
  fiberG: 25,
  source: 'USER_CONFIRMED',
})

const targetRules: FormRules = {
  calories: [
    { required: true, message: '请输入热量目标', trigger: 'blur' },
    { type: 'number', min: 800, max: 5000, message: '热量目标 800～5000 kcal', trigger: 'blur' },
  ],
  proteinG: [{ type: 'number', min: 20, max: 400, message: '蛋白质 20～400 g', trigger: 'blur' }],
  carbohydrateG: [{ type: 'number', min: 0, max: 800, message: '碳水 0～800 g', trigger: 'blur' }],
  fatG: [{ type: 'number', min: 10, max: 250, message: '脂肪 10～250 g', trigger: 'blur' }],
}

async function loadTarget() {
  targetLoading.value = true
  try {
    const t = await nutritionApi.getNutritionTarget()
    Object.assign(targetForm, {
      calories: t.calories,
      proteinG: t.proteinG,
      carbohydrateG: t.carbohydrateG,
      fatG: t.fatG,
      fiberG: t.fiberG ?? 25,
      source: t.source,
    })
    waterTargetMl.value = 2000
  } catch {
    // 保留默认值
  } finally {
    targetLoading.value = false
  }
}

async function saveTarget() {
  const valid = await targetFormRef.value?.validate().catch(() => false)
  if (!valid) return
  targetSaving.value = true
  try {
    await nutritionApi.updateNutritionTarget({
      calories: targetForm.calories,
      proteinG: targetForm.proteinG,
      carbohydrateG: targetForm.carbohydrateG,
      fatG: targetForm.fatG,
      fiberG: targetForm.fiberG,
      source: 'USER_CONFIRMED',
    })
    ElMessage.success('营养目标已保存')
  } catch {
    // 拦截器已提示
  } finally {
    targetSaving.value = false
  }
}

/* ==================== 页面骨架 ==================== */

const activeTab = ref('today')

const loadedTabs = new Set<string>(['today'])

function handleTabChange(name: string | number) {
  const key = String(name)
  if (loadedTabs.has(key)) return
  loadedTabs.add(key)
  if (key === 'foods') loadFoods()
  else if (key === 'target') loadTarget()
  // meal-plan 面板组件内部自行在 onMounted 加载
}

const mealTypeOptions = toOptions(MEAL_TYPE_LABELS)
const categoryOptions = toOptions(FOOD_CATEGORY_LABELS)

onMounted(() => {
  loadTodayData()
})
</script>

<template>
  <div class="nutrition-page">
    <header class="page-head">
      <div>
        <h2>饮食管理</h2>
        <p>记录每日饮食，追踪营养摄入与饮水</p>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="page-tabs" @tab-change="handleTabChange">
      <!-- 今日饮食 -->
      <el-tab-pane label="今日饮食" name="today">
        <div v-loading="todayLoading" class="today-wrap">
          <!-- 汇总卡 -->
          <el-card shadow="never" class="summary-card">
            <template #header>
              <div class="card-head">
                <span>每日营养汇总</span>
                <el-date-picker
                  v-model="selectedDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :clearable="false"
                  :disabled-date="(d: Date) => d.getTime() > Date.now()"
                  size="small"
                  style="width: 140px"
                />
              </div>
            </template>

            <template v-if="summary">
              <div class="summary-grid">
                <div v-for="c in summaryCards" :key="c.label" class="summary-item">
                  <div class="summary-top">
                    <span class="summary-label">{{ c.label }}</span>
                    <span class="summary-rate" :class="{ over: c.rate > 1.1 }">
                      {{ Math.round(c.rate * 100) }}%
                    </span>
                  </div>
                  <div class="summary-nums">
                    <b>{{ Math.round(c.consumed) }}</b>
                    <span class="target">/ {{ c.target }}</span>
                  </div>
                  <el-progress
                    :percentage="Math.min(100, Math.round(c.rate * 100))"
                    :status="c.rate > 1.1 ? 'exception' : c.rate >= 0.9 ? 'success' : undefined"
                    :stroke-width="8"
                  />
                </div>
              </div>
              <div class="summary-footer">
                <span>已记录 {{ summary.mealCount }} 餐</span>
                <span>记录完整度 {{ Math.round(summary.recordCompleteness * 100) }}%</span>
              </div>
            </template>
            <el-empty v-else description="暂无汇总数据" :image-size="60" />
          </el-card>

          <!-- 饮水卡 -->
          <el-card shadow="never" class="water-card">
            <template #header>
              <div class="card-head"><span>饮水记录</span></div>
            </template>
            <div class="water-body">
              <div class="water-stats">
                <span class="water-num">{{ waterTotalMl }}</span>
                <span class="water-target">/ {{ waterTargetMl }} ml</span>
              </div>
              <el-progress
                :percentage="Math.min(100, Math.round((waterTotalMl / waterTargetMl) * 100))"
                :stroke-width="10"
                :status="waterTotalMl >= waterTargetMl ? 'success' : undefined"
              />
              <div class="water-actions">
                <el-button
                  v-for="a in quickWaterAmounts"
                  :key="a"
                  size="small"
                  plain
                  :loading="waterSaving"
                  @click="addWater(a)"
                >
                  +{{ a }}ml
                </el-button>
              </div>
            </div>
          </el-card>

          <!-- 各餐记录 -->
          <el-card shadow="never" class="meals-card">
            <template #header>
              <div class="card-head">
                <span>饮食记录</span>
                <el-button type="primary" size="small" :icon="Plus" @click="openRecordDialog()">
                  添加记录
                </el-button>
              </div>
            </template>

            <el-empty
              v-if="groupedRecords.length === 0"
              description="今天还没有饮食记录，点击右上角添加"
              :image-size="72"
            />

            <div v-for="group in groupedRecords" :key="group.type" class="meal-group">
              <div class="meal-group-head">
                <b>{{ MEAL_TYPE_LABELS[group.type] }}</b>
                <span class="meal-group-kcal">
                  {{ Math.round(group.items.reduce((s, r) => s + r.totalNutrition.calories, 0)) }} kcal
                </span>
              </div>
              <div v-for="r in group.items" :key="r.id" class="meal-record">
                <div class="meal-record-head">
                  <span>{{ formatDateTime(r.eatenAt) }}</span>
                  <span v-if="r.note" class="meal-note">{{ r.note }}</span>
                </div>
                <el-table :data="r.items" size="small">
                  <el-table-column label="食物" min-width="120">
                    <template #default="{ row }">{{ row.foodNameSnapshot }}</template>
                  </el-table-column>
                  <el-table-column label="份量" width="90" align="right">
                    <template #default="{ row }">{{ row.amount }}{{ row.unit }}</template>
                  </el-table-column>
                  <el-table-column label="热量" width="80" align="right">
                    <template #default="{ row }">{{ Math.round(row.estimatedNutrition.calories) }} kcal</template>
                  </el-table-column>
                  <el-table-column label="蛋白质" width="70" align="right">
                    <template #default="{ row }">{{ row.estimatedNutrition.proteinG }}g</template>
                  </el-table-column>
                  <el-table-column label="碳水" width="70" align="right">
                    <template #default="{ row }">{{ row.estimatedNutrition.carbohydrateG }}g</template>
                  </el-table-column>
                  <el-table-column label="脂肪" width="70" align="right">
                    <template #default="{ row }">{{ row.estimatedNutrition.fatG }}g</template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 食物库 -->
      <el-tab-pane label="食物库" name="foods" lazy>
        <el-card shadow="never">
          <div class="filter-bar">
            <el-input
              v-model="foodFilters.keyword"
              placeholder="搜索食物，如：鸡蛋"
              clearable
              style="max-width: 280px"
              @input="onFoodKeywordInput"
              @clear="onFoodFilterChange"
            />
            <el-select
              v-model="foodFilters.category"
              placeholder="类别"
              clearable
              style="width: 140px"
              @change="onFoodFilterChange"
            >
              <el-option v-for="o in categoryOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>

          <div v-loading="foodsLoading" class="food-grid-wrap">
            <el-empty v-if="!foodsLoading && foods.length === 0" description="没有匹配的食物" />
            <div v-else class="food-grid">
              <button
                v-for="f in foods"
                :key="f.id"
                class="food-card"
                @click="openFoodDetail(f)"
              >
                <div class="food-head">
                  <b>{{ f.name }}</b>
                  <el-tag size="small" effect="plain">{{ FOOD_CATEGORY_LABELS[f.category] ?? f.category }}</el-tag>
                </div>
                <div class="food-kcal">
                  <span class="kcal-num">{{ f.nutritionPer100g.calories }}</span>
                  <span class="kcal-unit">kcal / 100{{ f.defaultServingUnit }}</span>
                </div>
                <div class="food-macros">
                  <span>蛋白 {{ f.nutritionPer100g.protein }}g</span>
                  <span>碳水 {{ f.nutritionPer100g.carbohydrate }}g</span>
                  <span>脂肪 {{ f.nutritionPer100g.fat }}g</span>
                </div>
                <div v-if="f.allergens.length" class="food-allergens">
                  过敏原：{{ f.allergens.join('、') }}
                </div>
              </button>
            </div>
          </div>

          <div v-if="foodsTotal > FOODS_PAGE_SIZE" class="pager">
            <el-pagination
              v-model:current-page="foodsPage"
              :page-size="FOODS_PAGE_SIZE"
              :total="foodsTotal"
              layout="total, prev, pager, next"
              background
              @current-change="loadFoods"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 饮食计划 -->
      <el-tab-pane label="饮食计划" name="meal-plan" lazy>
        <el-card shadow="never">
          <MealPlanPanel />
        </el-card>
      </el-tab-pane>

      <!-- 营养目标 -->
      <el-tab-pane label="营养目标" name="target" lazy>
        <el-card v-loading="targetLoading" shadow="never" class="target-card">
          <template #header>
            <div class="card-head">
              <span>每日营养目标</span>
              <el-tag v-if="targetForm.source" size="small" effect="plain">
                {{ NUTRITION_SOURCE_LABELS[targetForm.source] ?? targetForm.source }}
              </el-tag>
            </div>
          </template>

          <el-form
            ref="targetFormRef"
            :model="targetForm"
            :rules="targetRules"
            label-position="top"
            style="max-width: 480px"
          >
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="热量（kcal）" prop="calories">
                  <el-input-number v-model="targetForm.calories" :min="800" :max="5000" :step="50" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="蛋白质（g）" prop="proteinG">
                  <el-input-number v-model="targetForm.proteinG" :min="20" :max="400" :step="5" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="碳水（g）" prop="carbohydrateG">
                  <el-input-number v-model="targetForm.carbohydrateG" :min="0" :max="800" :step="10" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="脂肪（g）" prop="fatG">
                  <el-input-number v-model="targetForm.fatG" :min="10" :max="250" :step="5" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="膳食纤维（g）">
                  <el-input-number v-model="targetForm.fiberG" :min="0" :max="80" controls-position="right" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <div class="form-actions">
              <el-button type="primary" :loading="targetSaving" @click="saveTarget">保存目标</el-button>
            </div>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加饮食记录弹窗 -->
    <el-dialog
      v-model="recordDialogVisible"
      title="添加饮食记录"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="recordFormRef" :model="recordForm" :rules="recordRules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="餐次" prop="mealType">
              <el-select v-model="recordForm.mealType" style="width: 100%">
                <el-option v-for="o in mealTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="用餐时间" prop="eatenAt">
              <el-date-picker v-model="recordForm.eatenAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="食物条目">
          <div class="record-items">
            <div v-for="(item, i) in recordForm.items" :key="i" class="record-item-row">
              <el-select
                :model-value="item.foodId"
                filterable
                remote
                :remote-method="debouncedSearchFoods"
                :loading="foodSearching"
                placeholder="搜索食物"
                class="item-food"
                @change="(v: number | undefined) => { item.foodId = v; onFoodSelect(i, v) }"
              >
                <el-option v-for="f in foodOptions" :key="f.id" :label="f.name" :value="f.id" />
              </el-select>
              <el-input-number v-model="item.amount" :min="1" :max="2000" size="default" controls-position="right" class="item-amount" />
              <el-input v-model="item.unit" class="item-unit" placeholder="单位" />
              <el-button size="small" type="danger" plain circle :icon="Plus" @click="removeRecordItem(i)" style="transform: rotate(45deg)" />
            </div>
            <el-button size="small" :icon="Plus" @click="addRecordItem">添加食物</el-button>
          </div>
        </el-form-item>

        <el-form-item label="备注（选填）">
          <el-input v-model="recordForm.note" maxlength="50" placeholder="如：食堂午餐" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="recordSaving" @click="submitRecord">保存</el-button>
      </template>
    </el-dialog>

    <!-- 食物详情抽屉 -->
    <el-drawer v-model="foodDrawerVisible" :title="currentFood?.name ?? '食物详情'" size="380px">
      <div v-if="currentFood" class="food-detail">
        <div class="detail-tags">
          <el-tag size="small">{{ FOOD_CATEGORY_LABELS[currentFood.category] ?? currentFood.category }}</el-tag>
          <el-tag v-for="a in currentFood.allergens" :key="a" size="small" type="danger" effect="plain">
            {{ a }}
          </el-tag>
        </div>

        <div class="detail-amount">
          <span>份量：{{ foodRecordAmount }}{{ currentFood.defaultServingUnit }}</span>
          <el-input-number v-model="foodRecordAmount" :min="1" :max="2000" :step="10" size="small" controls-position="right" />
        </div>

        <div class="detail-nutrition">
          <h4>营养估算（按当前份量）</h4>
          <div class="nutrition-grid">
            <div class="n-item">
              <b>{{ scaledNutrition(currentFood, foodRecordAmount).calories }}</b>
              <span>热量 kcal</span>
            </div>
            <div class="n-item">
              <b>{{ scaledNutrition(currentFood, foodRecordAmount).protein }}</b>
              <span>蛋白质 g</span>
            </div>
            <div class="n-item">
              <b>{{ scaledNutrition(currentFood, foodRecordAmount).carbohydrate }}</b>
              <span>碳水 g</span>
            </div>
            <div class="n-item">
              <b>{{ scaledNutrition(currentFood, foodRecordAmount).fat }}</b>
              <span>脂肪 g</span>
            </div>
          </div>
        </div>

        <div class="detail-source">
          数据来源：{{ currentFood.source ?? '—' }}
          <span v-if="currentFood.sourceVersion">（{{ currentFood.sourceVersion }}）</span>
        </div>

        <div class="detail-actions">
          <el-button v-for="o in mealTypeOptions" :key="o.value" size="small" plain @click="recordThisFood(o.value as MealType)">
            记到{{ o.label }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.nutrition-page {
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
}

/* 今日汇总 */
.summary-card {
  margin-bottom: 16px;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 18px;
}
.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.summary-rate {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
}
.summary-rate.over {
  color: var(--el-color-danger);
}
.summary-nums b {
  font-size: 22px;
  font-weight: 700;
}
.summary-nums .target {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-left: 4px;
}
.summary-footer {
  display: flex;
  gap: 20px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 饮水 */
.water-card {
  margin-bottom: 16px;
}
.water-body {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.water-stats {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.water-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.water-target {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}
.water-body .el-progress {
  flex: 1;
  min-width: 160px;
}
.water-actions {
  display: flex;
  gap: 8px;
}

/* 餐次分组 */
.meals-card {
  margin-bottom: 16px;
}
.meal-group {
  margin-bottom: 18px;
}
.meal-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
}
.meal-group-kcal {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.meal-record {
  margin-bottom: 12px;
}
.meal-record-head {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.meal-note {
  color: var(--el-text-color-placeholder);
}

/* 食物库 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.food-grid-wrap {
  min-height: 200px;
}
.food-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.food-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  text-align: left;
  font-family: inherit;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.food-card:hover {
  transform: translateY(-2px);
  border-color: var(--el-color-primary-light-5);
  box-shadow: var(--el-box-shadow-light);
}
.food-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.food-kcal {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.kcal-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.kcal-unit {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.food-macros {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.food-allergens {
  font-size: 12px;
  color: var(--el-color-danger);
}

/* 记录弹窗 */
.record-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}
.record-item-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.item-food {
  flex: 1;
}
.item-amount {
  width: 110px;
}
.item-unit {
  width: 70px;
}

/* 食物抽屉 */
.food-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.detail-amount {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 14px;
}
.detail-nutrition h4 {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.nutrition-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.n-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 10px 4px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}
.n-item b {
  font-size: 17px;
  color: var(--el-color-primary);
}
.n-item span {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.detail-source {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 营养目标 */
.target-card {
  max-width: 640px;
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

@media (max-width: 640px) {
  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }
  .record-item-row {
    flex-wrap: wrap;
  }
  .item-food {
    width: 100%;
  }
}
</style>
