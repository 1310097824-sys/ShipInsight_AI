<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <h2>统计报表</h2>
        <p>集中查看船舶分布地图、AIS 点位地图、船型与风险等级统计、航运节点统计和 AIS 活动分析，并支持导出为 Excel 或 PDF。</p>
      </div>
      <el-space wrap>
        <el-select v-model="days" class="day-filter">
          <el-option v-for="item in dayOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" plain :loading="loading" @click="loadReports">刷新报表</el-button>
        <el-button :loading="exportingExcel" @click="handleExport('excel')">导出 Excel</el-button>
        <el-button :loading="exportingPdf" @click="handleExport('pdf')">导出 PDF</el-button>
      </el-space>
    </section>

    <div class="summary-grid">
      <StatCard eyebrow="船舶档案" :value="summary.totalVesselProfiles" hint="用于判断船舶档案覆盖规模" />
      <StatCard eyebrow="AIS 记录" :value="summary.totalAisRecords" hint="用于判断累计 AIS 数据量" />
      <StatCard eyebrow="航运节点" :value="summary.totalShippingZones" hint="用于判断港口、锚地、航道和重点水域覆盖面" />
      <StatCard eyebrow="活跃用户" :value="summary.totalUsers" hint="当前启用中的系统用户数量" />
    </div>

    <el-row :gutter="18">
      <el-col :lg="12" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <strong>船舶分布地图</strong>
              <div class="panel-header__tools">
                <el-select v-model="selectedRiskLevel" size="small" class="panel-filter">
                  <el-option
                    v-for="option in vesselRiskLevelOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <span>{{ vesselMapMarkers.length }} / {{ vesselDistributionPoints.length }} 个船舶点</span>
              </div>
            </div>
          </template>
          <ReportMapPanel :points="vesselMapMarkers" empty-description="暂无船舶分布点" :height="360" />
        </el-card>
      </el-col>
      <el-col :lg="12" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <strong>AIS 点位地图</strong>
              <span>{{ aisRecordMapMarkers.length }} 个 AIS 点</span>
            </div>
          </template>
          <ReportMapPanel :points="aisRecordMapMarkers" empty-description="暂无 AIS 点位" :height="360" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :lg="8" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>风险等级占比</strong>
          </template>
          <ChartPanel :option="protectionOption" />
        </el-card>
      </el-col>
      <el-col :lg="8" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>航行状态占比</strong>
          </template>
          <ChartPanel :option="iucnOption" />
        </el-card>
      </el-col>
      <el-col :lg="8" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>船型细分占比</strong>
          </template>
          <ChartPanel :option="classOption" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :lg="10" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>船型大类占比</strong>
          </template>
          <ChartPanel :option="phylumOption" />
        </el-card>
      </el-col>
      <el-col :lg="14" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>航运节点统计</strong>
          </template>
          <ChartPanel :option="ecosystemOption" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :lg="14" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>近 {{ days }} 天 AIS 趋势</strong>
          </template>
          <ChartPanel :option="trendOption" />
        </el-card>
      </el-col>
      <el-col :lg="10" :xs="24">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>按人员统计 AIS 记录数</strong>
          </template>
          <ChartPanel :option="observerOption" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import {
  downloadReportExport,
  fetchDashboardSummary,
  fetchShippingZoneStats,
  fetchOperationalStatusDistribution,
  fetchAisRecordActivity,
  fetchAisRecordMapPoints,
  fetchAisRecordTrend,
  fetchRiskLevelDistribution,
  fetchVesselTypeDistribution,
  fetchVesselDistributionPoints,
} from '@/api/reports'
import ChartPanel from '@/components/ChartPanel.vue'
import ReportMapPanel from '@/components/ReportMapPanel.vue'
import StatCard from '@/components/StatCard.vue'
import { listenDataChanged } from '@/utils/dataSync'
import { triggerBlobDownload } from '@/utils/download'
import type {
  AisRecordMapPoint,
  DashboardSummary,
  NameValuePoint,
  ShippingZoneStats,
  VesselDistributionPoint,
} from '@/types/gsmv'

const ALL_RISK_LEVEL = '__ALL__'
const EMPTY_RISK_LEVEL = '__EMPTY__'

const dayOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 },
]

const days = ref(30)
const loading = ref(false)
const exportingExcel = ref(false)
const exportingPdf = ref(false)
const selectedRiskLevel = ref(ALL_RISK_LEVEL)

const summary = ref<DashboardSummary>({
  totalVesselProfiles: 0,
  totalAisRecords: 0,
  totalShippingZones: 0,
  totalUsers: 0,
  recentAisRecordCount: 0,
})
const protectionPoints = ref<NameValuePoint[]>([])
const iucnPoints = ref<NameValuePoint[]>([])
const phylumPoints = ref<NameValuePoint[]>([])
const classPoints = ref<NameValuePoint[]>([])
const shippingZoneStats = ref<ShippingZoneStats[]>([])
const trendPoints = ref<NameValuePoint[]>([])
const recorderPoints = ref<NameValuePoint[]>([])
const vesselDistributionPoints = ref<VesselDistributionPoint[]>([])
const aisRecordMapPoints = ref<AisRecordMapPoint[]>([])

let stopDataSync: (() => void) | undefined

const vesselRiskLevelOptions = computed(() => {
  const optionMap = new Map<string, string>()

  vesselDistributionPoints.value.forEach((item) => {
    const value = normalizeRiskLevel(item.riskLevel)
    if (!optionMap.has(value)) {
      optionMap.set(value, item.riskLevel?.trim() || '未填写风险等级')
    }
  })

  return [
    { label: '全部风险等级', value: ALL_RISK_LEVEL },
    ...Array.from(optionMap.entries()).map(([value, label]) => ({ value, label })),
  ]
})

const filteredVesselDistributionPoints = computed(() =>
  vesselDistributionPoints.value.filter((item) => {
    if (selectedRiskLevel.value === ALL_RISK_LEVEL) {
      return true
    }
    return normalizeRiskLevel(item.riskLevel) === selectedRiskLevel.value
  }),
)

function wrapAxisLabel(value: string, maxChars = 6) {
  if (!value) {
    return ''
  }

  const lines: string[] = []
  for (let index = 0; index < value.length; index += maxChars) {
    lines.push(value.slice(index, index + maxChars))
  }
  return lines.join('\n')
}

function createWrappedAxisLabel(maxChars = 6) {
  return {
    interval: 0,
    rotate: 24,
    margin: 18,
    width: 104,
    lineHeight: 16,
    overflow: 'break' as const,
    formatter: (value: string) => wrapAxisLabel(value, maxChars),
  }
}

const protectionOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['42%', '72%'],
      data: protectionPoints.value,
      itemStyle: { borderRadius: 10 },
    },
  ],
}))

const iucnOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['42%', '72%'],
      data: iucnPoints.value,
      itemStyle: { borderRadius: 10 },
    },
  ],
}))

const phylumOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: phylumPoints.value.map((item) => item.name),
    axisLabel: { interval: 0, rotate: 18 },
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      data: phylumPoints.value.map((item) => item.value),
      itemStyle: { color: '#00e5ff', borderRadius: [8, 8, 0, 0] },
    },
  ],
  grid: { left: 36, right: 20, top: 28, bottom: 48 },
}))

const classOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: classPoints.value.map((item) => item.name),
    axisLabel: { interval: 0, rotate: 18 },
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      data: classPoints.value.map((item) => item.value),
      itemStyle: { color: '#20ff9f', borderRadius: [8, 8, 0, 0] },
    },
  ],
  grid: { left: 36, right: 20, top: 28, bottom: 48 },
}))

const ecosystemOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: ['AIS 记录数', '船型/目标数'],
    top: 6,
    textStyle: { color: '#d8eee7' },
  },
  xAxis: {
    type: 'category',
    data: shippingZoneStats.value.map((item) => item.zoneName),
    axisLabel: createWrappedAxisLabel(),
    axisTick: { alignWithLabel: true },
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: 'AIS 记录数',
      type: 'bar',
      data: shippingZoneStats.value.map((item) => item.recordCount),
      itemStyle: { color: '#00e5ff', borderRadius: [8, 8, 0, 0] },
    },
    {
      name: '船型/目标数',
      type: 'bar',
      data: shippingZoneStats.value.map((item) => item.linkedVesselCount),
      itemStyle: { color: '#20ff9f', borderRadius: [8, 8, 0, 0] },
    },
  ],
  grid: { left: 36, right: 20, top: 78, bottom: 104, containLabel: true },
}))

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: trendPoints.value.map((item) => item.name),
    boundaryGap: false,
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'line',
      smooth: true,
      data: trendPoints.value.map((item) => item.value),
      lineStyle: { color: '#00e5ff', width: 3 },
      areaStyle: { color: 'rgba(0, 229, 255, 0.18)' },
    },
  ],
  grid: { left: 36, right: 20, top: 28, bottom: 28 },
}))

const observerOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: recorderPoints.value.map((item) => item.name),
    axisLabel: { interval: 0, rotate: 18 },
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      data: recorderPoints.value.map((item) => item.value),
      itemStyle: { color: '#7c3cff', borderRadius: [8, 8, 0, 0] },
    },
  ],
  grid: { left: 36, right: 20, top: 28, bottom: 48 },
}))

const vesselMapMarkers = computed(() =>
  filteredVesselDistributionPoints.value.map((item) => ({
    id: item.vesselId,
    lat: item.locationLat,
    lng: item.locationLng,
    title: item.displayName || item.vesselName,
    subtitle: item.displayName ? item.vesselName : '',
    lines: [
      `地理范围：${item.routeDescription || '未填写'}`,
      `风险等级：${item.riskLevel || '未填写'}`,
      `航行状态：${item.operationalStatus || '未填写'}`,
    ],
  })),
)

const aisRecordMapMarkers = computed(() =>
  aisRecordMapPoints.value.map((item) => ({
    id: item.recordId,
    lat: item.locationLat,
    lng: item.locationLng,
    title: item.locationName || item.shippingZoneName,
    subtitle: `${item.shippingZoneName} / ${item.recorderName}`,
    lines: [item.recordedAt, `关联目标 ${item.linkedVesselCount} 个`, item.note || '无备注'],
  })),
)

async function loadReports() {
  loading.value = true
  try {
    const [
      summaryData,
      protection,
      iucn,
      phylum,
      classes,
      ecosystem,
      trend,
      observers,
      speciesPoints,
      observationPoints,
    ] = await Promise.all([
      fetchDashboardSummary(),
      fetchRiskLevelDistribution(),
      fetchOperationalStatusDistribution(),
      fetchVesselTypeDistribution('phylum'),
      fetchVesselTypeDistribution('class'),
      fetchShippingZoneStats(),
      fetchAisRecordTrend(days.value),
      fetchAisRecordActivity(days.value),
      fetchVesselDistributionPoints(),
      fetchAisRecordMapPoints(),
    ])

    summary.value = summaryData
    protectionPoints.value = protection
    iucnPoints.value = iucn
    phylumPoints.value = phylum
    classPoints.value = classes
    shippingZoneStats.value = ecosystem
    trendPoints.value = trend
    recorderPoints.value = observers
    vesselDistributionPoints.value = speciesPoints
    aisRecordMapPoints.value = observationPoints

    if (!vesselRiskLevelOptions.value.some((option) => option.value === selectedRiskLevel.value)) {
      selectedRiskLevel.value = ALL_RISK_LEVEL
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报表加载失败')
  } finally {
    loading.value = false
  }
}

async function handleExport(format: 'excel' | 'pdf') {
  const loadingRef = format === 'excel' ? exportingExcel : exportingPdf
  loadingRef.value = true

  try {
    const { blob, fileName } = await downloadReportExport(format, days.value)
    triggerBlobDownload(blob, fileName)
    ElMessage.success(format === 'excel' ? 'Excel 报表已开始下载' : 'PDF 报表已开始下载')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  } finally {
    loadingRef.value = false
  }
}

function normalizeRiskLevel(value?: string) {
  const trimmed = value?.trim()
  return trimmed || EMPTY_RISK_LEVEL
}

onMounted(() => {
  stopDataSync = listenDataChanged((detail) => {
    if (['vesselProfile', 'aisRecord', 'shippingZone', 'user'].includes(detail.type)) {
      void loadReports()
    }
  })
  void loadReports()
})

onBeforeUnmount(() => {
  stopDataSync?.()
})
</script>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 18px;
}

.day-filter {
  width: 140px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.panel-header strong {
  font-size: 16px;
  letter-spacing: 0.01em;
}

.panel-header span {
  color: var(--gsmv-muted);
  font-size: 13px;
}

.panel-header__tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.panel-filter {
  width: 180px;
}

@media (max-width: 720px) {
  .panel-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-header__tools {
    width: 100%;
    justify-content: space-between;
  }

  .panel-filter,
  .day-filter {
    width: 100%;
    max-width: 220px;
  }
}
</style>
