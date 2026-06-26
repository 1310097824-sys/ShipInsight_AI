<template>
  <div ref="pageShellRef" class="page-shell">
    <section class="page-hero reveal-section reveal-section--hero">
      <div>
        <h2>航线地图</h2>
        <p>在地图上查看 AIS 最新点位、按关键词和数据集日期筛选，并联动查看单船历史轨迹与详情信息。</p>
      </div>
      <el-space wrap>
        <el-tag type="success" size="large">{{ pointCount }} 个最新点位</el-tag>
        <el-tag size="large">{{ trackTagLabel }}</el-tag>
      </el-space>
    </section>

    <section class="map-story-band reveal-section reveal-section--story">
      <article class="map-story-band__feature">
        <div class="map-story-band__eyebrow">AIS Route Snapshot</div>
        <h3>{{ selectedRecordName }}</h3>
        <p>
          当前地图展示 {{ pointCount }} 条船舶最新 AIS 快照，{{ query.datasetDate ? `聚焦 ${query.datasetDate} 这一天的数据集，` : '默认显示最新数据集，' }}
          你可以按船名、MMSI、IMO 或呼号筛选，再点击点位查看详情并加载单船轨迹。
        </p>
      </article>

      <div class="map-story-band__stats">
        <article class="map-story-band__stat">
          <span>最新 AIS</span>
          <strong>{{ latestObservedAt }}</strong>
          <p>帮助快速回到最近一次船位更新，优先关注最新交通态势。</p>
        </article>
        <article class="map-story-band__stat">
          <span>当前焦点</span>
          <strong>{{ focusLabel }}</strong>
          <p>点击右侧列表或地图点位即可切换焦点船舶，并同步查看详细 AIS 信息。</p>
        </article>
        <article class="map-story-band__stat">
          <span>轨迹点数</span>
          <strong>{{ selectedTrackRecords.length }}</strong>
          <p>{{ selectedTrackVisible ? `正在显示 MMSI ${trackMmsi} 的历史轨迹。` : '选中一艘船后可加载它的历史 AIS 轨迹。' }}</p>
        </article>
      </div>
    </section>

    <div class="map-grid reveal-section reveal-section--grid">
      <el-card class="panel-card" shadow="never">
        <div class="toolbar map-toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="船名 / MMSI / IMO / 呼号 / 备注"
            clearable
            class="map-toolbar__keyword"
            @keyup.enter="refreshMapData"
          />
          <el-date-picker
            v-model="query.datasetDate"
            type="date"
            placeholder="选择航线日期"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            class="map-toolbar__date"
            :disabled-date="isDatasetDateDisabled"
          />
          <el-button type="primary" :loading="loading" @click="applySelectedDate">查看所选日期</el-button>
          <el-button plain :disabled="!query.datasetDate" @click="clearSelectedDate">回到最新</el-button>
          <el-button plain @click="refreshMapData">刷新航线地图</el-button>
          <el-button plain @click="resetFilter">重置</el-button>
          <div class="spacer" />
          <RouterLink to="/observations">
            <el-button plain>进入 AIS 记录</el-button>
          </RouterLink>
        </div>

        <div ref="mapRef" class="eco-map" />
      </el-card>

      <div class="map-side">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="side-header">
              <strong>AIS 点位列表</strong>
              <span>{{ pointCount }} 条</span>
            </div>
          </template>

          <el-scrollbar height="320">
            <div v-if="records.length" class="record-list">
              <button
                v-for="item in records"
                :key="item.id"
                type="button"
                class="record-item"
                :class="{ 'is-active': selectedDetail?.id === item.id }"
                @click="focusRecord(item.id, true)"
              >
                <div class="record-item__topline">
                  <span class="record-item__badge">MMSI {{ item.mmsi }}</span>
                  <span class="record-item__time">{{ displayTime(item.baseDateTime) }}</span>
                </div>
                <strong>{{ detailName(item) }}</strong>
                <span>{{ item.imo || item.callSign || '未记录 IMO / 呼号' }}</span>
                <div class="record-item__footer">
                  <span>坐标</span>
                  <span>{{ formatCoordinate(item.latitude) }}, {{ formatCoordinate(item.longitude) }}</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前筛选条件下没有 AIS 点位" />
          </el-scrollbar>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="side-header">
              <strong>点位详情</strong>
              <span v-if="trackLoading">轨迹加载中...</span>
            </div>
          </template>

          <template v-if="selectedDetail">
            <div class="detail-hero">
              <div>
                <div class="detail-hero__eyebrow">AIS Detail</div>
                <h3>{{ detailName(selectedDetail) }}</h3>
                <p>MMSI {{ selectedDetail.mmsi }} · {{ displayTime(selectedDetail.baseDateTime) }}</p>
              </div>
              <div class="detail-hero__tags">
                <el-tag effect="plain">{{ selectedDetail.imo || '无 IMO' }}</el-tag>
                <el-tag effect="plain">{{ selectedDetail.callSign || '无呼号' }}</el-tag>
              </div>
            </div>

            <div class="detail-stat-grid">
              <div class="detail-stat-card">
                <span>航速</span>
                <strong>{{ formatMetric(selectedDetail.sog, 'kn') }}</strong>
              </div>
              <div class="detail-stat-card">
                <span>航向 / 船首向</span>
                <strong>{{ courseHeadingLabel(selectedDetail) }}</strong>
              </div>
            </div>

            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="接收时间">{{ displayTime(selectedDetail.baseDateTime) }}</el-descriptions-item>
              <el-descriptions-item label="船名">{{ selectedDetail.vesselName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="MMSI">{{ selectedDetail.mmsi }}</el-descriptions-item>
              <el-descriptions-item label="IMO">{{ selectedDetail.imo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="呼号">{{ selectedDetail.callSign || '-' }}</el-descriptions-item>
              <el-descriptions-item label="坐标">{{ positionLabel(selectedDetail) }}</el-descriptions-item>
              <el-descriptions-item label="航行状态">{{ navigationStatusLabel(selectedDetail.status) }}</el-descriptions-item>
              <el-descriptions-item label="尺寸 / 吃水">{{ dimensionDraftLabel(selectedDetail) }}</el-descriptions-item>
              <el-descriptions-item label="录入人员">{{ selectedDetail.importedByName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="来源文件">{{ selectedDetail.sourceFile || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ selectedDetail.note || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div class="detail-actions">
              <el-button type="primary" plain :loading="trackLoading" @click="toggleSelectedTrack">
                {{ selectedTrackVisible ? '隐藏轨迹' : '显示轨迹' }}
              </el-button>
              <el-button plain @click="openAisRecordPage">进入 AIS 记录</el-button>
            </div>

            <div v-if="selectedTrackVisible" class="detail-track-summary">
              <strong>轨迹概览</strong>
              <span>{{ selectedTrackRecords.length }} 个轨迹点 · {{ trackTimeRange }}</span>
            </div>
          </template>
          <el-empty v-else description="点击地图点位或右侧列表查看详情" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import 'leaflet.markercluster'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, useRouter } from 'vue-router'
import { fetchAisDatasetDates, fetchAisMapRecords, fetchAisVesselTrack } from '@/api/aisRecords'
import { ZHANJIANG_OFFSHORE_CENTER } from '@/constants/ecosystem'
import { listenDataChanged } from '@/utils/dataSync'
import { addPreferredTileLayer, toMapDisplayPoint } from '@/utils/mapProvider'
import { buildMapPopupCard, createMapMarkerIcon } from '@/utils/mapMarkerTheme'
import type { AisRecordView } from '@/types/gsmv'

const MAP_LIMIT = 50000
const TRACK_LIMIT = 5000

const router = useRouter()
const pageShellRef = ref<HTMLDivElement>()
const mapRef = ref<HTMLDivElement>()
const loading = ref(false)
const trackLoading = ref(false)
const datasetDates = ref<string[]>([])
const records = ref<AisRecordView[]>([])
const selectedDetail = ref<AisRecordView | null>(null)
const selectedTrackRecords = ref<AisRecordView[]>([])
const trackMmsi = ref('')

const query = reactive({
  keyword: '',
  datasetDate: '',
})

const pointCount = computed(() => records.value.length)
const selectedRecordName = computed(() => {
  if (selectedDetail.value) {
    return detailName(selectedDetail.value)
  }
  if (query.datasetDate) {
    return `${query.datasetDate} 航线快照`
  }
  return 'AIS 航线快照'
})
const latestObservedAt = computed(() => records.value[0] ? displayTime(records.value[0].baseDateTime) : '等待 AIS 数据')
const focusLabel = computed(() => (selectedDetail.value ? detailName(selectedDetail.value) : '全部船舶'))
const selectedTrackVisible = computed(
  () => !!selectedDetail.value && !!trackMmsi.value && trackMmsi.value === selectedDetail.value.mmsi && selectedTrackRecords.value.length > 0,
)
const trackTagLabel = computed(() => (selectedTrackVisible.value ? `${selectedTrackRecords.value.length} 个轨迹点` : query.datasetDate || '最新数据集'))
const datasetDateSet = computed(() => new Set(datasetDates.value))
const trackTimeRange = computed(() => {
  if (!selectedTrackRecords.value.length) {
    return '未加载轨迹'
  }
  const first = selectedTrackRecords.value[0]
  const last = selectedTrackRecords.value[selectedTrackRecords.value.length - 1]
  return `${displayTime(first.baseDateTime)} 至 ${displayTime(last.baseDateTime)}`
})

let map: L.Map | null = null
let markerLayer: L.MarkerClusterGroup | null = null
let trackLayer: L.LayerGroup | null = null
let stopDataSyncListener: (() => void) | null = null
let resizeObserver: ResizeObserver | null = null
let revealObserver: IntersectionObserver | null = null
let invalidateTimer: number | null = null

const markerMap = new Map<string, L.Marker>()
const markerDataMap = new Map<string, AisRecordView>()
const defaultCenter = ZHANJIANG_OFFSHORE_CENTER

function scheduleInvalidateMap(delay = 80) {
  if (!map) {
    return
  }
  if (invalidateTimer != null) {
    window.clearTimeout(invalidateTimer)
  }
  invalidateTimer = window.setTimeout(() => {
    map?.invalidateSize(false)
    invalidateTimer = null
  }, delay)
}

function createClusterIcon(cluster: L.MarkerCluster) {
  const count = cluster.getChildCount()
  const size = count >= 50 ? 54 : count >= 20 ? 48 : 42
  return L.divIcon({
    html: `<span>${count}</span>`,
    className: 'gsmv-marker-cluster',
    iconSize: L.point(size, size),
  })
}

function getMarkerTone(record: AisRecordView) {
  const type = record.vesselType ?? 0
  const speed = record.sog ?? 0
  if (type >= 80) return 'emerald'
  if (speed >= 8) return 'violet'
  return 'aqua'
}

function displayTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.includes('T') ? value.replace('T', ' ') : value
}

function timestamp(value?: string) {
  if (!value) {
    return 0
  }
  return new Date(value.includes('T') ? value : value.replace(' ', 'T')).getTime()
}

function formatCoordinate(value?: number | null) {
  if (value == null) {
    return '-'
  }
  return Number(value).toFixed(5)
}

function formatMetric(value: number | string | null | undefined, unit: string) {
  if (value == null || value === '') {
    return '-'
  }
  return `${value}${unit}`
}

function detailName(record: AisRecordView) {
  return record.vesselName || `MMSI ${record.mmsi}`
}

function positionLabel(record: Pick<AisRecordView, 'latitude' | 'longitude'>) {
  return `${formatCoordinate(record.latitude)}, ${formatCoordinate(record.longitude)}`
}

function courseHeadingLabel(record: Pick<AisRecordView, 'cog' | 'heading'>) {
  const course = formatMetric(record.cog, '°')
  const heading = formatMetric(record.heading, '°')
  return `${course} / ${heading}`
}

function navigationStatusLabel(status?: number | null) {
  const labels: Record<number, string> = {
    0: '机动航行',
    1: '锚泊',
    2: '失去控制',
    3: '操纵受限',
    4: '吃水受限',
    5: '系泊',
    6: '搁浅',
    7: '捕鱼作业',
    8: '帆航',
    15: '未定义',
  }
  return typeof status === 'number' ? labels[status] || `状态 ${status}` : '未上报'
}

function dimensionDraftLabel(record: Pick<AisRecordView, 'length' | 'width' | 'draft'>) {
  const size = [record.length ? `${record.length}m` : '-', record.width ? `${record.width}m` : '-'].join(' / ')
  const draft = typeof record.draft === 'number' ? ` / 吃水 ${record.draft}m` : ''
  return `${size}${draft}`
}

function updateMarkerVisualState(activeId?: string | null) {
  markerMap.forEach((marker, id) => {
    const item = markerDataMap.get(id)
    if (!item) {
      return
    }
    marker.setIcon(
      createMapMarkerIcon(detailName(item), {
        active: id === activeId,
        tone: getMarkerTone(item),
      }),
    )
    marker.setZIndexOffset(id === activeId ? 1200 : 0)
  })
}

function ensureMap() {
  if (map || !mapRef.value) {
    return
  }

  map = L.map(mapRef.value, { zoomControl: true }).setView(toMapDisplayPoint(defaultCenter[0], defaultCenter[1]), 7)
  addPreferredTileLayer(map)
  markerLayer = L.markerClusterGroup({
    showCoverageOnHover: false,
    spiderfyOnMaxZoom: true,
    zoomToBoundsOnClick: true,
    disableClusteringAtZoom: 9,
    maxClusterRadius: 42,
    iconCreateFunction: createClusterIcon,
  }).addTo(map)
  trackLayer = L.layerGroup().addTo(map)
  scheduleInvalidateMap(120)

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      scheduleInvalidateMap(60)
    })
    resizeObserver.observe(mapRef.value)
  }
}

function popupHtml(item: AisRecordView) {
  return buildMapPopupCard({
    eyebrow: 'AIS',
    title: detailName(item),
    subtitle: `MMSI ${item.mmsi}`,
    meta: displayTime(item.baseDateTime),
    chips: [
      { label: 'Speed', value: formatMetric(item.sog, 'kn') },
      { label: 'Course', value: formatMetric(item.cog, '°') },
    ],
    lines: [
      `Coordinates ${positionLabel(item)}`,
      item.imo ? `IMO ${item.imo}` : '',
      item.callSign ? `CallSign ${item.callSign}` : '',
      item.sourceFile ? `Source ${item.sourceFile}` : '',
    ].filter(Boolean),
  })
}

function renderMarkers() {
  if (!map || !markerLayer) {
    return
  }

  markerLayer.clearLayers()
  markerMap.clear()
  markerDataMap.clear()

  if (!records.value.length) {
    map.setView(toMapDisplayPoint(defaultCenter[0], defaultCenter[1]), 7)
    scheduleInvalidateMap()
    return
  }

  const bounds: [number, number][] = []

  records.value.forEach((item) => {
    const point = toMapDisplayPoint(item.latitude, item.longitude)
    const marker = L.marker(point, {
      icon: createMapMarkerIcon(detailName(item), {
        active: selectedDetail.value?.id === item.id,
        tone: getMarkerTone(item),
      }),
    })

    marker.bindPopup(popupHtml(item), { className: 'gsmv-map-popup' })
    marker.on('click', () => {
      focusRecord(item.id, false)
    })
    marker.addTo(markerLayer!)
    markerMap.set(item.id, marker)
    markerDataMap.set(item.id, item)
    bounds.push(point)
  })

  if (bounds.length === 1) {
    map.setView(bounds[0], 8, { animate: false })
  } else {
    map.fitBounds(bounds, { padding: [24, 24], maxZoom: 8 })
  }

  updateMarkerVisualState(selectedDetail.value?.id)
  scheduleInvalidateMap()
}

function renderTrack() {
  if (!trackLayer) {
    return
  }
  trackLayer.clearLayers()

  if (!selectedTrackRecords.value.length) {
    return
  }

  const latLngs = selectedTrackRecords.value.map((item) => toMapDisplayPoint(item.latitude, item.longitude))
  const polyline = L.polyline(latLngs, {
    color: '#5de7ff',
    weight: 4,
    opacity: 0.82,
  })
  trackLayer.addLayer(polyline)

  selectedTrackRecords.value.forEach((item, index) => {
    const point = toMapDisplayPoint(item.latitude, item.longitude)
    const isEndpoint = index === 0 || index === selectedTrackRecords.value.length - 1
    const marker = L.circleMarker(point, {
      radius: isEndpoint ? 6 : 4,
      color: isEndpoint ? '#ffffff' : '#91f0ff',
      weight: 2,
      fillColor: isEndpoint ? '#1fd8c1' : '#2f6bff',
      fillOpacity: 0.88,
    })
    marker.bindPopup(popupHtml(item), { className: 'gsmv-map-popup' })
    trackLayer?.addLayer(marker)
  })
}

async function loadMapRecords() {
  loading.value = true
  try {
    const pageData = await fetchAisMapRecords({
      keyword: query.keyword.trim() || undefined,
      datasetDate: query.datasetDate || undefined,
      limit: MAP_LIMIT,
    })
    records.value = [...pageData.items].sort((a, b) => timestamp(b.baseDateTime) - timestamp(a.baseDateTime))
    renderMarkers()
    if (records.value.length) {
      const currentId = selectedDetail.value?.id
      const next = currentId ? records.value.find((item) => item.id === currentId) || records.value[0] : records.value[0]
      selectedDetail.value = next
      updateMarkerVisualState(next.id)
      if (trackMmsi.value && next.mmsi !== trackMmsi.value) {
        clearTrack()
      }
    } else {
      selectedDetail.value = null
      clearTrack()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '航线地图加载失败')
  } finally {
    loading.value = false
  }
}

async function loadDatasetDates() {
  try {
    datasetDates.value = await fetchAisDatasetDates()
    if (query.datasetDate && !datasetDates.value.includes(query.datasetDate)) {
      query.datasetDate = ''
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '数据集日期加载失败')
  }
}

async function refreshMapData() {
  await Promise.all([loadDatasetDates(), loadMapRecords()])
}

function applySelectedDate() {
  clearTrack()
  void refreshMapData()
}

function clearSelectedDate() {
  query.datasetDate = ''
  clearTrack()
  void refreshMapData()
}

function isDatasetDateDisabled(date: Date) {
  if (!datasetDateSet.value.size) {
    return false
  }
  return !datasetDateSet.value.has(formatDateOnly(date))
}

function formatDateOnly(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function focusRecord(id: string, openPopup: boolean) {
  const target = records.value.find((item) => item.id === id)
  if (!target) {
    return
  }
  selectedDetail.value = target
  updateMarkerVisualState(id)

  if (trackMmsi.value && trackMmsi.value !== target.mmsi) {
    clearTrack()
  }

  const marker = markerMap.get(id)
  if (marker && map && markerLayer) {
    markerLayer.zoomToShowLayer(marker, () => {
      map?.setView(marker.getLatLng(), Math.max(map.getZoom(), 8), { animate: false })
      if (openPopup) {
        marker.openPopup()
      }
    })
  }
}

async function toggleSelectedTrack() {
  if (!selectedDetail.value) {
    return
  }

  if (selectedTrackVisible.value) {
    clearTrack()
    return
  }

  trackLoading.value = true
  try {
    const pageData = await fetchAisVesselTrack(selectedDetail.value.mmsi, TRACK_LIMIT)
    selectedTrackRecords.value = [...pageData.items].sort((a, b) => timestamp(a.baseDateTime) - timestamp(b.baseDateTime))
    trackMmsi.value = selectedDetail.value.mmsi
    renderTrack()

    if (map && selectedTrackRecords.value.length > 1) {
      const bounds = L.latLngBounds(selectedTrackRecords.value.map((item) => toMapDisplayPoint(item.latitude, item.longitude)))
      map.fitBounds(bounds, { padding: [28, 28], maxZoom: 10 })
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '单船轨迹加载失败')
  } finally {
    trackLoading.value = false
  }
}

function clearTrack() {
  selectedTrackRecords.value = []
  trackMmsi.value = ''
  trackLayer?.clearLayers()
}

function resetFilter() {
  query.keyword = ''
  query.datasetDate = ''
  clearTrack()
  void refreshMapData()
}

function openAisRecordPage() {
  if (!selectedDetail.value) {
    router.push('/observations')
    return
  }
  router.push({
    path: '/observations',
    query: {
      keyword: selectedDetail.value.mmsi,
    },
  })
}

function setupRevealObserver() {
  if (!pageShellRef.value || typeof IntersectionObserver === 'undefined') {
    pageShellRef.value?.querySelectorAll<HTMLElement>('.reveal-section').forEach((element) => {
      element.classList.add('is-visible')
    })
    return
  }

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          revealObserver?.unobserve(entry.target)
        }
      })
    },
    {
      threshold: 0.18,
      rootMargin: '0px 0px -10% 0px',
    },
  )

  pageShellRef.value.querySelectorAll<HTMLElement>('.reveal-section').forEach((element) => {
    revealObserver?.observe(element)
  })
}

onMounted(async () => {
  try {
    stopDataSyncListener = listenDataChanged(() => {
      void refreshMapData()
    })
    await nextTick()
    setupRevealObserver()
    ensureMap()
    await refreshMapData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '航线地图初始化失败')
  }
})

onBeforeUnmount(() => {
  stopDataSyncListener?.()
  stopDataSyncListener = null
  resizeObserver?.disconnect()
  resizeObserver = null
  revealObserver?.disconnect()
  revealObserver = null
  if (invalidateTimer != null) {
    window.clearTimeout(invalidateTimer)
    invalidateTimer = null
  }
  map?.remove()
  map = null
  markerLayer = null
  trackLayer = null
  markerMap.clear()
  markerDataMap.clear()
})
</script>

<style scoped>
.reveal-section {
  opacity: 0;
  transform: translateY(24px) scale(0.985);
  filter: blur(10px);
  transition:
    opacity 0.72s ease,
    transform 0.72s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.72s ease;
}

.reveal-section.is-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

.reveal-section--hero {
  transition-delay: 0.02s;
}

.reveal-section--story {
  transition-delay: 0.12s;
}

.reveal-section--grid {
  transition-delay: 0.2s;
}

.map-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.85fr);
  gap: 18px;
}

.map-story-band {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  gap: 18px;
  margin-bottom: 18px;
}

.map-story-band__feature,
.map-story-band__stat {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(150, 232, 255, 0.14);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 18px 36px rgba(2, 15, 44, 0.14);
  transition:
    transform 0.28s ease,
    border-color 0.28s ease,
    box-shadow 0.28s ease;
}

.map-story-band__feature {
  padding: 24px 26px;
  border-radius: 28px;
  background:
    radial-gradient(circle at 12% 0%, rgba(90, 232, 255, 0.18), transparent 34%),
    linear-gradient(145deg, rgba(10, 41, 94, 0.94), rgba(5, 21, 58, 0.98));
}

.map-story-band__feature::after,
.map-story-band__stat::after {
  content: "";
  position: absolute;
  inset: auto -20% -38% auto;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(103, 246, 211, 0.12), transparent 70%);
  pointer-events: none;
  animation: map-card-drift 14s ease-in-out infinite;
}

.map-story-band__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(90, 232, 255, 0.1);
  border: 1px solid rgba(156, 241, 255, 0.18);
  color: #7feaff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.map-story-band__feature h3 {
  margin: 14px 0 8px;
  color: #f7fdff;
  font-size: 32px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.map-story-band__feature p {
  max-width: 560px;
  margin: 0;
  color: rgba(223, 246, 255, 0.82);
  line-height: 1.8;
}

.map-story-band__feature:hover,
.map-story-band__stat:hover {
  transform: translateY(-3px);
  border-color: rgba(162, 239, 255, 0.22);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 24px 40px rgba(2, 15, 44, 0.18);
}

.map-story-band__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.map-story-band__stat {
  position: relative;
  padding: 18px 18px 20px;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02)),
    rgba(5, 24, 60, 0.78);
}

.map-story-band__stat span {
  display: block;
  color: rgba(197, 232, 246, 0.72);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.map-story-band__stat strong {
  display: block;
  margin-top: 22px;
  max-width: calc(100% - 72px);
  color: #f4fdff;
  font-size: 26px;
  line-height: 1.08;
  letter-spacing: -0.04em;
  text-wrap: balance;
}

.map-story-band__stat p {
  margin: 10px 0 0;
  color: rgba(214, 241, 252, 0.74);
  font-size: 12px;
  line-height: 1.7;
}

.map-story-band__stat::before {
  content: "";
  position: absolute;
  top: 16px;
  right: 16px;
  width: 38px;
  height: 38px;
  border-radius: 16px;
  border: 1px solid rgba(176, 246, 255, 0.18);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 12px 24px rgba(2, 15, 42, 0.16);
}

.map-story-band__stat::after {
  content: "";
  position: absolute;
  top: 26px;
  right: 26px;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  z-index: 1;
}

.map-story-band__stat:nth-child(1)::before {
  background:
    linear-gradient(180deg, rgba(112, 241, 255, 0.18), rgba(9, 49, 102, 0.82)),
    rgba(8, 26, 58, 0.9);
}

.map-story-band__stat:nth-child(1)::after {
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.96) 0 22%, transparent 24%),
    conic-gradient(from 90deg, rgba(89, 235, 255, 0.98), rgba(20, 125, 219, 0.84));
}

.map-story-band__stat:nth-child(2)::before {
  background:
    linear-gradient(180deg, rgba(118, 245, 226, 0.2), rgba(7, 74, 96, 0.82)),
    rgba(8, 26, 58, 0.9);
}

.map-story-band__stat:nth-child(2)::after {
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.96) 0 18%, transparent 20%),
    radial-gradient(circle at 50% 50%, rgba(118, 245, 226, 0.98) 0 56%, transparent 58%);
  box-shadow: 0 0 0 5px rgba(118, 245, 226, 0.14);
}

.map-story-band__stat:nth-child(3)::before {
  background:
    linear-gradient(180deg, rgba(132, 208, 255, 0.2), rgba(29, 78, 162, 0.82)),
    rgba(8, 26, 58, 0.9);
}

.map-story-band__stat:nth-child(3)::after {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(196, 235, 255, 0.94));
  clip-path: polygon(50% 0%, 100% 38%, 80% 100%, 20% 100%, 0% 38%);
}

@keyframes map-card-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-10px, -8px, 0) scale(1.04);
  }
}

.map-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.map-toolbar__keyword {
  width: min(320px, 100%);
}

.map-toolbar__date {
  width: 220px;
}

.eco-map {
  min-height: 620px;
  border-radius: 22px;
  overflow: hidden;
}

.map-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.side-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid rgba(132, 230, 255, 0.14);
  border-radius: 20px;
  background:
    linear-gradient(160deg, rgba(91, 233, 255, 0.08), rgba(6, 26, 62, 0.78)),
    rgba(7, 28, 64, 0.82);
  color: var(--gsmv-text);
  text-align: left;
  cursor: pointer;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.record-item__topline,
.record-item__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.record-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(156, 241, 255, 0.18);
  background: rgba(90, 232, 255, 0.12);
  color: #8befff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.record-item__time {
  color: rgba(206, 235, 247, 0.76);
  font-size: 12px;
}

.record-item strong {
  color: #f1fcff;
  font-size: 18px;
  line-height: 1.25;
}

.record-item span {
  color: rgba(203, 234, 247, 0.8);
  font-size: 13px;
}

.record-item__footer {
  margin-top: 2px;
  padding-top: 8px;
  border-top: 1px solid rgba(150, 232, 255, 0.08);
}

.detail-hero {
  position: relative;
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid rgba(152, 234, 255, 0.16);
  border-radius: 22px;
  background:
    radial-gradient(circle at 14% 0%, rgba(90, 232, 255, 0.14), transparent 34%),
    linear-gradient(145deg, rgba(10, 41, 94, 0.94), rgba(5, 21, 58, 0.98));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.04),
    0 18px 34px rgba(2, 15, 42, 0.18);
  overflow: hidden;
}

.detail-hero::after {
  content: "";
  position: absolute;
  right: -26px;
  top: -28px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(112, 241, 255, 0.16), transparent 68%);
  pointer-events: none;
}

.detail-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(90, 232, 255, 0.1);
  border: 1px solid rgba(156, 241, 255, 0.18);
  color: #82ecff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.detail-hero h3 {
  margin: 12px 0 6px;
  color: #f6feff;
  font-size: 24px;
  line-height: 1.15;
}

.detail-hero p {
  margin: 0;
  color: rgba(221, 244, 255, 0.78);
  line-height: 1.7;
}

.detail-hero__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-content: flex-start;
  gap: 8px;
}

.detail-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.detail-stat-card {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(150, 232, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03)),
    rgba(5, 24, 60, 0.76);
}

.detail-stat-card span {
  display: block;
  color: rgba(197, 232, 246, 0.72);
  font-size: 12px;
}

.detail-stat-card strong {
  display: block;
  margin-top: 8px;
  color: #f2fdff;
  font-size: 22px;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.detail-track-summary {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(150, 232, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03)),
    rgba(5, 24, 60, 0.76);
}

.detail-track-summary strong {
  color: #f4fdff;
}

.detail-track-summary span {
  color: rgba(210, 236, 247, 0.78);
  font-size: 13px;
}

.record-item:hover,
.record-item.is-active {
  transform: translateY(-2px);
  border-color: rgba(126, 237, 255, 0.28);
  background:
    linear-gradient(145deg, rgba(108, 244, 255, 0.18), rgba(14, 70, 149, 0.34)),
    rgba(11, 36, 82, 0.9);
  box-shadow:
    0 14px 28px rgba(0, 10, 34, 0.2),
    0 0 0 1px rgba(115, 238, 255, 0.08) inset;
}

.record-item.is-active {
  border-color: rgba(155, 244, 255, 0.42);
  box-shadow:
    0 18px 34px rgba(0, 10, 34, 0.24),
    0 0 0 1px rgba(155, 244, 255, 0.14) inset,
    0 0 26px rgba(79, 216, 255, 0.12);
}

.record-item.is-active strong {
  color: #ffffff;
}

.record-item.is-active span {
  color: rgba(227, 249, 255, 0.88);
}

@media (max-width: 1080px) {
  .map-story-band,
  .map-grid {
    grid-template-columns: 1fr;
  }

  .eco-map {
    min-height: 460px;
  }

  .detail-hero,
  .detail-stat-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .map-story-band__stats {
    grid-template-columns: 1fr;
  }

  .map-toolbar__keyword,
  .map-toolbar__date {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reveal-section,
  .reveal-section.is-visible,
  .map-story-band__feature,
  .map-story-band__stat,
  .map-story-band__feature::after,
  .map-story-band__stat::after {
    animation: none !important;
    transition: none !important;
    transform: none !important;
    filter: none !important;
    opacity: 1 !important;
  }
}
</style>
