<template>
  <div class="marine-traffic-page">
    <div ref="mapRef" class="traffic-map" />

    <div
      v-if="hoveredVessel"
      class="map-hover-card"
      :class="{ 'is-below': hoveredVessel.placement === 'below' }"
      :style="{ left: `${hoveredVessel.x}px`, top: `${hoveredVessel.y}px` }"
    >
      <strong>{{ hoveredVessel.summary.name }}</strong>
      <span>{{ hoveredVessel.summary.typeLabel }} · {{ worldLabel(hoveredVessel.offset) }}</span>
      <dl>
        <div>
          <dt>位置</dt>
          <dd>{{ positionLabel(hoveredVessel.summary.latest) }}</dd>
        </div>
        <div>
          <dt>航速</dt>
          <dd>{{ hoveredVessel.summary.speedLabel }}</dd>
        </div>
        <div>
          <dt>航向</dt>
          <dd>{{ hoveredVessel.summary.courseLabel }}</dd>
        </div>
      </dl>
    </div>

    <aside class="left-rail" aria-label="Marine map navigation">
      <RouterLink to="/dashboard" class="rail-button rail-button--home" title="返回系统">
        <el-icon><House /></el-icon>
      </RouterLink>
      <button class="rail-button is-active" type="button" title="实时船舶">
        <el-icon><MapLocation /></el-icon>
      </button>
      <button class="rail-button" type="button" title="港口与锚地">
        <el-icon><Location /></el-icon>
      </button>
      <button class="rail-button" type="button" title="轨迹线">
        <el-icon><DataLine /></el-icon>
      </button>
      <button class="rail-button" type="button" title="图层">
        <el-icon><Operation /></el-icon>
      </button>
      <button class="rail-button" type="button" title="告警">
        <el-icon><Warning /></el-icon>
      </button>
    </aside>

    <div class="search-bar">
      <el-icon><Search /></el-icon>
      <input v-model="keyword" type="search" placeholder="Search MarineTraffic" @keydown.enter="applyKeyword" />
      <button v-if="keyword" type="button" @click="clearKeyword">Clear</button>
    </div>

    <div class="top-status">
      <span>ShipInsight AIS</span>
      <strong>{{ latestLabel }}</strong>
    </div>

    <section class="vessel-panel" :class="{ 'is-collapsed': panelCollapsed }">
      <header>
        <div>
          <span>LIVE AIS LAYERS</span>
          <strong>{{ filteredVesselCount }} vessels</strong>
          <small v-if="mapTotalVessels > filteredVesselCount">loaded {{ filteredVesselCount }} / {{ mapTotalVessels }}</small>
        </div>
        <button type="button" @click="panelCollapsed = !panelCollapsed">
          {{ panelCollapsed ? 'Open' : 'Close' }}
        </button>
      </header>

      <template v-if="!panelCollapsed">
        <div class="layer-toggles">
          <label>
            <input v-model="visibleLayers.vessels" type="checkbox" />
            AIS 点
          </label>
          <label>
            <input v-model="visibleLayers.tracks" type="checkbox" />
            轨迹线
          </label>
          <label>
            <input v-model="visibleLayers.history" type="checkbox" />
            历史点
          </label>
        </div>

        <div class="dataset-date-filter">
          <label for="dataset-date-select">数据集日期</label>
          <div class="dataset-date-filter__control">
            <select id="dataset-date-select" v-model="selectedDatasetDate" :disabled="loading" @change="applyDatasetDate">
              <option value="">最新日期</option>
              <option v-for="date in datasetDates" :key="date" :value="date">{{ date }}</option>
            </select>
            <button type="button" :disabled="!selectedDatasetDate || loading" @click="clearDatasetDate">最新</button>
          </div>
        </div>

        <div class="metric-grid">
          <div>
            <span>AIS records</span>
            <strong>{{ mapTotalRecords || filteredRecords.length }}</strong>
          </div>
          <div>
            <span>Tracks</span>
            <strong>{{ trackCount }}</strong>
          </div>
          <div>
            <span>Avg speed</span>
            <strong>{{ averageSpeedLabel }}</strong>
          </div>
        </div>

        <div class="vessel-number-picker">
          <label for="vessel-number-query">船只编号 / MMSI</label>
          <div class="vessel-number-picker__control">
            <input
              id="vessel-number-query"
              v-model="vesselNumberQuery"
              type="search"
              placeholder="输入 MMSI 后回车定位轨迹"
              autocomplete="off"
              @keydown.enter.prevent="selectVesselNumberQuery"
            />
            <button type="button" :disabled="!vesselNumberQuery.trim()" @click="selectVesselNumberQuery">定位</button>
          </div>
          <div v-if="vesselNumberQuery.trim() && vesselNumberOptions.length" class="vessel-number-picker__options">
            <button
              v-for="item in vesselNumberOptions"
              :key="item.mmsi"
              type="button"
              :class="{ 'is-selected': selectedMmsi === item.mmsi }"
              @click="selectVesselByNumber(item.mmsi)"
            >
              <strong>{{ item.mmsi }}</strong>
              <span>{{ item.name }} · {{ item.pointCount }} pts</span>
            </button>
          </div>
          <div v-else-if="vesselNumberQuery.trim()" class="vessel-number-picker__empty">没有匹配的船只编号</div>
          <div v-if="selectedVessel" class="selected-vessel-strip">
            <div>
              <span>{{ trackVisible ? '已显示轨迹' : '已选中船只' }}</span>
              <strong>{{ selectedVessel.name }}</strong>
              <small>MMSI {{ selectedVessel.mmsi }} · {{ trackVisible ? `${selectedVessel.pointCount} 个轨迹点` : '最新日期船位' }}</small>
            </div>
            <div class="selected-vessel-strip__actions">
              <button v-if="!trackVisible" type="button" @click="showSelectedVesselTrack">显示轨迹</button>
              <button v-else type="button" @click="hideSelectedVesselTrack">隐藏轨迹</button>
              <button type="button" @click="clearVesselSelection">取消</button>
            </div>
          </div>
        </div>

        <div class="legend">
          <div v-if="mapTotalVessels > filteredVesselCount" class="legend-note">
            默认显示数据集最新日期的船位快照；选中船只并点击“显示轨迹”后才加载该船全部 AIS 点。
          </div>
          <div v-for="item in legendItems" :key="item.label">
            <span :style="{ background: item.color }" />
            {{ item.label }}
          </div>
        </div>

        <div class="vessel-list">
          <button
            v-for="item in vesselListItems"
            :key="item.mmsi"
            type="button"
            :class="{ 'is-selected': selectedMmsi === item.mmsi }"
            @click="focusVessel(item.mmsi)"
          >
            <span class="vessel-list__type">{{ item.typeLabel }}</span>
            <strong>{{ item.name }}</strong>
            <small>MMSI {{ item.mmsi }} · {{ item.pointCount }} pts</small>
            <span>{{ item.positionLabel }}</span>
          </button>
          <div v-if="!vesselSummaries.length" class="empty-panel">No AIS data matched.</div>
        </div>
      </template>
    </section>

    <div class="right-toolbar">
      <button type="button" title="灰色海图" @click="switchBaseLayer('light')" :class="{ 'is-active': baseLayerMode === 'light' }">
        <el-icon><MapLocation /></el-icon>
      </button>
      <button type="button" title="OSM 标准底图" @click="switchBaseLayer('osm')" :class="{ 'is-active': baseLayerMode === 'osm' }">
        <el-icon><Location /></el-icon>
      </button>
      <button type="button" title="刷新 AIS" :disabled="loading" @click="reload">
        <el-icon><Refresh /></el-icon>
      </button>
      <button type="button" title="回到数据范围" @click="fitDataBounds">
        <el-icon><Aim /></el-icon>
      </button>
    </div>

    <div class="zoom-stack">
      <button type="button" @click="zoomBy(1)">+</button>
      <span>{{ zoomLevel }}</span>
      <button type="button" @click="zoomBy(-1)">−</button>
    </div>

    <div class="coordinate-readout">
      <strong>{{ pointerLabel }}</strong>
      <span>{{ selectedVessel ? `${selectedVessel.latest.latitude.toFixed(5)}, ${selectedVessel.latest.longitude.toFixed(5)}` : mapCenterLabel }}</span>
    </div>

    <div v-if="loading" class="loading-mask">Loading AIS layer...</div>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import {
  Aim,
  DataLine,
  House,
  Location,
  MapLocation,
  Operation,
  Refresh,
  Search,
  Warning,
} from '@element-plus/icons-vue'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchAisDatasetDates, fetchAisMapRecords, fetchAisRecords, fetchAisVesselTrack } from '@/api/aisRecords'
import type { AisRecordView } from '@/types/gsmv'

type BaseLayerMode = 'light' | 'osm'
type WorldOffset = (typeof WORLD_COPY_OFFSETS)[number]

interface VesselSummary {
  mmsi: string
  name: string
  typeLabel: string
  color: string
  latest: AisRecordView
  records: AisRecordView[]
  pointCount: number
  positionLabel: string
  speedLabel: string
  courseLabel: string
  headingLabel: string
  timeLabel: string
  imo?: string
}

interface HoveredVessel {
  summary: VesselSummary
  offset: number
  x: number
  y: number
  placement: 'above' | 'below'
}

const MAP_LATEST_LIMIT = 50000
const VESSEL_TRACK_LIMIT = 20000
const WORLD_COPY_OFFSETS = [-360, 0, 360] as const
const WORLD_LABELS = [
  { label: '-360° 世界', lng: -360 },
  { label: '0° 世界', lng: 0 },
  { label: '+360° 世界', lng: 360 },
]
const WORLD_EDGE_LONGITUDES = [-540, -180, 180, 540]
const TRIPLE_WORLD_BOUNDS = L.latLngBounds([
  [-85, -540],
  [85, 540],
])
const DEFAULT_CENTER: [number, number] = [18, 0]

interface VesselRenderTarget {
  summary: VesselSummary
  offset: number
  x: number
  y: number
}

class VesselCanvasLayer extends L.Layer {
  private canvas: HTMLCanvasElement | null = null
  private context: CanvasRenderingContext2D | null = null
  private mapInstance: L.Map | null = null
  private frameId = 0
  private summaries: VesselSummary[] = []
  private selectedMmsi = ''
  private visible = true
  private targets: VesselRenderTarget[] = []

  onAdd(targetMap: L.Map) {
    this.mapInstance = targetMap
    this.canvas = L.DomUtil.create('canvas', 'ais-vessel-canvas') as HTMLCanvasElement
    this.canvas.style.position = 'absolute'
    this.canvas.style.pointerEvents = 'none'
    this.context = this.canvas.getContext('2d')
    targetMap.getPanes().overlayPane.appendChild(this.canvas)
    targetMap.on('moveend zoomend resize viewreset', this.scheduleRedraw)
    this.scheduleRedraw()
    return this
  }

  onRemove(targetMap: L.Map) {
    targetMap.off('moveend zoomend resize viewreset', this.scheduleRedraw)
    if (this.frameId) {
      window.cancelAnimationFrame(this.frameId)
      this.frameId = 0
    }
    this.canvas?.remove()
    this.canvas = null
    this.context = null
    this.mapInstance = null
    this.targets = []
    return this
  }

  setData(summaries: VesselSummary[], selectedMmsi: string, visible: boolean) {
    this.summaries = summaries
    this.selectedMmsi = selectedMmsi
    this.visible = visible
    this.scheduleRedraw()
  }

  nearestTarget(point: L.Point, hitRadius: number) {
    const radiusSquared = hitRadius * hitRadius
    let nearest: (VesselRenderTarget & { distanceSquared: number }) | null = null

    for (const target of this.targets) {
      const dx = target.x - point.x
      const dy = target.y - point.y
      const distanceSquared = dx * dx + dy * dy
      if (distanceSquared > radiusSquared || (nearest && distanceSquared >= nearest.distanceSquared)) {
        continue
      }
      nearest = { ...target, distanceSquared }
    }

    return nearest
  }

  private scheduleRedraw = () => {
    if (this.frameId) {
      return
    }
    this.frameId = window.requestAnimationFrame(() => {
      this.frameId = 0
      this.redraw()
    })
  }

  private redraw() {
    if (!this.mapInstance || !this.canvas || !this.context) {
      return
    }

    const currentMap = this.mapInstance
    const canvas = this.canvas
    const context = this.context
    const size = currentMap.getSize()
    const pixelRatio = window.devicePixelRatio || 1
    const topLeft = currentMap.containerPointToLayerPoint([0, 0])

    L.DomUtil.setPosition(canvas, topLeft)
    canvas.style.width = `${size.x}px`
    canvas.style.height = `${size.y}px`

    const targetWidth = Math.max(1, Math.round(size.x * pixelRatio))
    const targetHeight = Math.max(1, Math.round(size.y * pixelRatio))
    if (canvas.width !== targetWidth || canvas.height !== targetHeight) {
      canvas.width = targetWidth
      canvas.height = targetHeight
    }

    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)
    context.clearRect(0, 0, size.x, size.y)
    this.targets = []

    if (!this.visible) {
      return
    }

    for (const summary of this.summaries) {
      for (const offset of WORLD_COPY_OFFSETS) {
        const point = currentMap.latLngToContainerPoint([summary.latest.latitude, summary.latest.longitude + offset])
        if (point.x < -30 || point.x > size.x + 30 || point.y < -30 || point.y > size.y + 30) {
          continue
        }
        this.targets.push({ summary, offset, x: point.x, y: point.y })
        this.drawVessel(point.x, point.y, summary, this.selectedMmsi === summary.mmsi)
      }
    }
  }

  private drawVessel(x: number, y: number, summary: VesselSummary, selected: boolean) {
    const context = this.context
    if (!context) {
      return
    }

    const heading = normalizeHeading(summary.latest.heading, summary.latest.cog)
    context.save()
    context.translate(x, y)

    if (selected) {
      context.beginPath()
      context.arc(0, 0, 17, 0, Math.PI * 2)
      context.strokeStyle = 'rgba(21, 145, 180, 0.72)'
      context.lineWidth = 2
      context.stroke()
    }

    context.rotate((heading * Math.PI) / 180)
    context.beginPath()
    context.moveTo(0, -14)
    context.lineTo(8, 11)
    context.lineTo(0, 7)
    context.lineTo(-8, 11)
    context.closePath()
    context.fillStyle = summary.color
    context.strokeStyle = 'rgba(36, 48, 56, 0.38)'
    context.lineWidth = 1
    context.fill()
    context.stroke()

    context.beginPath()
    context.arc(0, 2, 2.6, 0, Math.PI * 2)
    context.fillStyle = '#ffffff'
    context.fill()
    context.restore()
  }
}

const mapRef = ref<HTMLDivElement>()
const records = ref<AisRecordView[]>([])
const selectedTrackRecords = ref<AisRecordView[]>([])
const loading = ref(false)
const mapTotalVessels = ref(0)
const mapTotalRecords = ref(0)
const trackVisible = ref(false)
const keyword = ref('')
const appliedKeyword = ref('')
const datasetDates = ref<string[]>([])
const selectedDatasetDate = ref('')
const vesselNumberQuery = ref('')
const selectedMmsi = ref('')
const selectedWorldOffset = ref<WorldOffset>(0)
const hoveredVessel = ref<HoveredVessel | null>(null)
const panelCollapsed = ref(false)
const baseLayerMode = ref<BaseLayerMode>('light')
const zoomLevel = ref(2)
const pointerLabel = ref('15° 33.67 S')
const mapCenterLabel = ref('33° 45.00 W')

const visibleLayers = reactive({
  vessels: true,
  tracks: true,
  history: true,
})

let map: L.Map | null = null
let baseLayer: L.TileLayer | null = null
let labelLayer: L.TileLayer | null = null
let vesselCanvasLayer: VesselCanvasLayer | null = null
let historyLayer: L.LayerGroup | null = null
let trackLayer: L.LayerGroup | null = null
let worldGuideLayer: L.LayerGroup | null = null
let activeDetailPopup: L.Popup | null = null
let pointerFrameId = 0
let pendingPointerEvent: { latLng: L.LatLng; containerPoint: L.Point } | null = null

const legendItems = [
  { label: 'Cargo / Tanker', color: '#2fb65d' },
  { label: 'Fast / Active', color: '#ef3f36' },
  { label: 'Passenger / Special', color: '#12b8c7' },
  { label: 'Unknown / Slow', color: '#d47b4c' },
]

const displayRecords = computed(() => {
  if (!selectedMmsi.value || !selectedTrackRecords.value.length) {
    return records.value
  }
  return [
    ...records.value.filter((item) => item.mmsi !== selectedMmsi.value),
    ...selectedTrackRecords.value,
  ]
})

const filteredRecords = computed(() => {
  const source = validRecords(displayRecords.value)
  const needle = appliedKeyword.value.trim().toLowerCase()
  if (!needle) {
    return source
  }
  return source.filter((item) => {
    if (trackVisible.value && selectedMmsi.value && item.mmsi === selectedMmsi.value) {
      return true
    }
    return [item.mmsi, item.vesselName, item.imo, item.callSign, item.note, String(item.vesselType ?? '')]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  })
})

function groupRecordsByMmsi(source: AisRecordView[]) {
  const groups = new Map<string, AisRecordView[]>()
  for (const record of source) {
    if (!groups.has(record.mmsi)) {
      groups.set(record.mmsi, [])
    }
    groups.get(record.mmsi)?.push(record)
  }
  groups.forEach((items) => items.sort((a, b) => timestamp(a.baseDateTime) - timestamp(b.baseDateTime)))
  return groups
}

function summarizeGroups(groups: Map<string, AisRecordView[]>) {
  return [...groups.entries()]
    .map(([mmsi, items]) => buildSummary(mmsi, items))
    .sort((a, b) => timestamp(b.latest.baseDateTime) - timestamp(a.latest.baseDateTime))
}

const allVesselSummaries = computed<VesselSummary[]>(() => summarizeGroups(groupRecordsByMmsi(validRecords(displayRecords.value))))

const vesselGroups = computed(() => groupRecordsByMmsi(filteredRecords.value))

const vesselSummaries = computed<VesselSummary[]>(() => summarizeGroups(vesselGroups.value))

const selectedVessel = computed(() => vesselSummaries.value.find((item) => item.mmsi === selectedMmsi.value))
const vesselNumberOptions = computed(() => {
  const needle = vesselNumberQuery.value.trim().toLowerCase()
  if (!needle) {
    return []
  }
  return allVesselSummaries.value
    .filter((item) =>
      [item.mmsi, item.name, item.imo, item.latest.callSign]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(needle)),
    )
    .sort((a, b) => {
      const aExact = a.mmsi === needle ? 0 : a.mmsi.startsWith(needle) ? 1 : 2
      const bExact = b.mmsi === needle ? 0 : b.mmsi.startsWith(needle) ? 1 : 2
      return aExact - bExact || timestamp(b.latest.baseDateTime) - timestamp(a.latest.baseDateTime)
    })
    .slice(0, 8)
})
const filteredVesselCount = computed(() => vesselSummaries.value.length)
const vesselListItems = computed(() => vesselSummaries.value.slice(0, 150))
const trackCount = computed(() => vesselSummaries.value.filter((item) => item.records.length >= 2).length)
const latestLabel = computed(() => vesselSummaries.value[0]?.timeLabel || 'No AIS')
const averageSpeedLabel = computed(() => {
  const speeds = filteredRecords.value.map((item) => item.sog).filter((value): value is number => typeof value === 'number')
  if (!speeds.length) {
    return '0 kn'
  }
  const avg = speeds.reduce((sum, value) => sum + value, 0) / speeds.length
  return `${avg.toFixed(1)} kn`
})

function validRecords(source: AisRecordView[]) {
  return source.filter((item) => Number.isFinite(item.latitude) && Number.isFinite(item.longitude) && item.mmsi)
}

function timestamp(value?: string) {
  if (!value) {
    return 0
  }
  return new Date(value.includes('T') ? value : value.replace(' ', 'T')).getTime()
}

function displayTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.includes('T') ? value.replace('T', ' ') : value
}

function normalizeHeading(value?: number | null, fallback?: number | null) {
  const candidate = typeof value === 'number' && value >= 0 && value <= 360 ? value : fallback
  return typeof candidate === 'number' && Number.isFinite(candidate) ? candidate : 0
}

function typeLabel(record: AisRecordView) {
  const value = record.vesselType
  if (value == null) {
    return 'Unknown Vessel'
  }
  if (value >= 70 && value < 90) {
    return value >= 80 ? 'Tanker' : 'Cargo Vessel'
  }
  if (value >= 60 && value < 70) {
    return 'Passenger Vessel'
  }
  if (value >= 30 && value < 40) {
    return 'Special Craft'
  }
  return `Type ${value}`
}

function vesselColor(record: AisRecordView) {
  const type = record.vesselType ?? 0
  const speed = record.sog ?? 0
  if (type >= 60 && type < 70) return '#12b8c7'
  if (speed >= 8) return '#ef3f36'
  if (type >= 70 && type < 90) return '#2fb65d'
  return '#d47b4c'
}

function buildSummary(mmsi: string, items: AisRecordView[]): VesselSummary {
  const latest = items[items.length - 1]
  const name = latest.vesselName || `MMSI ${mmsi}`
  return {
    mmsi,
    name,
    typeLabel: typeLabel(latest),
    color: vesselColor(latest),
    latest,
    records: items,
    pointCount: items.length,
    positionLabel: `${latest.latitude.toFixed(4)}, ${latest.longitude.toFixed(4)}`,
    speedLabel: typeof latest.sog === 'number' ? `${latest.sog.toFixed(1)} kn` : '-',
    courseLabel: typeof latest.cog === 'number' ? `${latest.cog.toFixed(1)}°` : '-',
    headingLabel: typeof latest.heading === 'number' ? `${latest.heading}°` : '-',
    timeLabel: displayTime(latest.baseDateTime),
    imo: latest.imo,
  }
}

function navigationStatusLabel(status?: number | null) {
  const labels: Record<number, string> = {
    0: 'Under way using engine',
    1: 'At anchor',
    2: 'Not under command',
    3: 'Restricted manoeuvrability',
    4: 'Constrained by draught',
    5: 'Moored',
    6: 'Aground',
    7: 'Engaged in fishing',
    8: 'Under way sailing',
    15: 'Not defined',
  }
  return typeof status === 'number' ? labels[status] || `Status ${status}` : 'Not reported'
}

function dimensionLabel(record: AisRecordView) {
  const length = typeof record.length === 'number' ? `${record.length}m` : '-'
  const width = typeof record.width === 'number' ? `${record.width}m` : '-'
  return `${length} / ${width}`
}

function draftLabel(record: AisRecordView) {
  return typeof record.draft === 'number' ? `${record.draft.toFixed(1)}m` : '-'
}

function positionLabel(record: AisRecordView) {
  return `${record.latitude.toFixed(5)}, ${record.longitude.toFixed(5)}`
}

function receivedLabel(record: AisRecordView) {
  return displayTime(record.importedAt || record.baseDateTime)
}

async function loadAisMapRecords() {
  loading.value = true
  closeActiveDetailPopup()
  try {
    const datasetDateRange = datasetDateToRange(selectedDatasetDate.value)
    const [pageData, recordCountData] = await Promise.all([
      fetchAisMapRecords({
        keyword: appliedKeyword.value.trim() || undefined,
        datasetDate: selectedDatasetDate.value || undefined,
        limit: MAP_LATEST_LIMIT,
      }),
      fetchAisRecords({
        keyword: appliedKeyword.value.trim() || undefined,
        observedFrom: datasetDateRange?.from,
        observedTo: datasetDateRange?.to,
        page: 1,
        size: 1,
      }),
    ])
    records.value = pageData.items
    mapTotalVessels.value = pageData.total
    mapTotalRecords.value = recordCountData.total
    renderMapLayers()
    fitDataBounds()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AIS 地图数据加载失败')
  } finally {
    loading.value = false
  }
}

async function loadDatasetDates() {
  try {
    datasetDates.value = await fetchAisDatasetDates()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AIS 数据集日期加载失败')
  }
}

function datasetDateToRange(date: string) {
  if (!date) {
    return null
  }
  return {
    from: `${date}T00:00:00`,
    to: `${date}T23:59:59`,
  }
}

async function loadVesselTrackRecords(mmsi: string) {
  const pageData = await fetchAisVesselTrack(mmsi, VESSEL_TRACK_LIMIT)
  return pageData.items
}

function createBaseLayer(mode: BaseLayerMode) {
  if (mode === 'osm') {
    return L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap',
      bounds: TRIPLE_WORLD_BOUNDS,
      maxZoom: 18,
    })
  }
  return L.tileLayer('https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; OpenStreetMap &copy; CARTO',
    bounds: TRIPLE_WORLD_BOUNDS,
    subdomains: 'abcd',
    maxZoom: 19,
  })
}

function createLabelLayer() {
  return L.tileLayer('https://{s}.basemaps.cartocdn.com/light_only_labels/{z}/{x}/{y}{r}.png', {
    attribution: '&copy; CARTO',
    bounds: TRIPLE_WORLD_BOUNDS,
    subdomains: 'abcd',
    maxZoom: 19,
    opacity: 0.75,
  })
}

function initMap() {
  if (!mapRef.value || map) {
    return
  }

  map = L.map(mapRef.value, {
    zoomControl: false,
    worldCopyJump: false,
    maxBounds: TRIPLE_WORLD_BOUNDS,
    maxBoundsViscosity: 1,
    minZoom: 1,
    maxZoom: 14,
    zoomDelta: 0.5,
    zoomSnap: 0.25,
    preferCanvas: true,
  }).setView(DEFAULT_CENTER, 1)

  baseLayer = createBaseLayer(baseLayerMode.value).addTo(map)
  labelLayer = createLabelLayer().addTo(map)
  worldGuideLayer = L.layerGroup().addTo(map)
  trackLayer = L.layerGroup().addTo(map)
  historyLayer = L.layerGroup().addTo(map)
  vesselCanvasLayer = new VesselCanvasLayer().addTo(map)
  renderWorldGuides()

  map.on('zoomend', () => {
    zoomLevel.value = map?.getZoom() ?? 2
  })
  map.on('mousemove', handleMapPointerMove)
  map.on('click', handleMapClick)
  map.on('mouseout dragstart zoomstart', clearHover)
  map.on('moveend', () => {
    const center = map?.getCenter()
    if (center) {
      pointerLabel.value = formatLat(center.lat)
      mapCenterLabel.value = formatLng(center.lng)
    }
  })
}

function renderWorldGuides() {
  if (!worldGuideLayer) {
    return
  }
  const guides = worldGuideLayer
  guides.clearLayers()

  WORLD_EDGE_LONGITUDES.forEach((lng) => {
    L.polyline(
      [
        [-85, lng],
        [85, lng],
      ],
      {
        color: lng === -180 || lng === 180 ? '#778994' : '#a7b4bb',
        weight: lng === -180 || lng === 180 ? 1.2 : 0.8,
        opacity: lng === -180 || lng === 180 ? 0.42 : 0.28,
        dashArray: '7 10',
        interactive: false,
      },
    ).addTo(guides)
  })

  WORLD_LABELS.forEach((world) => {
    L.marker([72, world.lng], {
      interactive: false,
      icon: L.divIcon({
        className: 'world-copy-label',
        html: `<span>${world.label}</span>`,
        iconSize: L.point(112, 28),
        iconAnchor: L.point(56, 14),
      }),
    }).addTo(guides)
  })
}

function switchBaseLayer(mode: BaseLayerMode) {
  baseLayerMode.value = mode
  if (!map) {
    return
  }
  if (baseLayer) {
    map.removeLayer(baseLayer)
  }
  if (labelLayer) {
    map.removeLayer(labelLayer)
  }
  baseLayer = createBaseLayer(mode).addTo(map)
  labelLayer = createLabelLayer().addTo(map)
  renderMapLayers()
}

function worldLabel(offset: number) {
  if (offset > 0) {
    return `+${offset}° 世界`
  }
  if (offset < 0) {
    return `${offset}° 世界`
  }
  return '0° 世界'
}

function popupHtml(summary: VesselSummary, offset = 0) {
  const latest = summary.latest
  return `
    <article class="traffic-detail-card">
      <header class="traffic-detail-card__header">
        <div class="traffic-detail-card__flag">
          <span>AIS</span>
        </div>
        <div class="traffic-detail-card__title">
          <strong>${escapeHtml(summary.name)}</strong>
          <span>${escapeHtml(summary.typeLabel)} · ${escapeHtml(worldLabel(offset))}</span>
        </div>
      </header>

      <section class="traffic-detail-card__hero">
        <div>
          <small>当前位置</small>
          <strong>${escapeHtml(positionLabel(latest))}</strong>
        </div>
        <div>
          <small>MMSI</small>
          <strong>${escapeHtml(summary.mmsi)}</strong>
        </div>
        <div>
          <small>IMO / 呼号</small>
          <strong>${escapeHtml(summary.imo || latest.callSign || '-')}</strong>
        </div>
      </section>

      <section class="traffic-detail-card__route">
        <div class="traffic-detail-card__route-row">
          <strong>${escapeHtml(summary.speedLabel)}</strong>
          <span>${escapeHtml(summary.courseLabel)}</span>
          <strong>${escapeHtml(summary.headingLabel)}</strong>
        </div>
        <div class="traffic-detail-card__track">
          <span style="width: 58%"></span>
          <i></i>
        </div>
      </section>

      <section class="traffic-detail-card__grid">
        <div>
          <small>Navigational status</small>
          <strong>${escapeHtml(navigationStatusLabel(latest.status))}</strong>
        </div>
        <div>
          <small>Speed / Course</small>
          <strong>${escapeHtml(summary.speedLabel)} / ${escapeHtml(summary.courseLabel)}</strong>
        </div>
        <div>
          <small>Draught</small>
          <strong>${escapeHtml(draftLabel(latest))}</strong>
        </div>
        <div>
          <small>Dimensions</small>
          <strong>${escapeHtml(dimensionLabel(latest))}</strong>
        </div>
      </section>

      <footer class="traffic-detail-card__footer">
        <span>Last AIS: <strong>${escapeHtml(summary.timeLabel)}</strong></span>
        <span>Received: <strong>${escapeHtml(receivedLabel(latest))}</strong>${latest.sourceFile ? ` · ${escapeHtml(latest.sourceFile)}` : ''}</span>
      </footer>
    </article>
  `
}

function detailPopupOffset(targetMap: L.Map, latLng: L.LatLng) {
  const point = targetMap.latLngToContainerPoint(latLng)
  const container = targetMap.getContainer()
  let x = 0
  let y = 0

  if (point.x < 330) {
    x = 260
  } else if (point.x > container.clientWidth - 620) {
    x = -260
  }

  if (point.y < 430) {
    y = Math.min(220, 430 - point.y)
  }

  return L.point(x, y)
}

function openVesselDetail(summary: VesselSummary, offset: WorldOffset) {
  if (!map) {
    return
  }
  const currentMap = map
  clearHover()
  selectedMmsi.value = summary.mmsi
  selectedWorldOffset.value = offset
  vesselCanvasLayer?.setData(vesselSummaries.value, selectedMmsi.value, visibleLayers.vessels)
  clearHover()

  const latLng = L.latLng(summary.latest.latitude, summary.latest.longitude + offset)
  currentMap.panInside(latLng, {
    paddingTopLeft: L.point(280, 180),
    paddingBottomRight: L.point(520, 140),
    animate: true,
  })

  window.setTimeout(() => {
    const popup = L.popup({
      className: 'traffic-detail-shell',
      closeButton: true,
      maxWidth: 460,
      minWidth: 420,
      offset: detailPopupOffset(currentMap, latLng),
      autoPanPadding: L.point(32, 32),
    })
      .setLatLng(latLng)
      .setContent(popupHtml(summary, offset))

    activeDetailPopup = popup
    popup.on('remove', () => {
      if (activeDetailPopup !== popup) {
        return
      }
      activeDetailPopup = null
    })

    popup.openOn(currentMap)
    clearHover()
  }, 160)
}

function clearHover() {
  hoveredVessel.value = null
}

function hoverPointFor(summary: VesselSummary, offset: number) {
  if (!map) {
    return null
  }
  const point = map.latLngToContainerPoint([summary.latest.latitude, summary.latest.longitude + offset])
  const container = map.getContainer()
  const x = Math.min(Math.max(point.x, 148), Math.max(148, container.clientWidth - 148))
  const placement: HoveredVessel['placement'] = point.y < 155 ? 'below' : 'above'
  const y = placement === 'below' ? Math.min(point.y + 22, container.clientHeight - 32) : Math.max(point.y - 18, 84)
  return { x, y, placement }
}

function showHoverForVessel(summary: VesselSummary, offset: number) {
  const point = hoverPointFor(summary, offset)
  if (!point) {
    return
  }
  hoveredVessel.value = {
    summary,
    offset,
    ...point,
  }
}

function nearestVesselTarget(point: L.Point) {
  if (!map || !vesselCanvasLayer || !visibleLayers.vessels) {
    return null
  }
  const hitRadius = Math.max(16, Math.min(26, (map.getZoom() + 4) * 2.5))
  return vesselCanvasLayer.nearestTarget(point, hitRadius)
}

function updateHoverFromPointer() {
  if (!pendingPointerEvent) {
    pointerFrameId = 0
    return
  }
  const event = pendingPointerEvent
  pendingPointerEvent = null
  pointerFrameId = 0

  pointerLabel.value = formatLat(event.latLng.lat)
  mapCenterLabel.value = formatLng(event.latLng.lng)

  const nearest = nearestVesselTarget(event.containerPoint)
  if (!nearest) {
    clearHover()
    return
  }

  showHoverForVessel(nearest.summary, nearest.offset)
}

function handleMapPointerMove(event: L.LeafletMouseEvent) {
  pendingPointerEvent = {
    latLng: event.latlng,
    containerPoint: event.containerPoint,
  }
  if (!pointerFrameId) {
    pointerFrameId = window.requestAnimationFrame(updateHoverFromPointer)
  }
}

function handleMapClick(event: L.LeafletMouseEvent) {
  const nearest = nearestVesselTarget(event.containerPoint)
  if (nearest) {
    void focusVessel(nearest.summary.mmsi)
  }
}

function closeActiveDetailPopup() {
  if (!activeDetailPopup || !map) {
    activeDetailPopup = null
    return
  }
  const popup = activeDetailPopup
  activeDetailPopup = null
  map.closePopup(popup)
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function renderMapLayers() {
  if (!map || !vesselCanvasLayer || !historyLayer || !trackLayer) {
    return
  }
  const history = historyLayer
  const tracks = trackLayer
  history.clearLayers()
  tracks.clearLayers()
  vesselCanvasLayer.setData(vesselSummaries.value, selectedMmsi.value, visibleLayers.vessels)

  for (const summary of vesselSummaries.value) {
    if (summary.records.length < 2 || summary.mmsi !== selectedMmsi.value || !trackVisible.value) {
      continue
    }
    for (const offset of WORLD_COPY_OFFSETS) {
      const path = summary.records.map((item) => [item.latitude, item.longitude + offset] as [number, number])

      if (visibleLayers.tracks && path.length >= 2) {
        L.polyline(path, {
          color: summary.color,
          weight: selectedMmsi.value === summary.mmsi ? 3.4 : 1.8,
          opacity: selectedMmsi.value === summary.mmsi ? 0.92 : 0.46,
          dashArray: selectedMmsi.value === summary.mmsi ? undefined : '6 6',
          lineCap: 'round',
          interactive: false,
        }).addTo(tracks)
      }

      if (visibleLayers.history) {
        summary.records.slice(0, -1).forEach((record) => {
          L.circleMarker([record.latitude, record.longitude + offset], {
            radius: selectedMmsi.value === summary.mmsi ? 4 : 2.8,
            color: summary.color,
            fillColor: summary.color,
            fillOpacity: selectedMmsi.value === summary.mmsi ? 0.58 : 0.32,
            opacity: 0.5,
            weight: 1,
            interactive: false,
          }).addTo(history)
        })
      }
    }
  }
}

function preferredWorldOffset(summary: VesselSummary) {
  const centerLng = map?.getCenter().lng ?? summary.latest.longitude
  return WORLD_COPY_OFFSETS.reduce((best, offset) => {
    const bestDistance = Math.abs(summary.latest.longitude + best - centerLng)
    const distance = Math.abs(summary.latest.longitude + offset - centerLng)
    return distance < bestDistance ? offset : best
  }, 0)
}

function fitDataBounds() {
  if (!map || !filteredRecords.value.length) {
    map?.setView(DEFAULT_CENTER, 1)
    return
  }
  const points = WORLD_COPY_OFFSETS.flatMap((offset) =>
    filteredRecords.value.map((item) => [item.latitude, item.longitude + offset] as [number, number]),
  )
  const bounds = L.latLngBounds(points)
  map.fitBounds(bounds, { padding: [70, 90], maxZoom: 3.5 })
}

function fitVesselTrack(summary: VesselSummary, offset = preferredWorldOffset(summary)) {
  if (!map) {
    return
  }
  const points = summary.records.map((item) => [item.latitude, item.longitude + offset] as [number, number])
  if (points.length < 2) {
    map.setView([summary.latest.latitude, summary.latest.longitude + offset], Math.max(map.getZoom(), 5), { animate: true })
    return
  }
  map.fitBounds(L.latLngBounds(points), {
    paddingTopLeft: [96, 94],
    paddingBottomRight: [430, 150],
    maxZoom: 7,
  })
}

async function focusVessel(mmsi: string) {
  const summary = allVesselSummaries.value.find((item) => item.mmsi === mmsi)
  if (!map || !summary) {
    return
  }
  const offset = preferredWorldOffset(summary)
  selectedMmsi.value = summary.mmsi
  selectedWorldOffset.value = offset
  selectedTrackRecords.value = []
  trackVisible.value = false
  renderMapLayers()
  map.setView([summary.latest.latitude, summary.latest.longitude + offset], Math.max(map.getZoom(), 6), { animate: true })
  window.setTimeout(() => openVesselDetail(summary, offset), 180)
}

async function showSelectedVesselTrack() {
  if (!selectedMmsi.value) {
    ElMessage.warning('请先选中一艘船')
    return
  }
  loading.value = true
  try {
    selectedTrackRecords.value = await loadVesselTrackRecords(selectedMmsi.value)
    trackVisible.value = true
    const summary = allVesselSummaries.value.find((item) => item.mmsi === selectedMmsi.value)
    renderMapLayers()
    if (summary) {
      fitVesselTrack(summary, selectedWorldOffset.value)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AIS 船舶轨迹加载失败')
  } finally {
    loading.value = false
  }
}

function hideSelectedVesselTrack() {
  selectedTrackRecords.value = []
  trackVisible.value = false
  renderMapLayers()
}

async function selectVesselByNumber(mmsi: string) {
  keyword.value = mmsi
  appliedKeyword.value = mmsi
  vesselNumberQuery.value = mmsi
  if (!allVesselSummaries.value.some((item) => item.mmsi === mmsi)) {
    await loadAisMapRecords()
  }
  const summary = allVesselSummaries.value.find((item) => item.mmsi === mmsi)
  if (!summary) {
    ElMessage.warning('没有找到这个船只编号')
    return
  }
  await nextTick()
  await focusVessel(mmsi)
}

function selectVesselNumberQuery() {
  const query = vesselNumberQuery.value.trim()
  if (!query) {
    return
  }
  const exactMatch = allVesselSummaries.value.find((item) => item.mmsi === query)
  const target = exactMatch || vesselNumberOptions.value[0]
  if (!target) {
    void selectVesselByNumber(query)
    return
  }
  void selectVesselByNumber(target.mmsi)
}

function clearVesselSelection() {
  selectedMmsi.value = ''
  selectedWorldOffset.value = 0
  vesselNumberQuery.value = ''
  selectedTrackRecords.value = []
  closeActiveDetailPopup()
  renderMapLayers()
}

function applyKeyword() {
  appliedKeyword.value = keyword.value
  selectedMmsi.value = ''
  selectedWorldOffset.value = 0
  vesselNumberQuery.value = ''
  selectedTrackRecords.value = []
  closeActiveDetailPopup()
  void loadAisMapRecords()
}

function clearKeyword() {
  keyword.value = ''
  appliedKeyword.value = ''
  selectedMmsi.value = ''
  selectedWorldOffset.value = 0
  vesselNumberQuery.value = ''
  selectedTrackRecords.value = []
  closeActiveDetailPopup()
  void loadAisMapRecords()
}

function applyDatasetDate() {
  selectedMmsi.value = ''
  selectedWorldOffset.value = 0
  vesselNumberQuery.value = ''
  selectedTrackRecords.value = []
  trackVisible.value = false
  closeActiveDetailPopup()
  void loadAisMapRecords()
}

function clearDatasetDate() {
  selectedDatasetDate.value = ''
  applyDatasetDate()
}

function zoomBy(delta: number) {
  if (!map) {
    return
  }
  map.setZoom(map.getZoom() + delta)
}

async function reload() {
  selectedTrackRecords.value = []
  await loadAisMapRecords()
}

function formatLat(value: number) {
  const abs = Math.abs(value)
  return `${abs.toFixed(2)}° ${value >= 0 ? 'N' : 'S'}`
}

function formatLng(value: number) {
  const abs = Math.abs(value)
  return `${abs.toFixed(2)}° ${value >= 0 ? 'E' : 'W'}`
}

watch(
  () => [visibleLayers.vessels, visibleLayers.tracks, visibleLayers.history, vesselSummaries.value.length, selectedMmsi.value],
  () => {
    renderMapLayers()
  },
)

onMounted(async () => {
  await nextTick()
  initMap()
  await loadDatasetDates()
  await loadAisMapRecords()
})

onBeforeUnmount(() => {
  closeActiveDetailPopup()
  if (pointerFrameId) {
    window.cancelAnimationFrame(pointerFrameId)
    pointerFrameId = 0
  }
  map?.remove()
  map = null
  baseLayer = null
  labelLayer = null
  vesselCanvasLayer = null
  historyLayer = null
  trackLayer = null
  pendingPointerEvent = null
})
</script>

<style scoped>
.marine-traffic-page {
  position: fixed;
  inset: 0;
  z-index: 10000;
  overflow: hidden;
  background: #eef2f4;
  color: #26333d;
  font-family:
    Inter,
    "Segoe UI",
    "Microsoft YaHei",
    sans-serif;
}

:global(#app > .marine-traffic-page) {
  position: fixed;
  inset: 0;
  z-index: 10000;
  width: 100vw;
  height: 100vh;
}

.traffic-map {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  background: #edf2f5;
}

.traffic-map :deep(.leaflet-tile-pane) {
  filter: saturate(0.72) contrast(0.96) brightness(1.05);
}

.traffic-map :deep(.leaflet-control-attribution) {
  color: #65727c;
  background: rgba(244, 247, 249, 0.72);
  backdrop-filter: blur(8px);
}

:deep(.ais-vessel-canvas) {
  pointer-events: none;
}

.map-hover-card {
  position: absolute;
  z-index: 660;
  min-width: 226px;
  max-width: min(286px, calc(100vw - 24px));
  padding: 9px 12px 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  color: #26333d;
  pointer-events: none;
  box-shadow: 0 12px 30px rgba(31, 43, 52, 0.22);
  transform: translate(-50%, -100%);
}

.map-hover-card::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 100%;
  border-top: 7px solid rgba(255, 255, 255, 0.96);
  border-right: 7px solid transparent;
  border-left: 7px solid transparent;
  transform: translateX(-50%);
}

.map-hover-card.is-below {
  transform: translate(-50%, 0);
}

.map-hover-card.is-below::after {
  top: auto;
  bottom: 100%;
  border-top: 0;
  border-bottom: 7px solid rgba(255, 255, 255, 0.96);
}

.map-hover-card strong {
  display: block;
  max-width: 250px;
  overflow: hidden;
  color: #1f2d37;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-hover-card span {
  display: block;
  margin-top: 3px;
  color: #64717c;
  font-size: 11px;
  letter-spacing: 0;
}

.map-hover-card dl {
  display: grid;
  gap: 3px;
  margin: 8px 0 0;
}

.map-hover-card dl div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 11px;
}

.map-hover-card dt {
  color: #79858e;
  letter-spacing: 0;
}

.map-hover-card dd {
  margin: 0;
  color: #26333d;
  font-weight: 700;
}

:deep(.world-copy-label) {
  background: transparent;
  border: 0;
}

:deep(.world-copy-label span) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112px;
  height: 28px;
  border: 1px solid rgba(93, 112, 124, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  color: #40505c;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
  box-shadow: 0 8px 18px rgba(31, 43, 52, 0.1);
  backdrop-filter: blur(8px);
}

.left-rail {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 600;
  width: 58px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 7px;
  background: #172231;
  box-shadow: 4px 0 14px rgba(26, 42, 55, 0.18);
}

.rail-button,
.right-toolbar button,
.zoom-stack button,
.search-bar button,
.vessel-panel button,
.vessel-card__close {
  border: 0;
  cursor: pointer;
  font: inherit;
}

.rail-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #eef4f8;
  background: transparent;
  transition:
    background-color 0.16s ease,
    transform 0.16s ease;
}

.rail-button :deep(.el-icon) {
  font-size: 23px;
}

.rail-button:hover,
.rail-button.is-active {
  background: rgba(255, 255, 255, 0.18);
}

.rail-button--home {
  margin-bottom: 22px;
  color: #ffffff;
}

.search-bar {
  position: absolute;
  z-index: 610;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  width: min(420px, calc(100vw - 190px));
  height: 42px;
  gap: 10px;
  padding: 0 15px;
  border-radius: 999px;
  background: rgba(54, 65, 74, 0.9);
  color: #ffffff;
  box-shadow: 0 10px 26px rgba(30, 42, 52, 0.2);
  backdrop-filter: blur(10px);
}

.search-bar input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #ffffff;
  font-size: 15px;
}

.search-bar input::placeholder {
  color: rgba(255, 255, 255, 0.58);
  font-weight: 600;
}

.search-bar button {
  color: #dfe8ed;
  background: transparent;
  font-size: 12px;
}

.top-status {
  position: absolute;
  z-index: 610;
  top: 14px;
  right: 76px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
  color: #4f5d67;
  box-shadow: 0 10px 24px rgba(31, 43, 52, 0.12);
}

.top-status span {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.top-status strong {
  color: #26333d;
  font-size: 13px;
}

.vessel-panel {
  position: absolute;
  z-index: 610;
  top: 70px;
  right: 76px;
  width: 360px;
  max-height: calc(100vh - 150px);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 46px rgba(31, 43, 52, 0.18);
  backdrop-filter: blur(14px);
  overflow: hidden;
}

.vessel-panel.is-collapsed {
  width: 190px;
}

.vessel-panel header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(45, 60, 72, 0.12);
}

.vessel-panel header span {
  display: block;
  margin-bottom: 3px;
  color: #74808a;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.vessel-panel header strong {
  color: #26333d;
  font-size: 18px;
}

.vessel-panel header small {
  display: block;
  margin-top: 3px;
  color: #7a8790;
  font-size: 11px;
  font-weight: 700;
}

.vessel-panel header button {
  padding: 7px 10px;
  border-radius: 7px;
  background: #edf2f5;
  color: #47535c;
  font-size: 12px;
}

.layer-toggles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 14px;
}

.layer-toggles label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  border-radius: 7px;
  background: #eef3f6;
  color: #35424c;
  font-size: 12px;
  font-weight: 700;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 0 14px 12px;
}

.dataset-date-filter {
  display: grid;
  gap: 8px;
  padding: 0 14px 12px;
}

.dataset-date-filter label {
  color: #687783;
  font-size: 11px;
  font-weight: 800;
}

.dataset-date-filter__control {
  display: grid;
  grid-template-columns: 1fr auto;
  overflow: hidden;
  border: 1px solid rgba(53, 66, 76, 0.14);
  border-radius: 8px;
  background: #f6f9fb;
}

.dataset-date-filter__control select {
  min-width: 0;
  height: 38px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #26333d;
  padding: 0 10px;
  font: inherit;
  font-size: 13px;
}

.dataset-date-filter__control button {
  padding: 0 12px;
  background: #26333d;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.dataset-date-filter__control button:disabled,
.dataset-date-filter__control select:disabled {
  opacity: 0.48;
  cursor: wait;
}

.metric-grid div {
  padding: 10px;
  border-radius: 7px;
  background: #f5f8fa;
}

.metric-grid span {
  display: block;
  color: #7b8790;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
}

.metric-grid strong {
  display: block;
  margin-top: 4px;
  color: #1f2d37;
  font-size: 17px;
}

.legend {
  display: grid;
  gap: 7px;
  padding: 0 14px 12px;
  color: #4f5d67;
  font-size: 12px;
}

.legend div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-note {
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(18, 184, 199, 0.1);
  color: #4c6b76;
  line-height: 1.45;
}

.legend span {
  width: 18px;
  height: 9px;
  border-radius: 999px;
}

.vessel-number-picker {
  display: grid;
  gap: 9px;
  padding: 0 14px 12px;
}

.vessel-number-picker label {
  color: #687783;
  font-size: 11px;
  font-weight: 800;
}

.vessel-number-picker__control {
  display: grid;
  grid-template-columns: 1fr auto;
  overflow: hidden;
  border: 1px solid rgba(53, 66, 76, 0.14);
  border-radius: 8px;
  background: #f6f9fb;
}

.vessel-number-picker__control input {
  min-width: 0;
  height: 38px;
  border: 0;
  outline: 0;
  background: transparent;
  color: #26333d;
  padding: 0 10px;
  font: inherit;
  font-size: 13px;
}

.vessel-number-picker__control input::placeholder {
  color: #8a969f;
}

.vessel-number-picker__control button,
.selected-vessel-strip button {
  padding: 0 12px;
  background: #26333d;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.vessel-number-picker__control button:disabled {
  opacity: 0.45;
  cursor: default;
}

.vessel-number-picker__options {
  display: grid;
  max-height: 188px;
  overflow: auto;
  border: 1px solid rgba(53, 66, 76, 0.1);
  border-radius: 8px;
  background: #ffffff;
}

.vessel-number-picker__options button {
  display: grid;
  gap: 2px;
  padding: 9px 10px;
  border-bottom: 1px solid rgba(53, 66, 76, 0.08);
  background: transparent;
  color: #35424c;
  text-align: left;
}

.vessel-number-picker__options button:last-child {
  border-bottom: 0;
}

.vessel-number-picker__options button:hover,
.vessel-number-picker__options button.is-selected {
  background: #eaf2f6;
}

.vessel-number-picker__options strong {
  color: #1f2d37;
  font-size: 13px;
}

.vessel-number-picker__options span,
.selected-vessel-strip small {
  overflow: hidden;
  color: #6f7c85;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vessel-number-picker__empty {
  padding: 10px;
  border-radius: 8px;
  background: #f6f1ed;
  color: #9a6746;
  font-size: 12px;
  text-align: center;
}

.selected-vessel-strip {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #e5eef4;
  box-shadow: inset 3px 0 0 #2fb65d;
}

.selected-vessel-strip div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.selected-vessel-strip span {
  color: #587381;
  font-size: 10px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.selected-vessel-strip strong {
  overflow: hidden;
  color: #1f2d37;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-vessel-strip button {
  height: 30px;
  border-radius: 7px;
  background: #ffffff;
  color: #42515c;
  box-shadow: 0 4px 12px rgba(31, 43, 52, 0.1);
}

.vessel-list {
  max-height: min(330px, calc(100vh - 660px));
  overflow: auto;
  padding: 0 10px 12px;
}

.vessel-list button {
  display: grid;
  width: 100%;
  gap: 4px;
  margin-bottom: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f4f7f9;
  color: #35424c;
  text-align: left;
  transition:
    background-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.vessel-list button:hover,
.vessel-list button.is-selected {
  background: #e5eef4;
  box-shadow: inset 3px 0 0 #2fb65d;
  transform: translateX(-2px);
}

.vessel-list__type {
  color: #567;
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.vessel-list strong {
  color: #1f2d37;
  font-size: 15px;
}

.vessel-list small,
.vessel-list button > span:last-child {
  color: #6f7c85;
  font-size: 12px;
}

.empty-panel {
  padding: 24px 12px;
  color: #70808c;
  text-align: center;
}

:global(.traffic-detail-shell) {
  opacity: 1 !important;
}

:global(.traffic-detail-shell .leaflet-popup-content-wrapper) {
  overflow: hidden;
  padding: 0 !important;
  border: 1px solid rgba(45, 60, 72, 0.14) !important;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98) !important;
  color: #26333d !important;
  box-shadow: 0 22px 56px rgba(31, 43, 52, 0.28) !important;
}

:global(.traffic-detail-shell .leaflet-popup-content) {
  width: 430px !important;
  margin: 0 !important;
  color: #26333d !important;
}

:global(.traffic-detail-shell .leaflet-popup-tip) {
  background: rgba(255, 255, 255, 0.98) !important;
}

:global(.traffic-detail-shell .leaflet-popup-close-button) {
  top: 10px !important;
  right: 10px !important;
  z-index: 2;
  width: 28px !important;
  height: 28px !important;
  border-radius: 8px;
  background: #eef3f7 !important;
  color: #7b86a1 !important;
  font-size: 28px !important;
  line-height: 25px !important;
}

:deep(.traffic-detail-card) {
  color: #26333d;
  font-family:
    Inter,
    "Segoe UI",
    "Microsoft YaHei",
    sans-serif;
}

:deep(.traffic-detail-card__header) {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 46px 10px 12px;
  border-bottom: 1px solid rgba(45, 60, 72, 0.14);
}

:deep(.traffic-detail-card__flag) {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 6px;
  background: #2463c8;
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

:deep(.traffic-detail-card__title) {
  min-width: 0;
}

:deep(.traffic-detail-card__title strong) {
  display: block;
  max-width: 330px;
  overflow: hidden;
  color: #1f2d37;
  font-size: 22px;
  line-height: 1.05;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.traffic-detail-card__title span) {
  display: block;
  margin-top: 2px;
  color: #68747d;
  font-size: 13px;
}

:deep(.traffic-detail-card__hero) {
  display: grid;
  grid-template-columns: 1.3fr 0.8fr 1fr;
  border-bottom: 1px solid rgba(45, 60, 72, 0.14);
}

:deep(.traffic-detail-card__hero div),
:deep(.traffic-detail-card__grid div) {
  min-width: 0;
  padding: 12px;
  border-right: 1px solid rgba(45, 60, 72, 0.14);
}

:deep(.traffic-detail-card__hero div:last-child),
:deep(.traffic-detail-card__grid div:last-child) {
  border-right: 0;
}

:deep(.traffic-detail-card small) {
  display: block;
  color: #7a858e;
  font-size: 12px;
  font-weight: 800;
}

:deep(.traffic-detail-card__hero strong),
:deep(.traffic-detail-card__grid strong) {
  display: block;
  margin-top: 5px;
  overflow-wrap: anywhere;
  color: #26333d;
  font-size: 14px;
  line-height: 1.25;
}

:deep(.traffic-detail-card__route) {
  padding: 16px 22px 14px;
  border-bottom: 1px solid rgba(45, 60, 72, 0.14);
}

:deep(.traffic-detail-card__route-row) {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px;
  color: #26333d;
}

:deep(.traffic-detail-card__route-row span) {
  color: #697783;
  font-size: 16px;
  font-weight: 800;
}

:deep(.traffic-detail-card__route-row strong:last-child) {
  text-align: right;
}

:deep(.traffic-detail-card__track) {
  position: relative;
  height: 24px;
  margin-top: 14px;
}

:deep(.traffic-detail-card__track::before) {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  top: 10px;
  height: 3px;
  border-radius: 999px;
  background: #a6a9ad;
}

:deep(.traffic-detail-card__track span) {
  position: absolute;
  left: 0;
  top: 10px;
  height: 3px;
  border-radius: 999px;
  background: #2e9cf0;
}

:deep(.traffic-detail-card__track i) {
  position: absolute;
  left: 55%;
  top: 0;
  width: 0;
  height: 0;
  border-top: 12px solid transparent;
  border-bottom: 12px solid transparent;
  border-left: 30px solid #2e9cf0;
}

:deep(.traffic-detail-card__grid) {
  display: grid;
  grid-template-columns: 1.25fr 1fr 0.7fr 0.8fr;
  border-bottom: 1px solid rgba(45, 60, 72, 0.14);
}

:deep(.traffic-detail-card__footer) {
  display: grid;
  gap: 5px;
  padding: 12px;
  color: #6b767f;
  font-size: 13px;
}

:deep(.traffic-detail-card__footer strong) {
  color: #4f5962;
}

.right-toolbar {
  position: absolute;
  z-index: 610;
  right: 12px;
  top: 12px;
  display: grid;
  gap: 7px;
}

.right-toolbar button,
.zoom-stack button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 7px;
  background: rgba(43, 54, 63, 0.88);
  color: #ffffff;
  box-shadow: 0 8px 22px rgba(31, 43, 52, 0.18);
}

.right-toolbar button:hover,
.right-toolbar button.is-active {
  background: rgba(27, 37, 45, 0.96);
}

.right-toolbar button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.zoom-stack {
  position: absolute;
  z-index: 610;
  right: 12px;
  bottom: 52px;
  display: grid;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(31, 43, 52, 0.18);
}

.zoom-stack button,
.zoom-stack span {
  border-radius: 0;
}

.zoom-stack span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  background: rgba(58, 70, 79, 0.88);
  color: #ffffff;
  font-weight: 800;
}

.coordinate-readout {
  position: absolute;
  z-index: 610;
  right: 66px;
  bottom: 18px;
  display: grid;
  gap: 4px;
  min-width: 168px;
  padding: 11px 13px;
  border-radius: 8px;
  background: rgba(54, 65, 74, 0.88);
  color: #ffffff;
  text-align: right;
  box-shadow: 0 8px 22px rgba(31, 43, 52, 0.18);
}

.coordinate-readout strong,
.coordinate-readout span {
  font-size: 13px;
}

.loading-mask {
  position: absolute;
  z-index: 700;
  left: 50%;
  bottom: 82px;
  transform: translateX(-50%);
  padding: 10px 15px;
  border-radius: 999px;
  background: rgba(43, 54, 63, 0.9);
  color: #ffffff;
  box-shadow: 0 8px 22px rgba(31, 43, 52, 0.18);
}

:deep(.traffic-popup-shell .leaflet-popup-content-wrapper) {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 26px rgba(31, 43, 52, 0.18);
}

:deep(.traffic-popup-shell .leaflet-popup-tip) {
  background: rgba(255, 255, 255, 0.94);
}

:deep(.traffic-popup) {
  min-width: 210px;
  color: #26333d;
}

:deep(.traffic-popup strong) {
  display: block;
  font-size: 16px;
}

:deep(.traffic-popup span),
:deep(.traffic-popup p),
:deep(.traffic-popup em),
:deep(.traffic-popup small) {
  display: block;
  margin-top: 5px;
  color: #60717d;
  font-style: normal;
}

:deep(.traffic-popup div) {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

@media (max-width: 860px) {
  .search-bar {
    left: 72px;
    right: 64px;
    width: auto;
    transform: none;
  }

  .top-status,
  .vessel-panel {
    display: none;
  }

  .vessel-card {
    left: 72px;
    width: calc(100vw - 142px);
  }

  .vessel-card__grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
