<template>
  <div class="page-shell dashboard-page">
    <section class="dashboard-command-grid">
      <article class="dashboard-window dashboard-map-window">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">AIS Situation Screen · Live Maritime Map</span>
            <strong>湛江近海 / 北部湾船舶交通态势大屏</strong>
            <p>围绕 AIS 最新快照、重点航路、低速目标和风险备注，快速查看当天航线态势与重点船舶动态。</p>
          </div>
          <div class="dashboard-map-modes" role="group" aria-label="地图图层">
            <button
              v-for="mode in mapModes"
              :key="mode.value"
              type="button"
              :class="{ 'is-active': selectedMapMode === mode.value }"
              @click="selectedMapMode = mode.value"
            >
              {{ mode.label }}
            </button>
          </div>
        </header>

        <div class="dashboard-map-toolbar">
          <el-select v-model="selectedDatasetDate" placeholder="选择 AIS 数据日期" clearable class="dashboard-map-toolbar__date">
            <el-option v-for="item in datasetDates" :key="item" :label="item" :value="item" />
          </el-select>
          <el-button plain :disabled="!hasPreviousDate" @click="selectAdjacentDate(-1)">上一日</el-button>
          <el-button plain :disabled="!hasNextDate" @click="selectAdjacentDate(1)">下一日</el-button>
          <el-button
            type="primary"
            :loading="loading"
            :class="{ 'is-playing': playingTimeline }"
            @click="toggleTimelinePlayback"
          >
            {{ playingTimeline ? '暂停轮播' : '按天轮播' }}
          </el-button>
          <el-button plain :disabled="!selectedDatasetDate" @click="resetSelectedDate">回到最新</el-button>
          <el-button plain :loading="loading" @click="loadOverview">刷新态势</el-button>
          <el-select v-model="playbackSpeed" class="dashboard-map-toolbar__speed" :disabled="playingTimeline">
            <el-option v-for="item in playbackSpeedOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div class="spacer" />
          <div class="dashboard-map-toolbar__snapshot">
            <span>当前快照：{{ activeDatasetLabel }}</span>
            <small :class="compareSummaryClass">{{ compareSummary }}</small>
          </div>
        </div>

        <div class="dashboard-map-stage">
          <div ref="mapRef" class="dashboard-map" />
          <div class="dashboard-map-stage__scan" />
          <div class="dashboard-map-stage__compass">
            <span>N</span>
            <strong>北部湾</strong>
          </div>
          <div class="dashboard-map-legend">
            <span><i class="legend-dot legend-dot--route" />主航路</span>
            <span><i class="legend-dot legend-dot--heat" />低速聚集</span>
            <span><i class="legend-dot legend-dot--risk" />风险备注</span>
            <span><i class="legend-dot legend-dot--ship" />AIS 船位</span>
          </div>
        </div>
      </article>

      <aside class="dashboard-intel-rail">
        <section class="dashboard-window dashboard-window__column dashboard-score-panel">
          <header class="dashboard-window__header dashboard-window__header--compact">
            <span class="dashboard-window__eyebrow">Situation Score</span>
            <strong>综合态势指数</strong>
          </header>
          <div class="dashboard-score-panel__body">
            <strong>{{ situationScore }}</strong>
            <p>{{ scoreDescription }}</p>
            <div class="dashboard-hero__pulse">
              <span />
              <span />
              <span />
            </div>
          </div>
        </section>

        <section class="dashboard-window dashboard-window__column dashboard-weather-panel">
          <header class="dashboard-window__header dashboard-window__header--compact">
            <span class="dashboard-window__eyebrow">Weather Analysis</span>
            <strong>今日航海天气 · {{ weatherCity }}</strong>
            <span class="dashboard-weather-panel__time">{{ weatherTime }}</span>
          </header>
          <div class="dashboard-weather-panel__body">
            <div v-if="weatherLoading" class="dashboard-weather-panel__status">
              <span class="dashboard-weather-panel__spin" />正在获取天气数据...
            </div>
            <div v-else-if="weatherError" class="dashboard-weather-panel__status dashboard-weather-panel__error">
              ⚠️ {{ weatherError }}
            </div>
            <div v-else-if="weatherResult" class="dashboard-weather-panel__result">
              <div class="dashboard-weather-panel__interpretation">
                <strong>AI 出海建议</strong>
                <p>{{ weatherResult.aiInterpretation || '暂无解读数据' }}</p>
              </div>
            <div v-if="formattedWeatherData" class="dashboard-weather-panel__raw">
              <strong>实时天气数据</strong>
              <pre>{{ formattedWeatherData }}</pre>
            </div>
            </div>
            <p class="dashboard-weather-panel__hint">
              💡 如果想要获取别的地方的天气，可以试试在<router-link to="/quiz/ai">知识问答模块</router-link>中询问 AI 助手哦！
            </p>
          </div>
        </section>

        <section class="dashboard-window dashboard-window__column">
          <header class="dashboard-window__header dashboard-window__header--compact">
            <span class="dashboard-window__eyebrow">Focus Vessel</span>
            <strong>当前焦点船舶</strong>
          </header>
          <div v-if="focusRecord" class="dashboard-focus-vessel">
            <div class="dashboard-focus-vessel__header">
              <div>
                <span>MMSI {{ focusRecord.mmsi }}</span>
                <strong>{{ vesselName(focusRecord) }}</strong>
              </div>
              <el-tag effect="plain">{{ focusRecord.imo || focusRecord.callSign || '未登记' }}</el-tag>
            </div>
            <div class="dashboard-focus-vessel__meta">
              <div>
                <small>接收时间</small>
                <strong>{{ displayTime(focusRecord.baseDateTime) }}</strong>
              </div>
              <div>
                <small>坐标</small>
                <strong>{{ positionLabel(focusRecord) }}</strong>
              </div>
              <div>
                <small>航速</small>
                <strong>{{ metricLabel(focusRecord.sog, 'kn') }}</strong>
              </div>
              <div>
                <small>航向</small>
                <strong>{{ metricLabel(focusRecord.cog, '°') }}</strong>
              </div>
            </div>
          </div>
          <el-empty v-else description="当前没有可展示的 AIS 焦点船舶" />
        </section>

        <section class="dashboard-window dashboard-window__column">
          <header class="dashboard-window__header dashboard-window__header--compact">
            <span class="dashboard-window__eyebrow">Watch List</span>
            <strong>态势预警</strong>
          </header>
          <div class="dashboard-alert-list">
            <article v-for="alert in alerts" :key="alert.title" class="dashboard-alert" :class="`is-${alert.level}`">
              <span>{{ alert.levelLabel }}</span>
              <strong>{{ alert.title }}</strong>
              <p>{{ alert.detail }}</p>
            </article>
          </div>
        </section>
      </aside>
    </section>

    <section class="dashboard-kpi-grid" aria-label="态势指标">
      <article
        v-for="item in kpiCards"
        :key="item.label"
        class="dashboard-window__metric"
        :class="{ 'is-interactive': !!item.mapMode, 'is-active': item.mapMode ? selectedMapMode === item.mapMode : false }"
        @click="handleKpiCardClick(item)"
      >
        <div class="dashboard-window__metric-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <p>{{ item.detail }}</p>
        </div>
        <em>{{ item.trend }}</em>
      </article>
    </section>

    <section class="dashboard-lower-grid">
      <article class="dashboard-window dashboard-window__column">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">Route Focus</span>
            <strong>重点水域与主航路</strong>
          </div>
        </header>
        <div class="dashboard-route-list">
          <article v-for="zone in zoneInsights" :key="zone.id" class="dashboard-route-card">
            <div class="dashboard-route-card__line" :style="{ background: zone.color }" />
            <div>
              <strong>{{ zone.name }}</strong>
              <p>{{ zone.detail }}</p>
            </div>
            <span>{{ zone.count }} 点 / {{ zone.avgSpeed }}</span>
          </article>
        </div>
      </article>

      <article class="dashboard-window dashboard-chart-window">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">AIS Trend</span>
            <strong>近 30 个数据日 AIS 活跃趋势</strong>
          </div>
        </header>
        <ChartPanel class="dashboard-compact-chart" :option="trendOption" />
      </article>

      <article class="dashboard-window dashboard-chart-window">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">Importer Load</span>
            <strong>AIS 录入活跃度</strong>
          </div>
        </header>
        <ChartPanel class="dashboard-compact-chart" :option="importerOption" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import 'leaflet.markercluster'
import { Aim, DataAnalysis, LocationFilled, Monitor, Ship, WarningFilled } from '@element-plus/icons-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import {
  fetchAisDatasetDates,
  fetchAisDatasetDateStats,
  fetchAisImporterStats,
  fetchAisMapRecords,
  fetchAisRiskSummary,
} from '@/api/aisRecords'
import { fetchAllShippingZones } from '@/api/ecosystems'
import { fetchWeatherInterpret } from '@/api/quiz'
import ChartPanel from '@/components/ChartPanel.vue'
import { useAuthStore } from '@/stores/auth'
import { addPreferredTileLayer, toMapDisplayPoint } from '@/utils/mapProvider'
import { buildMapPopupCard, createMapMarkerIcon } from '@/utils/mapMarkerTheme'
import type {
  AisDatasetDateStat,
  AisRankingStat,
  AisRecordView,
  AisRiskSummary,
  ShippingZone,
} from '@/types/gsmv'

type MapMode = 'all' | 'traffic' | 'slow' | 'risk'
type MarkerTone = 'aqua' | 'emerald' | 'violet'

interface FocusZone {
  id: string
  name: string
  kind: string
  lat: number
  lng: number
  radiusMeters: number
  color: string
  tone: MarkerTone
  detail: string
}

interface RouteSignal {
  id: string
  name: string
  description: string
  path: Array<[number, number]>
  color: string
}

interface ZoneInsight {
  id: string
  name: string
  detail: string
  count: number
  avgSpeed: string
  slowCount: number
  color: string
}

interface DashboardAlert {
  level: 'critical' | 'warning' | 'stable'
  levelLabel: string
  title: string
  detail: string
}

interface KpiCard {
  label: string
  value: string
  detail: string
  trend: string
  icon: typeof Ship
  mapMode?: MapMode | null
}

const MAP_LIMIT = 5000
const defaultCenter: [number, number] = [21.18, 110.53]
const authStore = useAuthStore()

const mapModes: Array<{ label: string; value: MapMode }> = [
  { label: '综合态势', value: 'all' },
  { label: '主航路流量', value: 'traffic' },
  { label: '低速目标', value: 'slow' },
  { label: '风险备注', value: 'risk' },
]

const focusZones: FocusZone[] = [
  {
    id: 'zhanjiang-port',
    name: '湛江港进出航道',
    kind: 'PORT GATE',
    lat: 21.1829,
    lng: 110.5344,
    radiusMeters: 18000,
    color: '#00e5ff',
    tone: 'aqua',
    detail: '港口入口、近岸锚地与拖带活动的主要交汇带。',
  },
  {
    id: 'qiongzhou-strait',
    name: '琼州海峡通道',
    kind: 'STRAIT',
    lat: 20.265,
    lng: 110.42,
    radiusMeters: 32000,
    color: '#20ff9f',
    tone: 'emerald',
    detail: '连接北部湾与南海东侧水域的密集过境通道。',
  },
  {
    id: 'beibu-gulf',
    name: '北部湾近岸带',
    kind: 'COASTAL',
    lat: 21.58,
    lng: 109.92,
    radiusMeters: 36000,
    color: '#ffb84d',
    tone: 'violet',
    detail: '近岸补给、沿海支线与区域渔运混行的重点海域。',
  },
]

const routeSignals: RouteSignal[] = [
  {
    id: 'route-zhanjiang-port',
    name: '湛江港进出港线',
    description: '连接港区、近岸锚地与外海主通道的核心进出港航路。',
    path: [
      [21.33, 110.72],
      [21.26, 110.63],
      [21.22, 110.58],
      [21.1829, 110.5344],
    ],
    color: '#00e5ff',
  },
  {
    id: 'route-qiongzhou',
    name: '琼州海峡主通道',
    description: '横向穿越海峡的主要过境航路，适合观察速度分化与通行密度。',
    path: [
      [20.31, 110.72],
      [20.27, 110.55],
      [20.23, 110.35],
      [20.21, 110.12],
    ],
    color: '#20ff9f',
  },
  {
    id: 'route-beibu',
    name: '北部湾沿海航路',
    description: '沿近岸连接北部湾多个港口和作业海域的支线航路。',
    path: [
      [21.82, 109.75],
      [21.64, 109.9],
      [21.42, 110.16],
      [21.22, 110.44],
    ],
    color: '#ffb84d',
  },
]

const mapRef = ref<HTMLDivElement>()
const loading = ref(false)
const selectedMapMode = ref<MapMode>('all')
const selectedDatasetDate = ref('')
const playingTimeline = ref(false)
const playbackSpeed = ref(3500)
const datasetDates = ref<string[]>([])
const records = ref<AisRecordView[]>([])
const selectedRecordId = ref('')
const riskSummary = ref<AisRiskSummary>({
  total: 0,
  lowSpeedCount: 0,
  stoppedCount: 0,
  abnormalNoteCount: 0,
  uniqueVesselCount: 0,
})
const datasetDateStats = ref<AisDatasetDateStat[]>([])
const importerStats = ref<AisRankingStat[]>([])
const shippingZones = ref<ShippingZone[]>([])
const canReadAis = computed(() => hasAuthority('OBS_READ'))
const canReadShippingZones = computed(() => hasAuthority('ECOSYSTEM_READ'))

let map: L.Map | null = null
let markerLayer: L.MarkerClusterGroup | null = null
let overlayLayer: L.LayerGroup | null = null
let resizeObserver: ResizeObserver | null = null
let playbackTimer: ReturnType<typeof window.setInterval> | null = null

// ==================== 天气模块 ====================
const weatherCity = ref('湛江')
const weatherTime = ref('')
const weatherLoading = ref(true)
const weatherError = ref<string | null>(null)
const weatherResult = ref<Record<string, unknown> | null>(null)
let weatherTimer: ReturnType<typeof setInterval> | null = null

const formattedWeatherData = computed(() => {
  const raw = weatherResult.value?.weatherData
  if (!raw || typeof raw !== 'string') return ''
  try {
    const parsed = JSON.parse(raw)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return raw
  }
})

function updateWeatherTime() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  weatherTime.value = `${y}-${m}-${d} ${h}:${min}:${s}`
}

async function loadWeather() {
  weatherLoading.value = true
  weatherError.value = null
  try {
    const data = await fetchWeatherInterpret(weatherCity.value)
    if (!data.akConfigured) {
      weatherError.value = String(data.error || '百度地图 AK 未配置')
    } else if (data.error) {
      weatherError.value = String(data.error)
    }
    weatherResult.value = data as unknown as Record<string, unknown>
  } catch (e) {
    weatherError.value = e instanceof Error ? e.message : '天气数据加载失败'
  } finally {
    weatherLoading.value = false
  }
}

const markerMap = new Map<string, L.Marker>()
const playbackSpeedOptions = [
  { label: '轮播 2 秒/日', value: 2000 },
  { label: '轮播 3.5 秒/日', value: 3500 },
  { label: '轮播 5 秒/日', value: 5000 },
]

const activeDatasetLabel = computed(() => selectedDatasetDate.value || datasetDates.value[0] || '最新数据集')
const activeDatasetIndex = computed(() => {
  const current = selectedDatasetDate.value || datasetDates.value[0] || ''
  return datasetDates.value.findIndex((item) => item === current)
})
const hasPreviousDate = computed(() => activeDatasetIndex.value >= 0 && activeDatasetIndex.value < datasetDates.value.length - 1)
const hasNextDate = computed(() => activeDatasetIndex.value > 0)
const activeRecordCount = computed(() => datasetDateStats.value.find((item) => item.datasetDate === activeDatasetLabel.value)?.recordCount || 0)
const previousDatasetStat = computed(() => {
  const currentIndex = datasetDateStats.value.findIndex((item) => item.datasetDate === activeDatasetLabel.value)
  if (currentIndex < 0 || currentIndex >= datasetDateStats.value.length - 1) {
    return null
  }
  return datasetDateStats.value[currentIndex + 1]
})
const compareSummary = computed(() => {
  const previous = previousDatasetStat.value
  if (!previous) {
    return activeRecordCount.value ? `当前共 ${activeRecordCount.value} 条 AIS 记录` : '当前暂无上一日对比'
  }
  const diff = activeRecordCount.value - previous.recordCount
  const sign = diff > 0 ? '+' : ''
  return `${previous.datasetDate} 对比 ${sign}${diff} 条`
})
const compareSummaryClass = computed(() => {
  const previous = previousDatasetStat.value
  if (!previous) {
    return 'is-neutral'
  }
  const diff = activeRecordCount.value - previous.recordCount
  if (diff > 0) return 'is-up'
  if (diff < 0) return 'is-down'
  return 'is-neutral'
})
const latestRecord = computed(() => records.value[0] || null)
const focusRecord = computed(() => records.value.find((item) => item.id === selectedRecordId.value) || latestRecord.value)
const filteredRecords = computed(() => {
  switch (selectedMapMode.value) {
    case 'slow':
      return records.value.filter((item) => isSlowRecord(item))
    case 'risk':
      return records.value.filter((item) => hasRiskNote(item) || isSlowRecord(item))
    case 'traffic':
      return records.value.filter((item) => isActiveRecord(item))
    default:
      return records.value
  }
})

const situationScore = computed(() => {
  const vesselBase = Math.min(20, Math.round(riskSummary.value.uniqueVesselCount / 6))
  const penalty = Math.min(18, riskSummary.value.abnormalNoteCount * 2 + Math.round(riskSummary.value.stoppedCount / 2))
  return Math.max(42, Math.min(96, 74 + vesselBase - penalty))
})

const scoreDescription = computed(() => {
  if (riskSummary.value.abnormalNoteCount > 0) {
    return `当前存在 ${riskSummary.value.abnormalNoteCount} 条带风险备注的 AIS 记录，建议优先复核。`
  }
  if (riskSummary.value.stoppedCount > 0) {
    return `当前有 ${riskSummary.value.stoppedCount} 条疑似停泊/停滞记录，需结合航线位置判断是否异常。`
  }
  return '当前快照以正常通行和低风险流动为主，未见明显异常备注聚集。'
})

const kpiCards = computed<KpiCard[]>(() => [
  {
    label: '快照船舶',
    value: `${filteredRecords.value.length}`,
    detail: `${activeDatasetLabel.value} 当前地图层里可见的 AIS 最新点位`,
    trend: selectedMapMode.value === 'all' ? 'Live' : mapModes.find((item) => item.value === selectedMapMode.value)?.label || '筛选中',
    icon: Ship,
    mapMode: 'all',
  },
  {
    label: 'AIS 记录总量',
    value: `${riskSummary.value.total}`,
    detail: '系统内累计纳入态势研判的 AIS 记录总数',
    trend: datasetDateStats.value[0] ? `${datasetDateStats.value[0].recordCount} / 最新日` : '统计中',
    icon: DataAnalysis,
    mapMode: null,
  },
  {
    label: '低速目标',
    value: `${riskSummary.value.lowSpeedCount}`,
    detail: '航速低于 1 kn 的目标，用于观察等待、漂移与近停状态',
    trend: riskSummary.value.lowSpeedCount > 0 ? '关注' : '平稳',
    icon: Aim,
    mapMode: 'slow',
  },
  {
    label: '疑似停泊',
    value: `${riskSummary.value.stoppedCount}`,
    detail: '锚泊、系泊或近静止状态的 AIS 记录数',
    trend: riskSummary.value.stoppedCount > 0 ? '复核' : '正常',
    icon: Monitor,
    mapMode: 'slow',
  },
  {
    label: '风险备注',
    value: `${riskSummary.value.abnormalNoteCount}`,
    detail: '备注中包含异常、风险、告警或 warning 等关键词的记录',
    trend: riskSummary.value.abnormalNoteCount > 0 ? '高亮' : '无新增',
    icon: WarningFilled,
    mapMode: 'risk',
  },
  {
    label: '航运节点',
    value: `${shippingZones.value.length}`,
    detail: '已建档的港口、锚地、主航道与重点水域节点数量',
    trend: '底座',
    icon: LocationFilled,
    mapMode: 'traffic',
  },
])

const zoneInsights = computed<ZoneInsight[]>(() =>
  focusZones.map((zone) => {
    const items = filteredRecords.value.filter((record) => distanceMeters(record.latitude, record.longitude, zone.lat, zone.lng) <= zone.radiusMeters)
    const speeds = items.map((record) => record.sog).filter((value): value is number => typeof value === 'number')
    const avgSpeed = speeds.length ? `${(speeds.reduce((sum, value) => sum + value, 0) / speeds.length).toFixed(1)} kn` : '-'
    const slowCount = items.filter((item) => isSlowRecord(item)).length
    return {
      id: zone.id,
      name: zone.name,
      detail: `${zone.detail}${slowCount ? ` 当前低速目标 ${slowCount} 条。` : ' 当前未见明显低速聚集。'}`,
      count: items.length,
      avgSpeed,
      slowCount,
      color: zone.color,
    }
  }),
)

const alerts = computed<DashboardAlert[]>(() => {
  const busiestZone = [...zoneInsights.value].sort((a, b) => b.count - a.count)[0]
  return [
    riskSummary.value.abnormalNoteCount > 0
      ? {
          level: 'critical',
          levelLabel: '高',
          title: '发现带风险备注的 AIS 记录',
          detail: `当前累计 ${riskSummary.value.abnormalNoteCount} 条带风险/异常备注的记录，建议在 AIS 记录页重点核验。`,
        }
      : {
          level: 'stable',
          levelLabel: '稳',
          title: '未发现新增风险备注聚集',
          detail: '当前备注字段中未见显著异常、风险或 warning 关键词堆积。',
        },
    riskSummary.value.stoppedCount > 0
      ? {
          level: 'warning',
          levelLabel: '中',
          title: '低速或疑似停泊目标较多',
          detail: `当前有 ${riskSummary.value.stoppedCount} 条停泊/近静止记录，需结合港区、锚地与航道位置判断是否异常。`,
        }
      : {
          level: 'stable',
          levelLabel: '稳',
          title: '停泊目标保持可解释范围',
          detail: '当前停泊/近静止记录未见异常放大，可继续观察下一轮数据更新。',
        },
    busiestZone
      ? {
          level: busiestZone.count >= 8 ? 'warning' : 'stable',
          levelLabel: busiestZone.count >= 8 ? '中' : '稳',
          title: `${busiestZone.name} 活跃度最高`,
          detail: `该区域当前聚集 ${busiestZone.count} 个最新点位，平均航速 ${busiestZone.avgSpeed}。`,
        }
      : {
          level: 'stable',
          levelLabel: '稳',
          title: '当前无区域聚集信号',
          detail: '地图快照暂无足够点位生成区域焦点判断。',
        },
  ]
})

const trendOption = computed<EChartsOption>(() => {
  const items = [...datasetDateStats.value].slice(0, 30).reverse()
  return {
    color: ['#00e5ff'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(6, 10, 24, 0.92)',
      borderColor: 'rgba(0, 229, 255, 0.24)',
      textStyle: { color: '#e8f3ff' },
    },
    grid: { left: 36, right: 18, top: 34, bottom: 34 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: items.map((item) => item.datasetDate.slice(5)),
      axisLine: { lineStyle: { color: 'rgba(232, 243, 255, 0.22)' } },
      axisTick: { show: false },
      axisLabel: { color: 'rgba(232, 243, 255, 0.62)' },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(232, 243, 255, 0.12)' } },
      axisLabel: { color: 'rgba(232, 243, 255, 0.62)' },
    },
    series: [
      {
        name: 'AIS 记录数',
        data: items.map((item) => item.recordCount),
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        areaStyle: { color: 'rgba(0, 229, 255, 0.18)' },
        lineStyle: { color: '#00e5ff', width: 3 },
        itemStyle: { color: '#20ff9f', borderColor: '#071224', borderWidth: 2 },
      },
    ],
  }
})

const importerOption = computed<EChartsOption>(() => ({
  color: ['#20ff9f'],
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(6, 10, 24, 0.92)',
    borderColor: 'rgba(0, 229, 255, 0.24)',
    textStyle: { color: '#e8f3ff' },
  },
  grid: { left: 36, right: 18, top: 34, bottom: 44 },
  xAxis: {
    type: 'category',
    data: importerStats.value.map((item) => item.label),
    axisLine: { lineStyle: { color: 'rgba(232, 243, 255, 0.22)' } },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(232, 243, 255, 0.62)', interval: 0, rotate: 16 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(232, 243, 255, 0.12)' } },
    axisLabel: { color: 'rgba(232, 243, 255, 0.62)' },
  },
  series: [
    {
      name: '记录数',
      type: 'bar',
      data: importerStats.value.map((item) => item.recordCount),
      itemStyle: { borderRadius: [8, 8, 2, 2] },
    },
  ],
}))

function activeDatasetParam() {
  return selectedDatasetDate.value || undefined
}

function isSlowRecord(record: AisRecordView) {
  return (typeof record.sog === 'number' && record.sog < 1) || record.status === 1 || record.status === 5
}

function isActiveRecord(record: AisRecordView) {
  return typeof record.sog === 'number' && record.sog >= 8
}

function hasRiskNote(record: AisRecordView) {
  return /(异常|风险|告警|可疑|abnormal|risk|warning)/i.test(record.note || '')
}

function vesselName(record: AisRecordView) {
  return record.vesselName || `MMSI ${record.mmsi}`
}

function displayTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.includes('T') ? value.replace('T', ' ') : value
}

function metricLabel(value: number | string | null | undefined, unit: string) {
  if (value == null || value === '') {
    return '-'
  }
  return `${value}${unit}`
}

function positionLabel(record: Pick<AisRecordView, 'latitude' | 'longitude'>) {
  return `${record.latitude.toFixed(5)}, ${record.longitude.toFixed(5)}`
}

function markerTone(record: AisRecordView): MarkerTone {
  if (hasRiskNote(record)) return 'violet'
  if (isActiveRecord(record)) return 'emerald'
  return 'aqua'
}

function timestamp(value?: string) {
  if (!value) {
    return 0
  }
  return new Date(value.includes('T') ? value : value.replace(' ', 'T')).getTime()
}

function toLatLng(point: [number, number]) {
  return toMapDisplayPoint(point[0], point[1])
}

function distanceMeters(lat1: number, lon1: number, lat2: number, lon2: number) {
  const toRad = (value: number) => (value * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLon = toRad(lon2 - lon1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function ensureMap() {
  if (map || !mapRef.value) {
    return
  }

  map = L.map(mapRef.value, {
    zoomControl: true,
    attributionControl: true,
    preferCanvas: true,
  }).setView(toMapDisplayPoint(defaultCenter[0], defaultCenter[1]), 8)

  addPreferredTileLayer(map)
  overlayLayer = L.layerGroup().addTo(map)
  markerLayer = L.markerClusterGroup({
    showCoverageOnHover: false,
    spiderfyOnMaxZoom: true,
    zoomToBoundsOnClick: true,
    disableClusteringAtZoom: 10,
    maxClusterRadius: 40,
  }).addTo(map)

  resizeObserver = new ResizeObserver(() => map?.invalidateSize(false))
  resizeObserver.observe(mapRef.value)
  window.setTimeout(() => map?.invalidateSize(false), 120)
}

function popupHtml(record: AisRecordView) {
  return buildMapPopupCard({
    eyebrow: 'AIS Snapshot',
    title: vesselName(record),
    subtitle: `MMSI ${record.mmsi}`,
    meta: displayTime(record.baseDateTime),
    chips: [
      { label: '航速', value: metricLabel(record.sog, 'kn') },
      { label: '航向', value: metricLabel(record.cog, '°') },
    ],
    lines: [
      `坐标 ${positionLabel(record)}`,
      record.imo ? `IMO ${record.imo}` : '',
      record.callSign ? `呼号 ${record.callSign}` : '',
      record.sourceFile ? `来源 ${record.sourceFile}` : '',
    ].filter(Boolean),
  })
}

function renderMap() {
  if (!map || !overlayLayer || !markerLayer) {
    return
  }
  const routeOverlay = overlayLayer
  const vesselMarkers = markerLayer

  routeOverlay.clearLayers()
  vesselMarkers.clearLayers()
  markerMap.clear()

  const routeMode = selectedMapMode.value === 'all' || selectedMapMode.value === 'traffic'
  const riskMode = selectedMapMode.value === 'all' || selectedMapMode.value === 'risk'
  const slowMode = selectedMapMode.value === 'all' || selectedMapMode.value === 'slow'

  if (routeMode) {
    routeSignals.forEach((route) => {
      L.polyline(route.path.map(toLatLng), {
        color: route.color,
        weight: 4,
        opacity: 0.84,
      })
        .bindPopup(
          buildMapPopupCard({
            eyebrow: 'Main Route',
            title: route.name,
            subtitle: route.description,
          }),
          { className: 'gsmv-map-popup' },
        )
        .addTo(routeOverlay)
    })
  }

  focusZones.forEach((zone) => {
    const insight = zoneInsights.value.find((item) => item.id === zone.id)
    const slowRatio = insight && insight.count ? insight.slowCount / insight.count : 0
    const emphasisColor = riskMode && slowRatio > 0.35 ? '#ff4f6a' : slowMode && insight?.slowCount ? '#ffb84d' : zone.color
    L.circle(toMapDisplayPoint(zone.lat, zone.lng), {
      radius: zone.radiusMeters,
      color: emphasisColor,
      fillColor: emphasisColor,
      fillOpacity: 0.08,
      opacity: 0.78,
      weight: 2,
      dashArray: riskMode ? '8 10' : undefined,
    })
      .bindPopup(
        buildMapPopupCard({
          eyebrow: zone.kind,
          title: zone.name,
          subtitle: zone.detail,
          chips: [
            { label: '快照点位', value: `${insight?.count || 0}` },
            { label: '平均航速', value: insight?.avgSpeed || '-' },
          ],
        }),
        { className: 'gsmv-map-popup' },
      )
      .addTo(routeOverlay)
  })

  const bounds: [number, number][] = []
  filteredRecords.value.forEach((record) => {
    const point = toMapDisplayPoint(record.latitude, record.longitude)
    const marker = L.marker(point, {
      icon: createMapMarkerIcon(vesselName(record), {
        compact: true,
        active: focusRecord.value?.id === record.id,
        tone: markerTone(record),
      }),
      zIndexOffset: focusRecord.value?.id === record.id ? 900 : 0,
    })

    marker
      .bindPopup(popupHtml(record), { className: 'gsmv-map-popup' })
      .on('click', () => {
        selectedRecordId.value = record.id
      })
      .addTo(vesselMarkers)

    markerMap.set(record.id, marker)
    bounds.push(point)
  })

  if (bounds.length === 1) {
    map.setView(bounds[0], 9, { animate: false })
  } else if (bounds.length > 1) {
    map.fitBounds(bounds, { padding: [26, 26], maxZoom: 10 })
  } else {
    map.setView(toMapDisplayPoint(defaultCenter[0], defaultCenter[1]), 7)
  }
}

async function loadOverview() {
  loading.value = true
  try {
    if (!canReadAis.value) {
      datasetDates.value = []
      records.value = []
      datasetDateStats.value = []
      importerStats.value = []
      riskSummary.value = emptyRiskSummary()
      shippingZones.value = canReadShippingZones.value ? await fetchAllShippingZones() : []
      selectedRecordId.value = ''
      renderMap()
      return
    }

    const [dates, mapData, dateStats, importers, risk, nodeOptions] = await Promise.all([
      fetchAisDatasetDates(),
      fetchAisMapRecords({
        datasetDate: activeDatasetParam(),
        limit: MAP_LIMIT,
      }),
      fetchAisDatasetDateStats({}),
      fetchAisImporterStats({ limit: 6 }),
      fetchAisRiskSummary({}),
      canReadShippingZones.value ? fetchAllShippingZones() : Promise.resolve([]),
    ])

    datasetDates.value = dates
    if (selectedDatasetDate.value && !dates.includes(selectedDatasetDate.value)) {
      selectedDatasetDate.value = ''
    }

    records.value = [...mapData.items].sort((a, b) => timestamp(b.baseDateTime) - timestamp(a.baseDateTime))
    datasetDateStats.value = dateStats
    importerStats.value = importers
    riskSummary.value = risk
    shippingZones.value = nodeOptions

    if (!records.value.some((item) => item.id === selectedRecordId.value)) {
      selectedRecordId.value = records.value[0]?.id || ''
    }

    renderMap()
  } catch (error) {
    if (!isForbiddenError(error)) {
      ElMessage.error(error instanceof Error ? error.message : '态势总览加载失败')
    }
  } finally {
    loading.value = false
  }
}

function hasAuthority(authority: string) {
  return (authStore.authorities || []).includes(authority) || authStore.roleCodes.includes('ADMIN')
}

function emptyRiskSummary(): AisRiskSummary {
  return {
    total: 0,
    lowSpeedCount: 0,
    stoppedCount: 0,
    abnormalNoteCount: 0,
    uniqueVesselCount: 0,
  }
}

function isForbiddenError(error: unknown) {
  return error instanceof Error && /没有权限|403|Forbidden/i.test(error.message)
}

function resetSelectedDate() {
  stopTimelinePlayback()
  selectedDatasetDate.value = ''
  void loadOverview()
}

function selectAdjacentDate(step: -1 | 1) {
  if (!datasetDates.value.length) {
    return
  }

  const current = activeDatasetIndex.value >= 0 ? activeDatasetIndex.value : 0
  const nextIndex = current - step
  if (nextIndex < 0 || nextIndex >= datasetDates.value.length) {
    return
  }
  stopTimelinePlayback()
  selectedDatasetDate.value = datasetDates.value[nextIndex]
  void loadOverview()
}

function startTimelinePlayback() {
  stopTimelinePlayback()
  if (!datasetDates.value.length) {
    return
  }
  playingTimeline.value = true
  if (!selectedDatasetDate.value) {
    selectedDatasetDate.value = datasetDates.value[0]
    void loadOverview()
  }
  playbackTimer = window.setInterval(() => {
    if (!datasetDates.value.length) {
      stopTimelinePlayback()
      return
    }
    const current = activeDatasetIndex.value >= 0 ? activeDatasetIndex.value : 0
    const nextIndex = current + 1 >= datasetDates.value.length ? 0 : current + 1
    selectedDatasetDate.value = datasetDates.value[nextIndex]
    void loadOverview()
  }, playbackSpeed.value)
}

function stopTimelinePlayback() {
  playingTimeline.value = false
  if (playbackTimer) {
    window.clearInterval(playbackTimer)
    playbackTimer = null
  }
}

function toggleTimelinePlayback() {
  if (playingTimeline.value) {
    stopTimelinePlayback()
    return
  }
  startTimelinePlayback()
}

function handleKpiCardClick(item: KpiCard) {
  if (!item.mapMode) {
    return
  }
  selectedMapMode.value = item.mapMode
}

watch(selectedMapMode, () => {
  renderMap()
})

watch(selectedDatasetDate, (value, previous) => {
  if (value === previous) {
    return
  }
  void loadOverview()
})

watch(
  () => selectedRecordId.value,
  (id) => {
    if (!id || !map) {
      return
    }
    const marker = markerMap.get(id)
    if (marker) {
      marker.openPopup()
    }
    renderMap()
  },
)

onMounted(async () => {
  updateWeatherTime()
  weatherTimer = setInterval(updateWeatherTime, 1000)
  void loadWeather()
  await nextTick()
  ensureMap()
  void loadOverview()
})

onBeforeUnmount(() => {
  stopTimelinePlayback()
  if (weatherTimer) {
    clearInterval(weatherTimer)
    weatherTimer = null
  }
  resizeObserver?.disconnect()
  resizeObserver = null
  overlayLayer = null
  markerLayer = null
  markerMap.clear()
  map?.remove()
  map = null
})
</script>

<style scoped>
.dashboard-page {
  gap: 16px;
}

.dashboard-window__eyebrow {
  display: inline-flex;
  width: fit-content;
  color: var(--gsmv-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.dashboard-hero__pulse {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 4px;
}

.dashboard-hero__pulse span {
  height: 6px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gsmv-primary), var(--gsmv-accent));
  box-shadow: 0 0 18px rgba(0, 229, 255, 0.36);
}

.dashboard-hero__pulse span:nth-child(2) {
  background: linear-gradient(90deg, var(--gsmv-warm), var(--gsmv-secondary));
}

.dashboard-hero__pulse span:nth-child(3) {
  background: linear-gradient(90deg, #ffb84d, var(--gsmv-danger));
}

.dashboard-kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-window,
.dashboard-window__metric,
.dashboard-window__column,
.dashboard-route-card,
.dashboard-alert {
  position: relative;
  border: 1px solid var(--gsmv-border);
  background:
    linear-gradient(145deg, rgba(0, 229, 255, 0.13), rgba(124, 60, 255, 0.09) 48%, rgba(32, 255, 159, 0.04)),
    linear-gradient(180deg, rgba(13, 18, 42, 0.86), rgba(5, 8, 22, 0.92));
  box-shadow: var(--gsmv-shadow-soft);
  backdrop-filter: blur(24px);
  overflow: hidden;
}

.dashboard-window::after,
.dashboard-window__metric::after,
.dashboard-window__column::after,
.dashboard-route-card::after,
.dashboard-alert::after {
  content: "";
  position: absolute;
  inset: auto -18% -42% 42%;
  height: 78%;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.16), transparent 72%);
  pointer-events: none;
}

.dashboard-window__metric {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  min-height: 128px;
  padding: 16px;
  border-radius: 22px;
}

.dashboard-window__metric.is-interactive {
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.dashboard-window__metric.is-interactive:hover,
.dashboard-window__metric.is-active {
  transform: translateY(-2px);
  border-color: rgba(0, 229, 255, 0.32);
  box-shadow:
    0 18px 34px rgba(0, 10, 34, 0.2),
    0 0 0 1px rgba(0, 229, 255, 0.1) inset;
}

.dashboard-window__metric-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid rgba(0, 229, 255, 0.22);
  border-radius: 14px;
  background: rgba(0, 229, 255, 0.1);
  color: var(--gsmv-primary);
}

.dashboard-window__metric span {
  display: block;
  color: rgba(232, 243, 255, 0.66);
  font-size: 13px;
  font-weight: 700;
}

.dashboard-window__metric strong {
  display: block;
  margin-top: 6px;
  color: #ffffff;
  font-size: 28px;
  line-height: 1;
}

.dashboard-window__metric p {
  margin: 8px 0 0;
  color: rgba(232, 243, 255, 0.58);
  font-size: 12px;
  line-height: 1.55;
}

.dashboard-window__metric em {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(32, 255, 159, 0.12);
  color: var(--gsmv-accent);
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.dashboard-command-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
}

.dashboard-window {
  border-radius: 26px;
}

.dashboard-window__header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 66px;
  padding: 15px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.11);
  background: linear-gradient(180deg, rgba(0, 229, 255, 0.08), transparent);
}

.dashboard-window__header--compact {
  min-height: 68px;
}

.dashboard-window__header strong {
  display: block;
  margin-top: 6px;
  color: #f4fbff;
  font-size: 18px;
  line-height: 1.2;
}

.dashboard-window__header p {
  margin: 6px 0 0;
  max-width: 760px;
  color: rgba(232, 243, 255, 0.62);
  font-size: 13px;
  line-height: 1.45;
}

.dashboard-map-modes {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dashboard-map-modes button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  color: rgba(232, 243, 255, 0.72);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.dashboard-map-modes button:hover,
.dashboard-map-modes button.is-active {
  border-color: rgba(0, 229, 255, 0.38);
  background: rgba(0, 229, 255, 0.14);
  color: #ffffff;
  transform: translateY(-1px);
}

.dashboard-map-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  padding: 0 18px 14px;
}

.dashboard-map-toolbar__date {
  width: 220px;
}

.dashboard-map-toolbar__speed {
  width: 150px;
}

.dashboard-map-toolbar__snapshot {
  display: grid;
  color: rgba(232, 243, 255, 0.66);
  font-size: 13px;
}

.dashboard-map-toolbar__snapshot small {
  color: rgba(232, 243, 255, 0.52);
  font-size: 11px;
}

.dashboard-map-toolbar__snapshot small.is-up {
  color: #ff7b7b;
}

.dashboard-map-toolbar__snapshot small.is-down {
  color: #20ff9f;
}

.dashboard-map-toolbar__snapshot small.is-neutral {
  color: rgba(232, 243, 255, 0.52);
}

.dashboard-map-toolbar :deep(.el-button.is-playing) {
  --el-button-bg-color: rgba(255, 184, 77, 0.18);
  --el-button-border-color: rgba(255, 184, 77, 0.42);
  --el-button-text-color: #ffe1a8;
}

.dashboard-map-stage {
  position: relative;
  padding: 14px;
}

.dashboard-map {
  min-height: clamp(340px, 42vh, 580px);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 22px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.1), rgba(124, 60, 255, 0.08)),
    rgba(7, 11, 24, 0.74);
  overflow: hidden;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    0 0 30px rgba(0, 229, 255, 0.12),
    0 18px 40px rgba(0, 4, 18, 0.28);
}

.dashboard-map-stage__scan {
  position: absolute;
  inset: 14px;
  z-index: 420;
  border-radius: 22px;
  background: linear-gradient(180deg, transparent, rgba(0, 229, 255, 0.09), transparent);
  background-size: 100% 38%;
  mix-blend-mode: screen;
  pointer-events: none;
  animation: dashboard-scan 5.8s linear infinite;
}

.dashboard-map-stage__compass,
.dashboard-map-legend {
  position: absolute;
  z-index: 430;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(5, 8, 22, 0.74);
  box-shadow: 0 18px 36px rgba(0, 4, 18, 0.28);
  backdrop-filter: blur(18px);
}

.dashboard-map-stage__compass {
  top: 30px;
  left: 30px;
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  border-radius: 20px;
}

.dashboard-map-stage__compass span {
  color: var(--gsmv-primary);
  font-size: 18px;
  font-weight: 900;
}

.dashboard-map-stage__compass strong {
  color: rgba(232, 243, 255, 0.7);
  font-size: 11px;
}

.dashboard-map-legend {
  right: 30px;
  bottom: 30px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  max-width: min(520px, calc(100% - 68px));
  padding: 12px 14px;
  border-radius: 18px;
}

.dashboard-map-legend span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: rgba(232, 243, 255, 0.78);
  font-size: 12px;
  font-weight: 700;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.legend-dot--route {
  background: var(--gsmv-primary);
}

.legend-dot--heat {
  background: #ffb84d;
}

.legend-dot--risk {
  background: var(--gsmv-danger);
}

.legend-dot--ship {
  background: var(--gsmv-accent);
}

.dashboard-intel-rail {
  display: grid;
  gap: 18px;
}

.dashboard-score-panel__body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding: 16px;
}

.dashboard-score-panel__body strong {
  color: #ffffff;
  font-size: 54px;
  line-height: 0.95;
}

.dashboard-score-panel__body p {
  margin: 0;
  color: rgba(232, 243, 255, 0.66);
  font-size: 13px;
  line-height: 1.55;
}

.dashboard-window__column {
  border-radius: 26px;
}

.dashboard-focus-vessel,
.dashboard-alert-list,
.dashboard-route-list {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding: 14px;
}

.dashboard-focus-vessel {
  gap: 14px;
}

.dashboard-focus-vessel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.dashboard-focus-vessel__header span,
.dashboard-alert span {
  color: var(--gsmv-primary);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.dashboard-focus-vessel__header strong,
.dashboard-alert strong,
.dashboard-route-card strong {
  display: block;
  margin-top: 6px;
  color: #ffffff;
  font-size: 16px;
}

.dashboard-focus-vessel__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-focus-vessel__meta small {
  display: block;
  color: rgba(232, 243, 255, 0.58);
  font-size: 12px;
}

.dashboard-focus-vessel__meta strong {
  display: block;
  margin-top: 6px;
  color: #f4fbff;
  font-size: 14px;
  line-height: 1.45;
}

.dashboard-alert {
  padding: 13px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.055);
}

.dashboard-alert p,
.dashboard-route-card p {
  margin: 8px 0 0;
  color: rgba(232, 243, 255, 0.62);
  font-size: 12px;
  line-height: 1.55;
}

.dashboard-alert.is-critical {
  border-color: rgba(255, 79, 106, 0.28);
  background: linear-gradient(135deg, rgba(255, 79, 106, 0.14), rgba(255, 255, 255, 0.045));
}

.dashboard-alert.is-warning {
  border-color: rgba(255, 184, 77, 0.3);
  background: linear-gradient(135deg, rgba(255, 184, 77, 0.12), rgba(255, 255, 255, 0.045));
}

.dashboard-alert.is-stable {
  border-color: rgba(32, 255, 159, 0.24);
  background: linear-gradient(135deg, rgba(32, 255, 159, 0.11), rgba(255, 255, 255, 0.045));
}

.dashboard-alert.is-critical span {
  color: var(--gsmv-danger);
}

.dashboard-alert.is-warning span {
  color: #ffb84d;
}

.dashboard-alert.is-stable span {
  color: var(--gsmv-accent);
}

.dashboard-lower-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(320px, 1fr) minmax(320px, 1fr);
  gap: 16px;
}

.dashboard-route-card {
  display: grid;
  grid-template-columns: 5px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 96px;
  padding: 15px;
  border-radius: 18px;
}

.dashboard-route-card__line {
  align-self: stretch;
  border-radius: 999px;
  box-shadow: 0 0 18px currentColor;
}

.dashboard-route-card > span {
  color: var(--gsmv-accent);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.dashboard-chart-window {
  min-width: 0;
}

.dashboard-compact-chart {
  display: block;
  padding: 16px;
}

.dashboard-compact-chart :deep(.chart-shell),
.dashboard-compact-chart :deep(.chart-panel) {
  min-height: 274px;
}

.dashboard-compact-chart :deep(.chart-shell) {
  border-radius: 20px;
}

@keyframes dashboard-scan {
  from {
    background-position: 0 -120%;
  }
  to {
    background-position: 0 220%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .dashboard-map-stage__scan {
    animation: none;
  }
}

@media (max-width: 1480px) {
  .dashboard-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-command-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-lower-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 980px) {
  .dashboard-kpi-grid,
  .dashboard-lower-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-map {
    min-height: 460px;
  }

  .dashboard-focus-vessel__meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-window__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-map-modes {
    width: 100%;
    justify-content: flex-start;
  }

  .dashboard-map-toolbar {
    padding: 0 14px 14px;
  }

  .dashboard-map-toolbar__date {
    width: 100%;
  }

  .dashboard-map-stage {
    padding: 12px;
  }

  .dashboard-map {
    min-height: 400px;
  }

  .dashboard-map-stage__scan {
    inset: 12px;
  }

  .dashboard-map-stage__compass {
    top: 24px;
    left: 24px;
  }

  .dashboard-map-legend {
    right: 24px;
    bottom: 24px;
  }

  .dashboard-route-card {
    grid-template-columns: 5px minmax(0, 1fr);
  }

  .dashboard-route-card > span {
    grid-column: 2;
  }
}

/* ==================== 天气面板 ==================== */
.dashboard-weather-panel__time {
  display: block;
  font-size: 13px;
  color: var(--gsmv-muted);
  margin-top: 2px;
  font-family: 'Courier New', monospace;
}

.dashboard-weather-panel__body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
}

.dashboard-weather-panel__status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--gsmv-muted);
  font-size: 14px;
}

.dashboard-weather-panel__spin {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid var(--gsmv-border);
  border-top-color: var(--gsmv-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.dashboard-weather-panel__error {
  color: var(--gsmv-warm);
}

.dashboard-weather-panel__result {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dashboard-weather-panel__interpretation {
  padding: 12px;
  background: rgba(0, 229, 255, 0.08);
  border-radius: 10px;
  border: 1px solid rgba(0, 229, 255, 0.18);
}

.dashboard-weather-panel__interpretation strong {
  display: block;
  font-size: 13px;
  color: var(--gsmv-primary);
  margin-bottom: 8px;
}

.dashboard-weather-panel__interpretation p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--gsmv-text);
}

.dashboard-weather-panel__raw {
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.28);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.dashboard-weather-panel__raw strong {
  display: block;
  font-size: 12px;
  color: var(--gsmv-muted);
  margin-bottom: 6px;
}

.dashboard-weather-panel__raw pre {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--gsmv-text);
  white-space: pre-wrap;
  word-break: break-all;
}

.dashboard-weather-panel__hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--gsmv-muted);
  line-height: 1.55;
}

.dashboard-weather-panel__hint a {
  color: var(--gsmv-primary);
  text-decoration: none;
}

.dashboard-weather-panel__hint a:hover {
  text-decoration: underline;
}
</style>
