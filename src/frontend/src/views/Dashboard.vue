<script setup>
// 首页看板：6 个统计卡片 + 近 6 个月新增宠物柱状图 + 免疫到期分布饼图 + 7 天内到期提醒表格
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Star,
  User,
  Clock,
  House,
  Present,
  TrendCharts,
  Refresh
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getStatOverview, getUpcomingVaccines } from '@/api'

// 按需注册 ECharts 模块：只打包用到的柱状图、饼图和画布渲染器，减小体积
use([CanvasRenderer, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const overview = ref(null)
const overviewError = ref(false)
const loadingOverview = ref(false)
const upcoming = ref([])
const loadingUpcoming = ref(false)

// 6 个统计卡片的数据；接口没回来时显示 "—"，不伪造 0
const cards = computed(() => {
  const o = overview.value
  return [
    {
      label: '宠物总数',
      value: o ? o.petTotal : '—',
      sub: '已建档宠物（只）',
      icon: Star,
      color: '#409eff'
    },
    {
      label: '主人总数',
      value: o ? o.ownerTotal : '—',
      sub: '已登记主人（位）',
      icon: User,
      color: '#67c23a'
    },
    {
      label: '待审核寄养',
      value: o ? o.fosterPending : '—',
      sub: '等待工作人员审核（单）',
      icon: Clock,
      color: '#e6a23c'
    },
    {
      label: '寄养中',
      value: o ? o.fosterActive : '—',
      sub: '当前在店寄养（单）',
      icon: House,
      color: '#909399'
    },
    {
      label: '待审核领养',
      value: o ? o.adoptionPending : '—',
      sub: '等待审核的领养申请（单）',
      icon: Present,
      color: '#f56c6c'
    },
    {
      label: '领养完成率',
      value: o ? `${o.adoptionRate ?? 0}%` : '—',
      sub: o ? `已通过 ${o.adoptionApproved} / 共 ${o.adoptionTotal} 单` : '已通过申请占比',
      icon: TrendCharts,
      color: '#7b5cf0'
    }
  ]
})

// 柱状图配置：月份做横轴，新增数量做纵轴
const monthlyData = computed(() => overview.value?.monthlyPets || [])
const barOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 45, right: 20, top: 35, bottom: 30 },
  xAxis: {
    type: 'category',
    data: monthlyData.value.map((item) => item.month),
    axisTick: { alignWithLabel: true }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '新增宠物',
      type: 'bar',
      barWidth: '45%',
      data: monthlyData.value.map((item) => item.total),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }
  ]
}))

// 饼图配置：逾期 / 7 天内 / 30 天内 / 更久
const dueData = computed(() => {
  const d = overview.value?.dueDistribution || {}
  return [
    { name: '已逾期', value: d.overdue || 0, itemStyle: { color: '#f56c6c' } },
    { name: '7 天内', value: d.in7 || 0, itemStyle: { color: '#e6a23c' } },
    { name: '30 天内', value: d.in30 || 0, itemStyle: { color: '#409eff' } },
    { name: '更久', value: d.later || 0, itemStyle: { color: '#67c23a' } }
  ]
})
const hasDueData = computed(() => dueData.value.some((item) => item.value > 0))
const pieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
  legend: { bottom: 0, icon: 'circle' },
  series: [
    {
      name: '免疫到期分布',
      type: 'pie',
      radius: ['42%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      label: { formatter: '{b}\n{c} 条' },
      data: dueData.value
    }
  ]
}))

// 到期天数的标签颜色：负数已逾期用红色，0 天和 7 天内用橙色
function daysTagType(daysLeft) {
  if (daysLeft === null || daysLeft === undefined) return 'info'
  if (daysLeft <= 0) return 'danger'
  if (daysLeft <= 7) return 'warning'
  return 'info'
}

function daysText(daysLeft) {
  if (daysLeft === null || daysLeft === undefined) return '—'
  if (daysLeft < 0) return `已逾期 ${-daysLeft} 天`
  if (daysLeft === 0) return '今天到期'
  return `${daysLeft} 天后到期`
}

// 加载看板统计
async function loadOverview() {
  loadingOverview.value = true
  overviewError.value = false
  try {
    overview.value = await getStatOverview()
  } catch (e) {
    // 请求层已经弹过错误提示，这里只记录失败状态用于显示错误条
    overviewError.value = true
  } finally {
    loadingOverview.value = false
  }
}

// 加载 7 天内到期免疫提醒
async function loadUpcoming() {
  loadingUpcoming.value = true
  try {
    const data = await getUpcomingVaccines(7)
    upcoming.value = Array.isArray(data) ? data : []
  } catch (e) {
    upcoming.value = []
  } finally {
    loadingUpcoming.value = false
  }
}

function loadAll() {
  loadOverview()
  loadUpcoming()
}

onMounted(() => {
  loadAll()
})

// 刷新后给个轻提示，让操作有反馈
async function handleRefresh() {
  await Promise.all([loadOverview(), loadUpcoming()])
  if (!overviewError.value) {
    ElMessage.success('看板数据已刷新')
  }
}
</script>

<template>
  <div class="page-container dashboard">
    <div class="dashboard-header">
      <div>
        <h3 class="dashboard-title">数据概览</h3>
        <p class="form-tip">数据来自后端 /stat/overview 接口，页面每次刷新都重新统计</p>
      </div>
      <el-button :icon="Refresh" :loading="loadingOverview" @click="handleRefresh">
        刷新数据
      </el-button>
    </div>

    <!-- 看板接口失败时明确提示，不显示假数据 -->
    <el-alert
      v-if="overviewError"
      title="看板数据加载失败，请确认后端已在 8080 端口启动，然后点击右上角刷新"
      type="error"
      :closable="false"
      show-icon
      class="dashboard-alert"
    />

    <!-- 6 个统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="card in cards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div
              class="stat-icon"
              :style="{ backgroundColor: card.color + '1a', color: card.color }"
            >
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
              <div class="stat-sub">{{ card.sub }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 两个图表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">近 6 个月新增宠物</span>
          </template>
          <div v-loading="loadingOverview" class="chart-box">
            <VChart
              v-if="overview && monthlyData.length"
              :option="barOption"
              autoresize
              class="chart"
            />
            <el-empty
              v-else-if="!loadingOverview"
              description="暂无月度新增数据"
              :image-size="80"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">免疫到期分布</span>
          </template>
          <div v-loading="loadingOverview" class="chart-box">
            <VChart
              v-if="overview && hasDueData"
              :option="pieOption"
              autoresize
              class="chart"
            />
            <el-empty
              v-else-if="!loadingOverview"
              description="暂无免疫到期数据"
              :image-size="80"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 7 天内到期提醒 -->
    <el-card shadow="never" class="upcoming-card">
      <template #header>
        <div class="upcoming-header">
          <span class="card-title">7 天内到期免疫提醒</span>
          <el-tag v-if="upcoming.length" type="warning" effect="light">
            共 {{ upcoming.length }} 条
          </el-tag>
        </div>
      </template>

      <el-table v-loading="loadingUpcoming" :data="upcoming" border stripe>
        <el-table-column prop="petName" label="宠物名称" min-width="110" />
        <el-table-column prop="ownerName" label="主人" min-width="110" />
        <el-table-column prop="vaccineType" label="疫苗类型" min-width="140" />
        <el-table-column prop="inoculateDate" label="接种日期" width="120" align="center" />
        <el-table-column prop="nextDueDate" label="下次到期日" width="120" align="center" />
        <el-table-column label="剩余天数" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="daysTagType(row.daysLeft)" effect="light">
              {{ daysText(row.daysLeft) }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="近 7 天没有到期的免疫记录" :image-size="70" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.dashboard-title {
  margin: 0 0 4px;
  font-size: 18px;
}

.dashboard-alert {
  margin-bottom: 16px;
}

.stat-row {
  margin-bottom: 4px;
}

.stat-card {
  margin-bottom: 16px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #606266;
}

.stat-value {
  margin: 2px 0;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-sub {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chart-row .el-card {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  color: #303133;
}

.chart-box {
  height: 320px;
}

.chart {
  width: 100%;
  height: 100%;
}

.upcoming-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
