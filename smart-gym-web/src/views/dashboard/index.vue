<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import * as dashboardApi from '@/api/dashboard'
import type { DashboardOverview, TrendData, TrendMetric } from '@/types/api/dashboard'

const router = useRouter()
const auth = useAuthStore()

const overview = ref<DashboardOverview | null>(null)
const loading = ref(true)

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await dashboardApi.getOverview()
  } catch {
    overview.value = null
  } finally {
    loading.value = false
  }
}

/* ==================== 概览卡片派生数据 ==================== */

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const workoutStatusText: Record<string, string> = {
  NOT_STARTED: '待开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  REST_DAY: '休息日',
}

const calorieRate = computed(() => {
  const n = overview.value?.nutrition
  if (!n || !n.caloriesTarget) return 0
  return Math.min(1, n.caloriesConsumed / n.caloriesTarget)
})

const proteinRate = computed(() => {
  const n = overview.value?.nutrition
  if (!n || !n.proteinTargetG) return 0
  return Math.min(1, n.proteinConsumedG / n.proteinTargetG)
})

const weightTrendText = computed(() => {
  const o = overview.value
  if (o?.latestWeightKg == null) return '暂无记录'
  if (o.weightChangeKg7d == null || Math.abs(o.weightChangeKg7d) < 0.05) return '7 天基本持平'
  const dir = o.weightChangeKg7d < 0 ? '下降' : '上升'
  return `7 天${dir} ${Math.abs(o.weightChangeKg7d).toFixed(1)} kg`
})

function askAgent(question: string) {
  // 快捷提问 → 跳转 AI 助理并携带问题（对话页读取 query 预填）
  router.push({ path: '/agent', query: { q: question } })
}

/* ==================== 趋势图（ECharts 按需引入） ==================== */

import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const trendMetric = ref<TrendMetric>('WEIGHT')
const trendLoading = ref(false)
const trendData = ref<TrendData | null>(null)

const METRIC_LABELS: Record<TrendMetric, string> = {
  WEIGHT: '体重',
  TRAINING_VOLUME: '训练容量',
  NUTRITION: '热量摄入',
}

/** 默认拉取近 30 天 */
function last30Days(): { from: string; to: string } {
  const to = new Date()
  const from = new Date(to.getTime() - 29 * 86400_000)
  const fmt = (d: Date) => d.toISOString().slice(0, 10)
  return { from: fmt(from), to: fmt(to) }
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const { from, to } = last30Days()
    const data = await dashboardApi.getTrend(trendMetric.value, from, to)
    trendLoading.value = false
    trendData.value = data
    // 先关 loading 再等两帧：chart div 由 v-if 控制，需等其真正插入 DOM 且有尺寸后 init
    await nextTick()
    await new Promise((r) => requestAnimationFrame(() => r(null)))
    renderChart()
  } catch {
    trendLoading.value = false
    trendData.value = null
  }
}

function renderChart() {
  // 初次渲染时容器可能因 v-loading 尚未挂载；存在实例则只更新配置
  if (chart) {
    applyChartOption()
    return
  }
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  applyChartOption()
}

function applyChartOption() {
  if (!chart) return
  const points = trendData.value?.points ?? []
  chart.setOption({
    grid: { left: 44, right: 16, top: 20, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      formatter: (params: unknown) => {
        const p = (params as { name: string; value: number }[])[0]
        return `${p.name}<br/><b>${p.value}</b> ${trendData.value?.unit ?? ''}`
      },
    },
    xAxis: {
      type: 'category',
      data: points.map((p) => p.date.slice(5)),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399', fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      scale: true,
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f0f2f5' } },
    },
    series: [
      {
        type: 'line',
        data: points.map((p) => p.value),
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2.5, color: '#10b981' },
        itemStyle: { color: '#10b981' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(16,185,129,0.22)' },
              { offset: 1, color: 'rgba(16,185,129,0)' },
            ],
          },
        },
      },
    ],
  })
}

function onMetricChange() {
  loadTrend()
}

function handleResize() {
  chart?.resize()
}

onMounted(async () => {
  // 页面根元素有 v-loading="loading"：overview 返回前 chart div 尚未挂载，
  // 必须先等 overview 完成再拉趋势并渲染，否则 chartEl 为 null 直接跳过
  await loadOverview()
  loadTrend()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 欢迎区 -->
    <header class="hero">
      <div>
        <h2>{{ greeting }}，{{ auth.user?.displayName ?? '健身伙伴' }}</h2>
        <p>今天也要朝着目标前进一步</p>
      </div>
      <div class="hero-date">
        <b>{{ overview?.date ?? '—' }}</b>
      </div>
    </header>

    <template v-if="overview">
      <!-- 三张概览卡 -->
      <div class="card-grid">
        <!-- 今日训练 -->
        <el-card shadow="never" class="overview-card workout-card">
          <div class="card-label">今日训练</div>
          <template v-if="overview.todayWorkout">
            <div class="workout-title">{{ overview.todayWorkout.title }}</div>
            <div class="workout-meta">
              <span v-if="overview.todayWorkout.estimatedMinutes">
                约 {{ overview.todayWorkout.estimatedMinutes }} 分钟
              </span>
              <el-tag
                size="small"
                :type="overview.todayWorkout.status === 'COMPLETED' ? 'success' : 'primary'"
              >
                {{ workoutStatusText[overview.todayWorkout.status] ?? overview.todayWorkout.status }}
              </el-tag>
            </div>
          </template>
          <div v-else class="workout-rest">
            <span>今天没有安排训练</span>
            <small>好好休息也是训练的一部分</small>
          </div>
          <el-button
            class="card-btn"
            size="small"
            plain
            @click="router.push('/workout-session')"
          >
            前往记录
          </el-button>
        </el-card>

        <!-- 营养摄入 -->
        <el-card shadow="never" class="overview-card nutrition-card">
          <div class="card-label">今日营养</div>
          <div class="nutrition-rows">
            <div class="n-row">
              <span class="n-name">热量</span>
              <div class="n-bar">
                <el-progress
                  :percentage="Math.round(calorieRate * 100)"
                  :stroke-width="8"
                  :color="calorieRate >= 0.9 ? '#10b981' : undefined"
                />
              </div>
              <span class="n-val">
                {{ Math.round(overview.nutrition.caloriesConsumed) }}/{{ overview.nutrition.caloriesTarget }} kcal
              </span>
            </div>
            <div class="n-row">
              <span class="n-name">蛋白质</span>
              <div class="n-bar">
                <el-progress
                  :percentage="Math.round(proteinRate * 100)"
                  :stroke-width="8"
                  :color="proteinRate >= 0.9 ? '#10b981' : undefined"
                />
              </div>
              <span class="n-val">
                {{ Math.round(overview.nutrition.proteinConsumedG) }}/{{ overview.nutrition.proteinTargetG }} g
              </span>
            </div>
          </div>
          <el-button class="card-btn" size="small" plain @click="router.push('/nutrition')">
            记录饮食
          </el-button>
        </el-card>

        <!-- 体重与完成率 -->
        <el-card shadow="never" class="overview-card body-card">
          <div class="card-label">身体数据</div>
          <div class="body-rows">
            <div class="b-row">
              <span class="b-name">最新体重</span>
              <span class="b-val">
                {{ overview.latestWeightKg != null ? overview.latestWeightKg.toFixed(1) : '—' }}
                <small>kg</small>
              </span>
            </div>
            <div class="b-row">
              <span class="b-name">体重变化</span>
              <span class="b-val" :class="{ down: (overview.weightChangeKg7d ?? 0) < 0 }">
                {{ weightTrendText }}
              </span>
            </div>
            <div class="b-row">
              <span class="b-name">本周训练完成率</span>
              <span class="b-val">
                {{ Math.round(overview.weeklyWorkoutCompletionRate * 100) }}%
              </span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 趋势图 + 快捷提问 -->
      <div class="bottom-grid">
        <el-card shadow="never" class="trend-card" v-loading="trendLoading">
          <template #header>
            <div class="trend-head">
              <span>近 30 天趋势</span>
              <el-radio-group v-model="trendMetric" size="small" @change="onMetricChange">
                <el-radio-button v-for="(label, key) in METRIC_LABELS" :key="key" :value="key">
                  {{ label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div v-if="trendData && trendData.points.length" ref="chartEl" class="chart" />
          <el-empty v-else description="暂无趋势数据" :image-size="72" />
        </el-card>

        <el-card shadow="never" class="quick-card">
          <template #header>
            <div class="trend-head"><span>问问 AI 教练</span></div>
          </template>
          <div class="quick-list">
            <button
              v-for="q in overview.quickQuestions"
              :key="q"
              class="quick-item"
              @click="askAgent(q)"
            >
              {{ q }}
            </button>
          </div>
          <el-button type="primary" class="agent-btn" @click="router.push('/agent')">
            进入 AI 健康助理
          </el-button>
        </el-card>
      </div>
    </template>

    <el-empty
      v-else-if="!loading"
      description="暂无看板数据，先去建立健康档案吧"
      :image-size="90"
    >
      <el-button type="primary" @click="router.push('/profile')">去建立档案</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.dashboard-page {
  max-width: 1200px;
  margin: 0 auto;
  min-height: 300px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.hero h2 {
  margin: 0 0 6px;
  font-size: 24px;
}
.hero p {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.hero-date {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  padding: 8px 14px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
}

/* 概览卡 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.overview-card {
  display: flex;
  flex-direction: column;
}
.card-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
}
.card-btn {
  align-self: flex-start;
  margin-top: auto;
}

.workout-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
}
.workout-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 14px;
}
.workout-rest {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 14px;
  font-size: 15px;
}
.workout-rest small {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.nutrition-rows {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 14px;
}
.n-row {
  display: grid;
  grid-template-columns: 44px 1fr auto;
  align-items: center;
  gap: 10px;
}
.n-name {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.n-bar {
  min-width: 0;
}
.n-val {
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
}

.body-rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.b-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.b-name {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.b-val {
  font-size: 15px;
  font-weight: 600;
}
.b-val small {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  font-weight: 400;
}
.b-val.down {
  color: var(--el-color-success);
}

/* 趋势 + 快捷提问 */
.bottom-grid {
  display: grid;
  grid-template-columns: 1.8fr 1fr;
  gap: 16px;
}
.trend-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.chart {
  width: 100%;
  height: 300px;
}

.quick-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}
.quick-item {
  text-align: left;
  font-family: inherit;
  font-size: 13px;
  padding: 11px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.2s ease;
}
.quick-item:hover {
  border-color: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.agent-btn {
  width: 100%;
}

@media (max-width: 900px) {
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
