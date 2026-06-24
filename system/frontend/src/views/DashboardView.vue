<template>
  <div class="page-shell dashboard-page">
    <section class="dashboard-command-grid">
      <article class="dashboard-window dashboard-map-window">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">LA/LB Port Command · Live Maritime Map</span>
            <strong>洛杉矶港 / 长滩港海上交通态势大屏</strong>
            <p>San Pedro Bay 进出港航线、外锚地排队、码头作业压力与异常风险热区。</p>
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

        <div class="dashboard-map-stage">
          <div ref="mapRef" class="dashboard-map" />
          <div class="dashboard-map-stage__scan" />
          <div class="dashboard-map-stage__compass">
            <span>N</span>
            <strong>11 nm</strong>
          </div>
          <div class="dashboard-map-legend">
            <span><i class="legend-dot legend-dot--route" />主航线</span>
            <span><i class="legend-dot legend-dot--heat" />热力区</span>
            <span><i class="legend-dot legend-dot--risk" />风险水域</span>
            <span><i class="legend-dot legend-dot--ship" />AIS 目标</span>
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
            <p>港外等待仍处高位，主进港航道保持可控通行。</p>
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
              <div v-if="weatherResult.weatherData" class="dashboard-weather-panel__raw">
                <strong>实时天气数据</strong>
                <pre>{{ weatherResult.weatherData }}</pre>
              </div>
            </div>
            <p class="dashboard-weather-panel__hint">
              💡 如果想要获取别的地方的天气，可以试试在<router-link to="/quiz/ai">知识问答模块</router-link>中询问 AI 助手哦！
            </p>
          </div>
        </section>

        <section class="dashboard-window dashboard-window__column">
          <header class="dashboard-window__header dashboard-window__header--compact">
            <span class="dashboard-window__eyebrow">Port Focus</span>
            <strong>重点港区</strong>
          </header>
          <div class="dashboard-focus-list">
            <article v-for="port in portSignals" :key="port.id" class="dashboard-focus-item">
              <div>
                <span>{{ port.kind }}</span>
                <strong>{{ port.name }}</strong>
                <p>{{ port.status }}</p>
              </div>
              <div class="dashboard-focus-item__score">
                <b>{{ port.berthUsage }}%</b>
                <small>泊位使用</small>
              </div>
            </article>
          </div>
        </section>

        <section class="dashboard-window dashboard-window__column">
          <header class="dashboard-window__header dashboard-window__header--compact">
            <span class="dashboard-window__eyebrow">Watch List</span>
            <strong>局势预警</strong>
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
      <article v-for="item in kpiCards" :key="item.label" class="dashboard-window__metric">
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
            <span class="dashboard-window__eyebrow">Route Load</span>
            <strong>航线载荷与流向</strong>
          </div>
        </header>
        <div class="dashboard-route-list">
          <article v-for="route in routeSignals" :key="route.id" class="dashboard-route-card">
            <div class="dashboard-route-card__line" :style="{ background: route.color }" />
            <div>
              <strong>{{ route.name }}</strong>
              <p>{{ route.description }}</p>
            </div>
            <span>{{ route.volume }}</span>
          </article>
        </div>
      </article>

      <article class="dashboard-window dashboard-chart-window">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">AIS Trend</span>
            <strong>近 30 天 AIS 活跃趋势</strong>
          </div>
        </header>
        <ChartPanel class="dashboard-compact-chart" :option="trendOption" />
      </article>

      <article class="dashboard-window dashboard-chart-window">
        <header class="dashboard-window__header">
          <div>
            <span class="dashboard-window__eyebrow">Operator Load</span>
            <strong>航线/锚地负载统计</strong>
          </div>
        </header>
        <ChartPanel class="dashboard-compact-chart" :option="routeLoadOption" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import { Aim, DataAnalysis, LocationFilled, Monitor, Ship, WarningFilled } from '@element-plus/icons-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { EChartsOption } from 'echarts'
import { fetchDashboardSummary, fetchObservationActivity, fetchObservationTrend } from '@/api/reports'
import { fetchWeatherInterpret } from '@/api/quiz'
import ChartPanel from '@/components/ChartPanel.vue'
import { listenDataChanged } from '@/utils/dataSync'
import { addPreferredTileLayer, toMapDisplayPoint } from '@/utils/mapProvider'
import { buildMapPopupCard, createMapMarkerIcon } from '@/utils/mapMarkerTheme'
import type { DashboardSummary, NameValuePoint } from '@/types/gsmv'

type MapMode = 'all' | 'traffic' | 'risk' | 'anchor'
type MarkerTone = 'aqua' | 'emerald' | 'violet'

interface PortSignal {
  id: string
  name: string
  kind: string
  lat: number
  lng: number
  status: string
  berthUsage: number
  queue: string
  tone: MarkerTone
}

interface RouteSignal {
  id: string
  name: string
  description: string
  path: Array<[number, number]>
  color: string
  volume: string
  risk: string
}

interface HeatCell {
  id: string
  name: string
  lat: number
  lng: number
  radius: number
  intensity: number
  type: 'traffic' | 'anchor'
  detail: string
}

interface RiskZone {
  id: string
  name: string
  lat: number
  lng: number
  radius: number
  level: 'high' | 'medium' | 'watch'
  detail: string
}

interface VesselTarget {
  id: string
  name: string
  lat: number
  lng: number
  status: string
  speed: string
  tone: MarkerTone
}

const summary = ref<DashboardSummary>({
  totalSpecies: 0,
  totalObservations: 0,
  totalEcosystems: 0,
  totalUsers: 0,
  recentObservationCount: 0,
})
const trendData = ref<NameValuePoint[]>([])
const observerActivity = ref<NameValuePoint[]>([])
const selectedMapMode = ref<MapMode>('all')
const mapRef = ref<HTMLDivElement>()

const losAngelesPort: [number, number] = [33.7315, -118.262]
const longBeachPort: [number, number] = [33.7542, -118.2165]

const mapModes: Array<{ label: string; value: MapMode }> = [
  { label: '综合态势', value: 'all' },
  { label: '航线流量', value: 'traffic' },
  { label: '风险热区', value: 'risk' },
  { label: '锚地等待', value: 'anchor' },
]

const portSignals: PortSignal[] = [
  {
    id: 'port-la',
    name: '洛杉矶港',
    kind: 'PORT OF LA',
    lat: losAngelesPort[0],
    lng: losAngelesPort[1],
    status: '集装箱泊位保持高负荷，进港窗口集中在傍晚。',
    berthUsage: 87,
    queue: '18 艘',
    tone: 'aqua',
  },
  {
    id: 'port-lb',
    name: '长滩港',
    kind: 'PORT OF LONG BEACH',
    lat: longBeachPort[0],
    lng: longBeachPort[1],
    status: '码头周转改善，但东侧锚地仍有排队压力。',
    berthUsage: 82,
    queue: '14 艘',
    tone: 'emerald',
  },
  {
    id: 'anchorage',
    name: 'San Pedro 外锚地',
    kind: 'ANCHORAGE',
    lat: 33.65,
    lng: -118.29,
    status: '外锚地目标密度升高，需关注低速漂移与交叉航迹。',
    berthUsage: 76,
    queue: '27 艘',
    tone: 'violet',
  },
]

const routeSignals: RouteSignal[] = [
  {
    id: 'pacific-main',
    name: '北太平洋主干线',
    description: '亚洲方向远洋船队进入 San Pedro Bay 的主通道。',
    path: [
      [33.98, -119.36],
      [33.89, -118.84],
      [33.79, -118.48],
      [33.73, -118.27],
    ],
    color: '#00e5ff',
    volume: '42 艘次/日',
    risk: '中',
  },
  {
    id: 'coastal-north',
    name: '加州北向沿岸线',
    description: '连接湾区、奥克兰与南加州港群的沿岸航路。',
    path: [
      [34.18, -118.78],
      [34.02, -118.58],
      [33.84, -118.38],
      [33.75, -118.22],
    ],
    color: '#20ff9f',
    volume: '26 艘次/日',
    risk: '低',
  },
  {
    id: 'mexico-south',
    name: '墨西哥南向支线',
    description: '南向连接 Ensenada 与 Baja 近海的支线通道。',
    path: [
      [33.28, -118.66],
      [33.43, -118.48],
      [33.61, -118.32],
      [33.73, -118.23],
    ],
    color: '#ffb84d',
    volume: '19 艘次/日',
    risk: '中',
  },
  {
    id: 'terminal-shuttle',
    name: '港内接驳环线',
    description: 'Terminal Island、内港与外港之间的拖轮/支线活动。',
    path: [
      [33.72, -118.29],
      [33.75, -118.25],
      [33.77, -118.21],
      [33.73, -118.18],
      [33.71, -118.24],
    ],
    color: '#ff4fd8',
    volume: '64 艘次/日',
    risk: '高',
  },
]

const heatCells: HeatCell[] = [
  {
    id: 'heat-gate',
    name: '防波堤入口热区',
    lat: 33.715,
    lng: -118.245,
    radius: 5200,
    intensity: 0.92,
    type: 'traffic',
    detail: '进出港交汇与低速等待叠加，短时密度最高。',
  },
  {
    id: 'heat-anchor-east',
    name: '东侧锚地热区',
    lat: 33.64,
    lng: -118.18,
    radius: 7600,
    intensity: 0.74,
    type: 'anchor',
    detail: '长滩方向待泊船舶集中，队列稳定但占用水域较大。',
  },
  {
    id: 'heat-anchor-west',
    name: '西南外海等待带',
    lat: 33.58,
    lng: -118.38,
    radius: 8500,
    intensity: 0.66,
    type: 'anchor',
    detail: '远洋干线船舶减速进入排序，建议持续跟踪 ETA 波动。',
  },
]

const riskZones: RiskZone[] = [
  {
    id: 'risk-crossing',
    name: '交叉航迹预警区',
    lat: 33.705,
    lng: -118.235,
    radius: 3600,
    level: 'high',
    detail: '进港航道与拖轮接驳流交叉，夜间复核优先级高。',
  },
  {
    id: 'risk-breakwater',
    name: '防波堤限速带',
    lat: 33.712,
    lng: -118.172,
    radius: 2900,
    level: 'medium',
    detail: '靠近港口入口的低速航段，需关注异常停滞。',
  },
  {
    id: 'risk-weather',
    name: '外海能见度观察区',
    lat: 33.52,
    lng: -118.52,
    radius: 5600,
    level: 'watch',
    detail: '外海侧可能出现航速分化，保持观察即可。',
  },
]

const vesselTargets: VesselTarget[] = [
  { id: 'v-01', name: 'APL HORIZON', lat: 33.71, lng: -118.31, status: '进港排序', speed: '8.4 kn', tone: 'aqua' },
  { id: 'v-02', name: 'EVER SIGNAL', lat: 33.64, lng: -118.22, status: '锚地等待', speed: '0.8 kn', tone: 'violet' },
  { id: 'v-03', name: 'MAERSK PACIFIC', lat: 33.82, lng: -118.48, status: '主线进港', speed: '13.2 kn', tone: 'emerald' },
  { id: 'v-04', name: 'COSCO BRIDGE', lat: 33.75, lng: -118.19, status: '靠泊接近', speed: '5.1 kn', tone: 'aqua' },
  { id: 'v-05', name: 'PACIFIC TUG 07', lat: 33.735, lng: -118.245, status: '港内接驳', speed: '6.7 kn', tone: 'emerald' },
  { id: 'v-06', name: 'SEA ARROW', lat: 33.575, lng: -118.405, status: '外海减速', speed: '3.2 kn', tone: 'violet' },
]

const alerts = [
  {
    level: 'critical',
    levelLabel: '高',
    title: '防波堤入口交叉航迹升高',
    detail: '过去 2 小时内低速目标与拖轮接驳轨迹重叠，需要保持复核。',
  },
  {
    level: 'warning',
    levelLabel: '中',
    title: '东侧锚地排队压力延续',
    detail: '长滩方向待泊目标占用水域较大，预计晚间有一轮集中进港。',
  },
  {
    level: 'stable',
    levelLabel: '稳',
    title: '北太平洋主干线通行可控',
    detail: '远洋船队速度分布正常，未见大范围绕航信号。',
  },
]

const weatherCity = ref('湛江')
const weatherTime = ref('')
const weatherLoading = ref(true)
const weatherError = ref<string | null>(null)
const weatherResult = ref<Record<string, unknown> | null>(null)
let weatherTimer: ReturnType<typeof setInterval> | null = null
let timeTimer: ReturnType<typeof setInterval> | null = null

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
    if (data.akConfigured === false) {
      weatherError.value = String(data.error || '百度地图 AK 未配置')
    } else if (data.error) {
      weatherError.value = String(data.error)
    }
    weatherResult.value = data as Record<string, unknown>
  } catch (e: unknown) {
    weatherError.value = e instanceof Error ? e.message : '天气数据加载失败'
  } finally {
    weatherLoading.value = false
  }
}

const fallbackTrend: NameValuePoint[] = [
  { name: '05-19', value: 28 },
  { name: '05-23', value: 34 },
  { name: '05-27', value: 39 },
  { name: '05-31', value: 36 },
  { name: '06-04', value: 48 },
  { name: '06-08', value: 43 },
  { name: '06-12', value: 52 },
  { name: '06-15', value: 47 },
]

let map: L.Map | null = null
let overlayLayer: L.LayerGroup | null = null
let resizeObserver: ResizeObserver | null = null
let stopDataSync: (() => void) | undefined

const situationScore = computed(() => {
  const base = 78
  const activityBoost = Math.min(summary.value.recentObservationCount || 5, 18)
  return Math.min(96, base + activityBoost)
})

const kpiCards = computed(() => [
  {
    label: 'AIS 活跃目标',
    value: `${Math.max(summary.value.recentObservationCount * 9, 148)}`,
    detail: 'San Pedro Bay 近岸与外锚地估算活跃目标',
    trend: '+8%',
    icon: Ship,
  },
  {
    label: '系统 AIS 记录',
    value: `${summary.value.totalObservations || 19}`,
    detail: '接入首页态势研判的累计动态记录',
    trend: 'Live',
    icon: DataAnalysis,
  },
  {
    label: '港外等待',
    value: '41 艘',
    detail: '洛杉矶/长滩港外锚地与排序队列',
    trend: '+6',
    icon: Aim,
  },
  {
    label: '风险热区',
    value: `${riskZones.length + heatCells.length}`,
    detail: '拥堵、交叉航迹、能见度与低速异常',
    trend: '3 高亮',
    icon: WarningFilled,
  },
  {
    label: '重点航线',
    value: `${routeSignals.length}`,
    detail: '远洋主干线、沿岸线、南向支线与港内接驳',
    trend: '4 条',
    icon: LocationFilled,
  },
  {
    label: '刷新延迟',
    value: '14 min',
    detail: '态势图层与报表摘要的最近同步间隔',
    trend: '正常',
    icon: Monitor,
  },
])

const trendPoints = computed(() => (trendData.value.length ? trendData.value : fallbackTrend))

const trendOption = computed<EChartsOption>(() => ({
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
    data: trendPoints.value.map((item) => item.name),
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
      name: 'AIS 动态',
      data: trendPoints.value.map((item) => item.value),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 7,
      areaStyle: { color: 'rgba(0, 229, 255, 0.18)' },
      lineStyle: { color: '#00e5ff', width: 3 },
      itemStyle: { color: '#20ff9f', borderColor: '#071224', borderWidth: 2 },
    },
  ],
}))

const routeLoadOption = computed<EChartsOption>(() => {
  const activityFallback = routeSignals.map((route, index) => ({
    name: route.name.slice(0, 4),
    value: Number(route.volume.match(/\d+/)?.[0] || 0) + (observerActivity.value[index]?.value || 0),
  }))

  return {
    color: ['#20ff9f', '#ff4fd8'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(6, 10, 24, 0.92)',
      borderColor: 'rgba(0, 229, 255, 0.24)',
      textStyle: { color: '#e8f3ff' },
    },
    grid: { left: 36, right: 18, top: 34, bottom: 44 },
    xAxis: {
      type: 'category',
      data: activityFallback.map((item) => item.name),
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
        name: '日均载荷',
        type: 'bar',
        data: activityFallback.map((item) => item.value),
        itemStyle: { borderRadius: [8, 8, 2, 2] },
      },
    ],
  }
})

function modeAllows(layer: 'traffic' | 'risk' | 'anchor') {
  return selectedMapMode.value === 'all' || selectedMapMode.value === layer
}

function toLatLng(point: [number, number]) {
  return toMapDisplayPoint(point[0], point[1])
}

function createHeatColor(intensity: number) {
  if (intensity > 0.85) {
    return '#ff4f6a'
  }
  if (intensity > 0.7) {
    return '#ffb84d'
  }
  return '#20ff9f'
}

function createRiskColor(level: RiskZone['level']) {
  if (level === 'high') {
    return '#ff4f6a'
  }
  if (level === 'medium') {
    return '#ffb84d'
  }
  return '#7c3cff'
}

function addPorts() {
  if (!overlayLayer) {
    return
  }
  const layer = overlayLayer

  portSignals.forEach((port) => {
    const marker = L.marker(toMapDisplayPoint(port.lat, port.lng), {
      icon: createMapMarkerIcon(port.name, { tone: port.tone }),
      zIndexOffset: 600,
    })

    marker
      .bindTooltip(port.name, {
        className: 'dashboard-map-tooltip',
        direction: 'top',
        offset: [0, -36],
        permanent: true,
      })
      .bindPopup(
        buildMapPopupCard({
          eyebrow: port.kind,
          title: port.name,
          subtitle: port.status,
          chips: [
            { label: '泊位使用', value: `${port.berthUsage}%` },
            { label: '等待队列', value: port.queue },
          ],
        }),
        { className: 'gsmv-map-popup' },
      )
      .addTo(layer)
  })
}

function addRoutes() {
  if (!overlayLayer || !modeAllows('traffic')) {
    return
  }
  const layer = overlayLayer

  routeSignals.forEach((route) => {
    const polyline = L.polyline(route.path.map(toLatLng), {
      color: route.color,
      weight: route.id === 'terminal-shuttle' ? 4 : 5,
      opacity: 0.88,
      dashArray: route.id === 'terminal-shuttle' ? '8 10' : undefined,
      lineCap: 'round',
    })

    polyline
      .bindPopup(
        buildMapPopupCard({
          eyebrow: 'Route Signal',
          title: route.name,
          subtitle: route.description,
          chips: [
            { label: '日均流量', value: route.volume },
            { label: '风险', value: route.risk },
          ],
        }),
        { className: 'gsmv-map-popup' },
      )
      .addTo(layer)

    const endPoint = route.path.at(-1)
    if (endPoint) {
      L.circleMarker(toLatLng(endPoint), {
        radius: 5,
        color: route.color,
        fillColor: route.color,
        fillOpacity: 0.9,
        weight: 2,
      }).addTo(layer)
    }
  })
}

function addHeatCells() {
  if (!overlayLayer) {
    return
  }
  const layer = overlayLayer

  heatCells
    .filter((cell) => modeAllows(cell.type))
    .forEach((cell) => {
      const color = createHeatColor(cell.intensity)
      L.circle(toMapDisplayPoint(cell.lat, cell.lng), {
        radius: cell.radius,
        color,
        fillColor: color,
        fillOpacity: 0.18 + cell.intensity * 0.2,
        opacity: 0.7,
        weight: 1.5,
        className: 'dashboard-heat-cell',
      })
        .bindPopup(
          buildMapPopupCard({
            eyebrow: cell.type === 'anchor' ? 'Anchorage Heat' : 'Traffic Heat',
            title: cell.name,
            subtitle: cell.detail,
            chips: [{ label: '热力强度', value: `${Math.round(cell.intensity * 100)}%` }],
          }),
          { className: 'gsmv-map-popup' },
        )
        .addTo(layer)
    })
}

function addRiskZones() {
  if (!overlayLayer || !modeAllows('risk')) {
    return
  }
  const layer = overlayLayer

  riskZones.forEach((zone) => {
    const color = createRiskColor(zone.level)
    L.circle(toMapDisplayPoint(zone.lat, zone.lng), {
      radius: zone.radius,
      color,
      fillColor: color,
      fillOpacity: 0.12,
      opacity: 0.86,
      weight: 2,
      dashArray: zone.level === 'high' ? undefined : '7 8',
    })
      .bindPopup(
        buildMapPopupCard({
          eyebrow: 'Risk Zone',
          title: zone.name,
          subtitle: zone.detail,
          chips: [{ label: '级别', value: zone.level === 'high' ? '高' : zone.level === 'medium' ? '中' : '观察' }],
        }),
        { className: 'gsmv-map-popup' },
      )
      .addTo(layer)
  })
}

function addVesselTargets() {
  if (!overlayLayer || selectedMapMode.value === 'risk') {
    return
  }
  const layer = overlayLayer

  vesselTargets.forEach((target) => {
    L.marker(toMapDisplayPoint(target.lat, target.lng), {
      icon: createMapMarkerIcon(target.name, { compact: true, tone: target.tone, active: target.status.includes('等待') }),
      zIndexOffset: 500,
    })
      .bindPopup(
        buildMapPopupCard({
          eyebrow: 'AIS Target',
          title: target.name,
          subtitle: target.status,
          chips: [{ label: '航速', value: target.speed }],
        }),
        { className: 'gsmv-map-popup' },
      )
      .addTo(layer)
  })
}

function renderOperationsMap() {
  if (!map || !overlayLayer) {
    return
  }

  overlayLayer.clearLayers()
  addHeatCells()
  addRoutes()
  addRiskZones()
  addVesselTargets()
  addPorts()
}

function initializeMap() {
  if (map || !mapRef.value) {
    return
  }

  map = L.map(mapRef.value, {
    zoomControl: true,
    attributionControl: true,
    preferCanvas: true,
  }).setView(toMapDisplayPoint(33.73, -118.28), 10)

  addPreferredTileLayer(map)
  overlayLayer = L.layerGroup().addTo(map)
  renderOperationsMap()

  const bounds = L.latLngBounds([
    toMapDisplayPoint(33.45, -118.62),
    toMapDisplayPoint(34.02, -118.05),
  ])
  map.fitBounds(bounds, { padding: [26, 26], maxZoom: 11 })

  resizeObserver = new ResizeObserver(() => map?.invalidateSize(false))
  resizeObserver.observe(mapRef.value)
  window.setTimeout(() => map?.invalidateSize(false), 120)
}

async function loadDashboard() {
  try {
    const [summaryData, trend, observers] = await Promise.all([
      fetchDashboardSummary(),
      fetchObservationTrend(30),
      fetchObservationActivity(30),
    ])

    summary.value = summaryData
    trendData.value = trend
    observerActivity.value = observers
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '态势总览加载失败，已保留港区态势示例图层')
  }
}

watch(selectedMapMode, () => {
  renderOperationsMap()
})

onMounted(async () => {
  await nextTick()
  initializeMap()
  updateWeatherTime()
  timeTimer = setInterval(updateWeatherTime, 1000)
  void loadWeather()
  stopDataSync = listenDataChanged((detail) => {
    if (['species', 'observation', 'ecosystem', 'user'].includes(detail.type)) {
      void loadDashboard()
    }
  })
  void loadDashboard()
})

onBeforeUnmount(() => {
  stopDataSync?.()
  resizeObserver?.disconnect()
  resizeObserver = null
  if (weatherTimer) {
    clearInterval(weatherTimer)
    weatherTimer = null
  }
  if (timeTimer) {
    clearInterval(timeTimer)
    timeTimer = null
  }
  overlayLayer = null
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
.dashboard-focus-item,
.dashboard-route-card {
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
.dashboard-focus-item::after,
.dashboard-route-card::after {
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

.dashboard-focus-list,
.dashboard-alert-list,
.dashboard-route-list {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
  padding: 14px;
}

.dashboard-focus-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  min-height: 106px;
  padding: 14px;
  border-radius: 20px;
}

.dashboard-focus-item span,
.dashboard-alert span {
  color: var(--gsmv-primary);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.dashboard-focus-item strong,
.dashboard-alert strong,
.dashboard-route-card strong {
  display: block;
  margin-top: 6px;
  color: #ffffff;
  font-size: 16px;
}

.dashboard-focus-item p,
.dashboard-alert p,
.dashboard-route-card p {
  margin: 8px 0 0;
  color: rgba(232, 243, 255, 0.62);
  font-size: 12px;
  line-height: 1.55;
}

.dashboard-focus-item__score {
  flex: 0 0 76px;
  display: grid;
  place-items: center;
  align-self: stretch;
  border: 1px solid rgba(0, 229, 255, 0.16);
  border-radius: 18px;
  background: rgba(0, 229, 255, 0.08);
}

.dashboard-focus-item__score b {
  color: var(--gsmv-accent);
  font-size: 20px;
}

.dashboard-focus-item__score small {
  color: rgba(232, 243, 255, 0.56);
  font-size: 11px;
}

.dashboard-alert {
  padding: 13px 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.055);
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

.dashboard-weather-panel__time {
  display: block;
  margin-top: 4px;
  color: rgba(232, 243, 255, 0.56);
  font-size: 12px;
  font-weight: 600;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
}

.dashboard-weather-panel__body {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 12px;
  padding: 14px;
}

.dashboard-weather-panel__status {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(232, 243, 255, 0.72);
  font-size: 13px;
  min-height: 80px;
}

.dashboard-weather-panel__spin {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 229, 255, 0.28);
  border-top-color: var(--gsmv-primary);
  border-radius: 50%;
  animation: dashboard-spin 0.8s linear infinite;
}

.dashboard-weather-panel__error {
  color: var(--gsmv-danger);
  line-height: 1.55;
}

.dashboard-weather-panel__result {
  display: grid;
  gap: 12px;
}

.dashboard-weather-panel__interpretation {
  padding: 12px;
  border: 1px solid rgba(32, 255, 159, 0.22);
  border-radius: 14px;
  background: rgba(32, 255, 159, 0.08);
}

.dashboard-weather-panel__interpretation strong {
  display: block;
  margin-bottom: 6px;
  color: var(--gsmv-accent);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.dashboard-weather-panel__interpretation p {
  margin: 0;
  color: #e8f3ff;
  font-size: 13px;
  line-height: 1.65;
  white-space: pre-wrap;
}

.dashboard-weather-panel__raw {
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.04);
}

.dashboard-weather-panel__raw strong {
  display: block;
  margin-bottom: 6px;
  color: rgba(232, 243, 255, 0.62);
  font-size: 11px;
  font-weight: 700;
}

.dashboard-weather-panel__raw pre {
  margin: 0;
  color: rgba(232, 243, 255, 0.72);
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
}

.dashboard-weather-panel__hint {
  margin: 4px 0 0;
  padding: 10px 12px;
  border: 1px solid rgba(255, 184, 77, 0.2);
  border-radius: 12px;
  background: rgba(255, 184, 77, 0.08);
  color: rgba(232, 243, 255, 0.68);
  font-size: 12px;
  line-height: 1.55;
}

.dashboard-weather-panel__hint a {
  color: var(--gsmv-primary);
  text-decoration: none;
  font-weight: 700;
}

.dashboard-weather-panel__hint a:hover {
  text-decoration: underline;
}

@keyframes dashboard-spin {
  to {
    transform: rotate(360deg);
  }
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

:global(.dashboard-map-tooltip) {
  border: 1px solid rgba(0, 229, 255, 0.24) !important;
  border-radius: 999px !important;
  background: rgba(5, 8, 22, 0.82) !important;
  box-shadow: 0 14px 30px rgba(0, 4, 18, 0.32) !important;
  color: #e8f3ff !important;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
  padding: 7px 10px !important;
  backdrop-filter: blur(14px);
}

:global(.dashboard-map-tooltip::before) {
  display: none;
}

:global(.dashboard-heat-cell) {
  filter: drop-shadow(0 0 18px rgba(255, 184, 77, 0.28));
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

  .dashboard-intel-rail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-lower-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 980px) {
  .dashboard-kpi-grid,
  .dashboard-intel-rail,
  .dashboard-lower-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-map {
    min-height: 460px;
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
</style>
