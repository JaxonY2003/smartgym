<script setup lang="ts">
/**
 * 饮食计划子模块：列表 + 创建 + 激活/归档 + 替换食物（接口 6.5）
 * 作为饮食页的"饮食计划" tab 内容。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as nutritionApi from '@/api/nutrition'
import {
  GOAL_LABELS,
  MEAL_TYPE_LABELS,
  PLAN_STATUS_LABELS,
  PLAN_STATUS_TAG_TYPES,
  toOptions,
} from '@/constants/dictionaries'
import { formatDate } from '@/utils/datetime'
import type { Food, MealType } from '@/types/api/nutrition'
import type { FitnessGoal } from '@/types/api/member'
import type { MealPlan } from '@/types/api/meal-plan'
import { mealsOf } from '@/types/api/meal-plan'

/* ==================== 列表 ==================== */

const plans = ref<MealPlan[]>([])
const listLoading = ref(false)

async function loadPlans() {
  listLoading.value = true
  try {
    const data = await nutritionApi.getMealPlans({ page: 1, pageSize: 20 })
    plans.value = data.items
  } catch {
    plans.value = []
  } finally {
    listLoading.value = false
  }
}

async function handleActivate(plan: MealPlan) {
  try {
    await ElMessageBox.confirm(
      `激活「${plan.name}」后，其他进行中的饮食计划将被归档。确定激活吗？`,
      '激活计划',
      { confirmButtonText: '激活', cancelButtonText: '取消', type: 'info' },
    )
  } catch {
    return
  }
  try {
    await nutritionApi.activateMealPlan(plan.id)
    ElMessage.success('计划已激活')
    await loadPlans()
  } catch {
    // 拦截器已提示
  }
}

async function handleArchive(plan: MealPlan) {
  try {
    await nutritionApi.patchMealPlan(plan.id, { status: 'ARCHIVED' })
    ElMessage.success('计划已归档')
    await loadPlans()
  } catch {
    // 拦截器已提示
  }
}

/* ==================== 详情（展开行内替换） ==================== */

const expandedIds = ref<number[]>([])
const detailCache = ref<Record<number, MealPlan>>({})

/** 展开某计划时加载详情（带 item.id，替换接口需要）。expand-change 参数为 (row, expandedRows) */
async function onExpand(_row: MealPlan, expandedRows: MealPlan[]) {
  expandedIds.value = expandedRows.map((r) => r.id)
  for (const r of expandedRows) {
    if (detailCache.value[r.id]) continue
    try {
      detailCache.value[r.id] = await nutritionApi.getMealPlan(r.id)
    } catch {
      // 展开失败保留列表数据
    }
  }
}

/* ==================== 替换食物弹窗 ==================== */

const replaceVisible = ref(false)
const replaceSaving = ref(false)
const replacePlanId = ref(0)
const replaceItemName = ref('')
const replaceFormRef = ref<FormInstance>()

const replaceForm = reactive({
  itemId: 0,
  replacementFoodId: undefined as number | undefined,
  reason: '',
})

const replaceRules: FormRules = {
  replacementFoodId: [{ required: true, message: '请选择替换食物', trigger: 'change' }],
}

/** 替换弹窗的食物候选（排除过敏原由服务端把关，前端提供搜索） */
const replaceFoodOptions = ref<Food[]>([])
const replaceSearching = ref(false)

async function searchReplaceFoods(keyword: string) {
  replaceSearching.value = true
  try {
    const data = await nutritionApi.getFoods({ keyword: keyword || undefined, page: 1, pageSize: 20 })
    replaceFoodOptions.value = data.items
  } catch {
    replaceFoodOptions.value = []
  } finally {
    replaceSearching.value = false
  }
}

function openReplace(plan: MealPlan, itemId: number, foodName: string) {
  replacePlanId.value = plan.id
  replaceForm.itemId = itemId
  replaceForm.replacementFoodId = undefined
  replaceForm.reason = ''
  replaceItemName.value = foodName
  searchReplaceFoods('')
  replaceVisible.value = true
}

async function submitReplace() {
  const valid = await replaceFormRef.value?.validate().catch(() => false)
  if (!valid) return
  replaceSaving.value = true
  try {
    const updated = await nutritionApi.replaceMealPlanItem(replacePlanId.value, {
      itemId: replaceForm.itemId,
      replacementFoodId: replaceForm.replacementFoodId!,
      reason: replaceForm.reason || undefined,
    })
    ElMessage.success('食物已替换，营养总量已重算')
    detailCache.value[updated.id] = updated
    replaceVisible.value = false
    await loadPlans()
  } catch {
    // 拦截器已提示（含过敏原拦截 BUSINESS_RULE_VIOLATION）
  } finally {
    replaceSaving.value = false
  }
}

/* ==================== 创建计划 ==================== */

const createVisible = ref(false)
const createSaving = ref(false)
const createFormRef = ref<FormInstance>()

const createForm = reactive({
  name: '',
  date: new Date().toISOString().slice(0, 10),
  goal: 'FAT_LOSS' as FitnessGoal,
  meals: [] as { mealType: MealType; time: string; items: { foodId: number | undefined; foodName: string; amount: number; unit: string }[] }[],
})

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入计划名称', trigger: 'blur' },
    { min: 2, max: 50, message: '名称为 2～50 个字符', trigger: 'blur' },
  ],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

const goalOptions = toOptions(GOAL_LABELS)
const mealTypeOptions = toOptions(MEAL_TYPE_LABELS)

/** 创建弹窗的食物远程搜索 */
const createFoodOptions = ref<Food[]>([])
const createSearching = ref(false)

async function searchCreateFoods(keyword: string) {
  createSearching.value = true
  try {
    const data = await nutritionApi.getFoods({ keyword: keyword || undefined, page: 1, pageSize: 20 })
    createFoodOptions.value = data.items
  } catch {
    createFoodOptions.value = []
  } finally {
    createSearching.value = false
  }
}

function addMeal() {
  createForm.meals.push({ mealType: 'BREAKFAST', time: '08:00', items: [] })
}

function removeMeal(index: number) {
  createForm.meals.splice(index, 1)
}

function addMealItem(meal: (typeof createForm.meals)[number]) {
  meal.items.push({ foodId: undefined, foodName: '', amount: 100, unit: 'g' })
}

function removeMealItem(meal: (typeof createForm.meals)[number], index: number) {
  meal.items.splice(index, 1)
}

function onCreateFoodSelect(meal: (typeof createForm.meals)[number], index: number, foodId: number | undefined) {
  const food = createFoodOptions.value.find((f) => f.id === foodId)
  const item = meal.items[index]
  if (food && item) {
    item.foodName = food.name
    item.unit = food.defaultServingUnit
  }
}

function openCreate() {
  Object.assign(createForm, {
    name: '',
    date: new Date().toISOString().slice(0, 10),
    goal: 'FAT_LOSS',
    meals: [],
  })
  searchCreateFoods('')
  createVisible.value = true
}

const createValid = () =>
  createForm.meals.length > 0 &&
  createForm.meals.every((m) => m.items.length > 0 && m.items.every((i) => i.foodId && i.amount > 0))

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!createValid()) {
    ElMessage.warning('请为每个餐次至少安排一个有效食物')
    return
  }
  createSaving.value = true
  try {
    await nutritionApi.createMealPlan({
      name: createForm.name,
      date: createForm.date,
      goal: createForm.goal,
      meals: createForm.meals.map((m) => ({
        mealType: m.mealType,
        time: m.time,
        items: m.items.map((i) => ({ foodId: i.foodId!, amount: i.amount, unit: i.unit })),
      })),
      assumptions: ['食物份量按默认份量估算'],
    })
    ElMessage.success('饮食计划已创建')
    createVisible.value = false
    await loadPlans()
  } catch {
    // 拦截器已提示
  } finally {
    createSaving.value = false
  }
}

onMounted(() => {
  loadPlans()
})
</script>

<template>
  <div class="meal-plan-module">
    <div class="module-head">
      <span>为特定日期安排饮食，可按过敏原替换食物</span>
      <el-button type="primary" size="small" :icon="Plus" @click="openCreate">新建计划</el-button>
    </div>

    <div v-loading="listLoading" class="plan-list">
      <el-empty v-if="!listLoading && plans.length === 0" description="暂无饮食计划，点击右上角新建" :image-size="72" />

      <el-table
        v-else
        :data="plans"
        row-key="id"
        stripe
        :expand-row-keys="expandedIds"
        @expand-change="onExpand"
      >
        <el-table-column type="expand">
          <template #default="{ row }: { row: MealPlan }">
            <div class="expand-content">
              <template v-for="meal in mealsOf(detailCache[row.id] ?? row)" :key="meal.mealType + meal.time">
                <div class="meal-block">
                  <div class="meal-block-head">
                    <b>{{ MEAL_TYPE_LABELS[meal.mealType] }}</b>
                    <span class="meal-time">{{ meal.time }}</span>
                  </div>
                  <el-table :data="meal.items" size="small" border>
                    <el-table-column label="食物" min-width="120">
                      <template #default="{ row: item }: { row: { id?: number; foodName?: string; foodNameSnapshot?: string; foodId: number } }">
                        {{ item.foodName ?? item.foodNameSnapshot ?? `食物 #${item.foodId}` }}
                      </template>
                    </el-table-column>
                    <el-table-column label="份量" width="90" align="right">
                      <template #default="{ row: item }: { row: { amount: number; unit: string } }">
                        {{ item.amount }}{{ item.unit }}
                      </template>
                    </el-table-column>
                    <el-table-column v-if="meal.items.some((i) => i.id)" label="操作" width="90" align="center">
                      <template #default="{ row: item }: { row: { id?: number; foodName?: string; foodNameSnapshot?: string; foodId: number } }">
                        <el-button
                          v-if="item.id"
                          size="small"
                          text
                          type="primary"
                          @click="openReplace(row, item.id!, item.foodName ?? item.foodNameSnapshot ?? `#${item.foodId}`)"
                        >
                          替换
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </template>

              <div v-if="(detailCache[row.id] ?? row).assumptions?.length" class="plan-assumptions">
                <b>假设：</b>
                <span v-for="(a, i) in (detailCache[row.id] ?? row).assumptions" :key="i">{{ a }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="计划名称" min-width="160">
          <template #default="{ row }: { row: MealPlan }">
            <b>{{ row.name }}</b>
          </template>
        </el-table-column>
        <el-table-column label="日期" width="110">
          <template #default="{ row }: { row: MealPlan }">{{ formatDate(row.date) }}</template>
        </el-table-column>
        <el-table-column label="目标" width="90">
          <template #default="{ row }: { row: MealPlan }">
            {{ row.goal ? GOAL_LABELS[row.goal] : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="餐次" width="70" align="center">
          <template #default="{ row }: { row: MealPlan }">{{ mealsOf(row).length }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }: { row: MealPlan }">
            <el-tag :type="PLAN_STATUS_TAG_TYPES[row.status] ?? 'info'" size="small">
              {{ PLAN_STATUS_LABELS[row.status] ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }: { row: MealPlan }">
            <el-button
              v-if="row.status !== 'ACTIVE'"
              size="small"
              type="success"
              plain
              @click="handleActivate(row)"
            >
              激活
            </el-button>
            <el-button v-else size="small" plain @click="handleArchive(row)">归档</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 替换食物弹窗 -->
    <el-dialog
      v-model="replaceVisible"
      title="替换食物"
      width="440px"
      :close-on-click-modal="false"
    >
      <p class="replace-tip">
        将替换「{{ replaceItemName }}」。服务端会重新计算整份计划营养，并执行过敏原检查。
      </p>
      <el-form ref="replaceFormRef" :model="replaceForm" :rules="replaceRules" label-position="top">
        <el-form-item label="替换为" prop="replacementFoodId">
          <el-select
            v-model="replaceForm.replacementFoodId"
            filterable
            remote
            :remote-method="searchReplaceFoods"
            :loading="replaceSearching"
            placeholder="搜索食物"
            style="width: 100%"
          >
            <el-option v-for="f in replaceFoodOptions" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="替换原因（选填）">
          <el-input v-model="replaceForm.reason" maxlength="100" placeholder="如：不含花生且蛋白质接近" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replaceVisible = false">取消</el-button>
        <el-button type="primary" :loading="replaceSaving" @click="submitReplace">确认替换</el-button>
      </template>
    </el-dialog>

    <!-- 新建计划弹窗 -->
    <el-dialog
      v-model="createVisible"
      title="新建饮食计划"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-row :gutter="16">
          <el-col :span="10">
            <el-form-item label="计划名称" prop="name">
              <el-input v-model="createForm.name" maxlength="50" placeholder="如：减脂训练日饮食计划" />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="日期" prop="date">
              <el-date-picker
                v-model="createForm.date"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="目标">
              <el-select v-model="createForm.goal" style="width: 100%">
                <el-option v-for="o in goalOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">餐次安排</el-divider>

        <div v-for="(meal, mi) in createForm.meals" :key="mi" class="create-meal">
          <div class="create-meal-head">
            <el-select v-model="meal.mealType" size="small" style="width: 100px">
              <el-option v-for="o in mealTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
            <el-time-select v-model="meal.time" start="06:00" end="22:00" step="00:30" size="small" placeholder="时间" style="width: 110px" />
            <el-button size="small" type="primary" plain :icon="Plus" @click="addMealItem(meal)">食物</el-button>
            <el-button size="small" type="danger" plain size-class="sm" @click="removeMeal(mi)">删除餐次</el-button>
          </div>

          <div v-for="(item, ii) in meal.items" :key="ii" class="create-item-row">
            <el-select
              :model-value="item.foodId"
              filterable
              remote
              :remote-method="searchCreateFoods"
              :loading="createSearching"
              placeholder="搜索食物"
              size="small"
              class="create-food-select"
              @change="(v: number | undefined) => { item.foodId = v; onCreateFoodSelect(meal, ii, v) }"
            >
              <el-option v-for="f in createFoodOptions" :key="f.id" :label="f.name" :value="f.id" />
            </el-select>
            <el-input-number v-model="item.amount" :min="1" :max="2000" size="small" controls-position="right" style="width: 110px" />
            <el-input v-model="item.unit" size="small" style="width: 64px" />
            <el-button size="small" type="danger" plain circle :icon="Plus" style="transform: rotate(45deg)" @click="removeMealItem(meal, ii)" />
          </div>
          <p v-if="meal.items.length === 0" class="meal-empty">尚未安排食物</p>
        </div>

        <el-button size="small" dashed :icon="Plus" @click="addMeal">添加餐次</el-button>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSaving" :disabled="!createValid()" @click="submitCreate">
          创建计划
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.module-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.plan-list {
  min-height: 160px;
}

.expand-content {
  padding: 4px 12px 12px;
}

.meal-block {
  margin-bottom: 14px;
}
.meal-block-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 13px;
}
.meal-time {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.plan-assumptions {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.replace-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.create-meal {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  margin-bottom: 10px;
}
.create-meal-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.create-item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.create-food-select {
  width: 180px;
}
.meal-empty {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

@media (max-width: 640px) {
  .create-food-select {
    width: 100%;
  }
}
</style>
