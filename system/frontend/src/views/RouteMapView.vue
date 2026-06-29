<template>
  <div ref="pageShellRef" class="page-shell">
    <section class="page-hero reveal-section reveal-section--hero">
      <div>
        <h2>航线地图</h2>
        <p>基于 AIS 实时数据的船舶交通态势地图。按 MMSI、船名或数据集日期筛选，查看每艘船的航行轨迹、速度和航向，联动关联船舶档案。</p>
      </div>
      <el-space wrap>
        <el-tag type="success" size="large">{{ recordCount }} 艘船舶</el-tag>
        <el-tag size="large">{{ datasetDateLabel }}</el-tag>
      </el-space>
    </section>

    <section class="map-story-band reveal-section reveal-section--story">
      <article class="map-story-band__feature">
        <div class="map-story-band__eyebrow">AIS Snapshot</div>
        <h3>{{ focusedVesselLabel }}</h3>
        <p>
          当前地图正在追踪 {{ recordCount }} 艘船舶的 AIS 最新位置，聚焦 {{ focusedArea }} 一带的交通态势，
          通过地图、时间和船舶档案形成清晰的海上交通叙事。
        </p>
      </article>

      <div class="map-story-band__stats">
        <article class="map-story-band__stat">
          <span>最新 AIS 时间</span>
          <strong>{{ latestAisTime }}</strong>
          <p>最近的 AIS 数据更新时间。</p>
        </article>
        <article class="map-story-band__stat">
          <span>高速船舶</span>
          <strong>{{ speedingCount }}</strong>
          <p>航速 ≥ 10 节的在航船舶数量。</p>
        </article>
        <article class="map-story-band__stat">
          <span>关联船舶档案</span>
          <strong>{{ linkedVesselCount }}</strong>
          <p>已与船舶档案库关联的 AIS 目标。</p>
        </article>
      </div>
    </section>

    <div class="map-grid reveal-section reveal-section--grid">
      <el-card class="panel-card" shadow="never">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            placeholder="搜索 MMSI / IMO / 船名"
            clearable
            style="width: 220px"
            @keyup.enter="refreshMapData"
          >
            <template #prefix>
              <span class="toolbar-search-icon">🔍</span>
            </template>
          </el-input>
          <el-select v-model="selectedDatasetDate" placeholder="选择数据集日期" clearable style="width: 180px">
            <el-option v-for="item in datasetDates" :key="item" :label="item" :value="item" />
          </el-select>
          <el-button type="primary" :loading="loading" @click="refreshMapData">刷新航线地图</el-button>
          <el-button
            type="primary"
            :loading="trajectoryLoading"
            :disabled="!selectedRecord"
            plain
            @click="showTrajectory"
          >
            显示轨迹
          </el-button>
          <el-button
            v-if="trajectoryVisible"
            type="danger"
            plain
            size="small"
            @click="clearTrajectory"
          >
            清除轨迹
          </el-button>
          <el-button plain @click="resetFilter">重置</el-button>
          <div class="spacer" />
          <RouterLink to="/vessels">
            <el-button plain>进入船舶档案</el-button>
          </RouterLink>
        </div>

        <div ref="mapRef" class="route-map" />
      </el-card>

      <div class="map-side">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="side-header">
              <strong>船舶列表</strong>
              <span>{{ recordCount }} 艘</span>
            </div>
          </template>

          <el-scrollbar height="320">
            <div v-if="records.length" class="ship-list">
              <button
                v-for="item in records"
                :key="item.id"
                type="button"
                class="ship-item"
                :class="{ 'is-active': selectedRecord?.id === item.id }"
                @click="focusRecord(item.id, true)"
              >
                <div class="ship-item__topline">
                  <span class="ship-item__badge">{{ item.mmsi || '-' }}</span>
                  <span class="ship-item__time">{{ formatTime(item.baseDateTime) }}</span>
                </div>
                <strong>{{ item.vesselName || '未命名船舶' }}</strong>
                <div class="ship-item__meta">
                  <span>航速 {{ item.sog != null ? item.sog + ' 节' : '-' }}</span>
                  <span>航向 {{ item.cog != null ? item.cog + '°' : '-' }}</span>
                </div>
              </button>
            </div>
            <el-empty v-else description="当前筛选条件下没有 AIS 数据" />
          </el-scrollbar>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="side-header">
              <strong>船舶详情</strong>
              <span v-if="detailLoading">加载中...</span>
            </div>
          </template>

          <template v-if="selectedRecord">
            <div class="detail-hero">
              <div>
                <div class="detail-hero__eyebrow">AIS Detail</div>
                <h3>{{ selectedRecord.vesselName || '未命名' }}</h3>
                <p>MMSI {{ selectedRecord.mmsi }} · {{ formatTime(selectedRecord.baseDateTime) }}</p>
              </div>
              <div class="detail-hero__tags">
                <el-tag effect="plain">{{ selectedRecord.mmsi }}</el-tag>
                <el-tag v-if="selectedRecord.imo" effect="plain">IMO {{ selectedRecord.imo }}</el-tag>
              </div>
            </div>

            <div class="detail-stat-grid">
              <div class="detail-stat-card">
                <span>航速</span>
                <strong>{{ selectedRecord.sog != null ? selectedRecord.sog + ' 节' : '-' }}</strong>
              </div>
              <div class="detail-stat-card">
                <span>航向</span>
                <strong>{{ selectedRecord.cog != null ? selectedRecord.cog + '°' : '-' }}</strong>
              </div>
              <div class="detail-stat-card">
                <span>艏向</span>
                <strong>{{ selectedRecord.heading != null ? selectedRecord.heading + '°' : '-' }}</strong>
              </div>
              <div class="detail-stat-card">
                <span>船舶类型</span>
                <strong>{{ vesselTypeLabel(selectedRecord.vesselType) }}</strong>
              </div>
            </div>

            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="MMSI">{{ selectedRecord.mmsi || '-' }}</el-descriptions-item>
              <el-descriptions-item label="IMO">{{ selectedRecord.imo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="呼号">{{ selectedRecord.callSign || '-' }}</el-descriptions-item>
              <el-descriptions-item label="经纬度">
                {{ formatCoord(selectedRecord.latitude) }}, {{ formatCoord(selectedRecord.longitude) }}
              </el-descriptions-item>
              <el-descriptions-item label="船长">{{ selectedRecord.length != null ? selectedRecord.length + ' m' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="船宽">{{ selectedRecord.width != null ? selectedRecord.width + ' m' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="吃水">{{ selectedRecord.draft != null ? selectedRecord.draft + ' m' : '-' }}</el-descriptions-item>
              <el-descriptions-item label="数据来源">{{ selectedRecord.sourceFile || '-' }}</el-descriptions-item>
              <el-descriptions-item label="接收机">{{ selectedRecord.transceiver || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ selectedRecord.note || '-' }}</el-descriptions-item>
            </el-descriptions>

            <el-divider v-if="selectedRecord.linkedVessel">关联船舶档案</el-divider>

            <div v-if="selectedRecord.linkedVessel" class="detail-vessel-card">
              <div class="detail-vessel-card__info">
                <span>MMSI {{ selectedRecord.linkedVessel.mmsi || selectedRecord.mmsi }}</span>
                <strong>{{ selectedRecord.linkedVessel.vesselName || selectedRecord.vesselName || '未命名' }}</strong>
                <span v-if="selectedRecord.linkedVessel.vesselTypeName">{{ selectedRecord.linkedVessel.vesselTypeName }}</span>
              </div>
              <el-button
                type="primary"
                size="small"
                @click="navigateToVessel(selectedRecord.linkedVessel!.vesselId)"
              >
                查看档案
              </el-button>
            </div>
          </template>
          <el-empty v-else description="点击地图点位或右侧列表查看船舶详情" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import L from 'leaflet'
import 'leaflet.markercluster'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RouterLink, useRouter } from 'vue-router'
import { fetchAisMapRecords, fetchAisDatasetDates, fetchAisVesselTrack } from '@/api/aisRecords'
import { ZHANJIANG_OFFSHORE_CENTER } from '@/constants/ecosystem'
import { listenDataChanged } from '@/utils/dataSync'
import { addPreferredTileLayer, toMapDisplayPoint } from '@/utils/mapProvider'
import { buildMapPopupCard, createMapMarkerIcon } from '@/utils/mapMarkerTheme'
import type { AisRecordView } from '@/types/gsmv'

const router = useRouter()
const pageShellRef = ref<HTMLDivElement>()
const mapRef = ref<HTMLDivElement>()
const loading = ref(false)
const detailLoading = ref(false)
const trajectoryLoading = ref(false)
const trajectoryVisible = ref(false)
const records = ref<AisRecordView[]>([])
const selectedRecord = ref<AisRecordView | null>(null)
const datasetDates = ref<string[]>([])

const keyword = ref('')
const selectedDatasetDate = ref<string | undefined>(undefined)

const recordCount = computed(() => records.value.length)
const focusedVesselLabel = computed(() => {
  return selectedRecord.value?.vesselName || records.value[0]?.vesselName || '当前海域'
})
const focusedArea = computed(() => {
  if (selectedRecord.value) {
    return `${formatCoord(selectedRecord.value.latitude)}, ${formatCoord(selectedRecord.value.longitude)}`
  }
  return '湛江近海'
})
const latestAisTime = computed(() => {
  if (!records.value.length) return '等待数据'
  const latest = [...records.value]
    .map((item) => item.baseDateTime)
    .sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0]
  return formatTime(latest)
})
const speedingCount = computed(() => records.value.filter((item) => (item.sog ?? 0) >= 10).length)
const linkedVesselCount = computed(() => records.value.filter((item) => item.linkedVessel).length)
const datasetDateLabel = computed(() => {
  if (selectedDatasetDate.value) return `数据集: ${selectedDatasetDate.value}`
  return records.value.length ? `最新数据日` : '暂无 AIS 数据'
})

let map: L.Map | null = null
let markerLayer: L.MarkerClusterGroup | null = null
let trajectoryGroup: L.LayerGroup | null = null
let stopDataSyncListener: (() => void) | null = null
let resizeObserver: ResizeObserver | null = null
let revealObserver: IntersectionObserver | null = null
let invalidateTimer: number | null = null

const markerMap = new Map<string, L.Marker>()
const defaultCenter = ZHANJIANG_OFFSHORE_CENTER

function scheduleInvalidateMap(delay = 80) {
  if (!map) return
  if (invalidateTimer != null) window.clearTimeout(invalidateTimer)
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

function getMarkerTone(id: string) {
  const tones = ['aqua', 'emerald', 'violet'] as const
  const hash = [...id].reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return tones[Math.abs(hash) % tones.length]
}

function getSogColor(sog: number | null | undefined) {
  if (sog == null) return 'aqua'
  if (sog >= 14) return 'violet'
  if (sog >= 8) return 'aqua'
  return 'emerald'
}

function formatTime(value: string) {
  return value.includes('T') ? value.replace('T', ' ') : value
}

function formatCoord(value: unknown) {
  return Number(value).toFixed(4)
}

function vesselTypeLabel(code: number | null | undefined) {
  if (code == null) return '-'
  const map: Record<number, string> = {
    30: '渔船', 31: '拖轮', 32: '拖轮 (>200m)',
    33: '挖泥船', 34: '潜水作业', 35: '军事船', 36: '帆船',
    37: '游艇', 50: '引航船', 51: '搜救船', 52: '拖船',
    53: '港口供应', 54: '防污设备', 55: '执法船',
    60: '客船', 61: '客船', 62: '客船', 63: '客船', 69: '客船',
    70: '货船', 71: '货船', 72: '货船', 73: '货船', 74: '货船', 79: '货船',
    80: '油轮', 81: '油轮', 82: '油轮', 83: '油轮', 84: '油轮', 89: '油轮',
    90: '其他', 91: '其他', 92: '其他', 93: '其他', 94: '其他', 99: '其他',
  }
  return map[code] || `类型 ${code}`
}

function navigateToVessel(vesselId: number) {
  router.push(`/vessels/${vesselId}`)
}

function updateMarkerVisualState(activeId?: string | null) {
  markerMap.forEach((marker, id) => {
    const item = records.value.find((r) => r.id === id)
    marker.setIcon(
      createMapMarkerIcon(item?.vesselName || item?.mmsi || 'S', {
        active: id === activeId,
        tone: id === activeId ? 'aqua' : getMarkerTone(id),
      }),
    )
    marker.setZIndexOffset(id === activeId ? 1200 : 0)
  })
}

function ensureMap() {
  if (map || !mapRef.value) return
  map = L.map(mapRef.value, { zoomControl: true, maxZoom: 20 }).setView(
    toMapDisplayPoint(defaultCenter[0], defaultCenter[1]), 9,
  )
  addPreferredTileLayer(map)
  markerLayer = L.markerClusterGroup({
    showCoverageOnHover: false,
    spiderfyOnMaxZoom: true,
    zoomToBoundsOnClick: true,
    disableClusteringAtZoom: 10,
    maxClusterRadius: 38,
    iconCreateFunction: createClusterIcon,
  }).addTo(map)
  scheduleInvalidateMap(120)

  if (typeof ResizeObserver !== 'undefined' && mapRef.value) {
    resizeObserver = new ResizeObserver(() => scheduleInvalidateMap(60))
    resizeObserver.observe(mapRef.value)
  }
}

function renderMarkers() {
  if (!map || !markerLayer) return

  markerLayer.clearLayers()
  markerMap.clear()

  if (!records.value.length) {
    map.setView(toMapDisplayPoint(defaultCenter[0], defaultCenter[1]), 9)
    scheduleInvalidateMap()
    return
  }

  const bounds: [number, number][] = []

  records.value.forEach((item) => {
    const lat = Number(item.latitude)
    const lng = Number(item.longitude)
    const point = toMapDisplayPoint(lat, lng)
    const sogColor = getSogColor(item.sog)
    const name = item.vesselName || item.mmsi || '未知'

    const popupHtml = buildMapPopupCard({
      eyebrow: 'AIS Vessel',
      title: name,
      subtitle: `MMSI ${item.mmsi || '-'}`,
      meta: formatTime(item.baseDateTime),
      chips: [
        { label: '航速', value: item.sog != null ? `${item.sog} 节` : '-' },
        { label: '航向', value: item.cog != null ? `${item.cog}°` : '-' },
      ],
      lines: [
        `坐标 ${formatCoord(lat)}, ${formatCoord(lng)}`,
        item.imo ? `IMO ${item.imo}` : '',
        item.linkedVessel ? `关联档案: ${item.linkedVessel.vesselName}` : '',
      ],
    })

    const marker = L.marker(point, {
      icon: createMapMarkerIcon(name, {
        active: selectedRecord.value?.id === item.id,
        tone: selectedRecord.value?.id === item.id ? 'aqua' : sogColor,
      }),
    })

    marker.bindPopup(popupHtml, { className: 'gsmv-map-popup' })
    marker.on('click', () => focusRecord(item.id, false))
    marker.addTo(markerLayer!)
    markerMap.set(item.id, marker)
    bounds.push(point)
  })

  if (bounds.length === 1) {
    map.setView(bounds[0], 14, { animate: false })
  } else {
    map.fitBounds(bounds, { padding: [24, 24], maxZoom: 12 })
  }
  updateMarkerVisualState(selectedRecord.value?.id)
  scheduleInvalidateMap()
}

async function loadDatasetDates() {
  try {
    datasetDates.value = await fetchAisDatasetDates()
  } catch {
    // non-critical
  }
}

async function loadMapData() {
  loading.value = true
  try {
    const pageData = await fetchAisMapRecords({
      keyword: keyword.value || undefined,
      datasetDate: selectedDatasetDate.value,
      limit: 5000,
    })
    records.value = pageData.items
    renderMarkers()
    if (records.value.length) {
      const currentId = selectedRecord.value?.id
      const found = currentId ? records.value.find((r) => r.id === currentId) : null
      if (found) {
        selectedRecord.value = found
        updateMarkerVisualState(found.id)
      } else {
        selectedRecord.value = records.value[0]
        updateMarkerVisualState(records.value[0].id)
      }
    } else {
      selectedRecord.value = null
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '航线地图加载失败')
  } finally {
    loading.value = false
  }
}

async function refreshMapData() {
  await loadMapData()
}

function focusRecord(id: string, openPopup: boolean) {
  detailLoading.value = true
  try {
    const found = records.value.find((r) => r.id === id)
    if (found) {
      selectedRecord.value = found
    }
    updateMarkerVisualState(id)
    const marker = markerMap.get(id)
    if (marker && map && markerLayer) {
      markerLayer.zoomToShowLayer(marker, () => {
        map?.setView(marker.getLatLng(), Math.max(map.getZoom(), 13), { animate: false })
        if (openPopup) marker.openPopup()
      })
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AIS 详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function showTrajectory() {
  if (!selectedRecord.value || !selectedRecord.value.mmsi || !map) return
  trajectoryLoading.value = true
  try {
    // Clear any existing trajectory first
    clearTrajectorySilent()

    const trackData = await fetchAisVesselTrack(selectedRecord.value.mmsi, 5000)
    const points = (trackData.items || [])
      .filter((p) => p.latitude != null && p.longitude != null)
      .map((p) => ({
        lat: Number(p.latitude),
        lng: Number(p.longitude),
        sog: p.sog ?? 0,
        dt: p.baseDateTime,
      }))
      .sort((a, b) => new Date(a.dt).getTime() - new Date(b.dt).getTime())

    if (points.length < 2) {
      ElMessage.warning('轨迹点不足，无法绘制轨迹')
      return
    }

    const latlngs: L.LatLngExpression[] = points.map((p) => [p.lat, p.lng])

    trajectoryGroup = L.layerGroup().addTo(map)

    // Draw trajectory polyline with a gradient effect — thick glow line + core line
    L.polyline(latlngs, {
      color: '#5af0ff',
      weight: 6,
      opacity: 0.28,
      smoothFactor: 1,
      interactive: false,
    }).addTo(trajectoryGroup)

    L.polyline(latlngs, {
      color: '#27e8ff',
      weight: 2.5,
      opacity: 0.9,
      smoothFactor: 1,
      interactive: false,
    }).addTo(trajectoryGroup)

    // Direction arrows along the path
    const arrowStep = Math.max(1, Math.floor(points.length / 20))
    for (let i = arrowStep; i < points.length - 1; i += arrowStep) {
      const arrowIcon = L.divIcon({
        html: `<svg width="14" height="14" viewBox="0 0 14 14" style="display:block"><polygon points="0,0 14,7 0,14 3,7" fill="#27e8ff" opacity="0.85"/></svg>`,
        className: 'trajectory-arrow',
        iconSize: [14, 14],
        iconAnchor: [7, 7],
      })
      L.marker([points[i].lat, points[i].lng], { icon: arrowIcon, interactive: false }).addTo(trajectoryGroup)
    }

    // Start/end markers
    const startIcon = L.divIcon({
      html: `<svg width="16" height="16" viewBox="0 0 16 16" style="display:block"><circle cx="8" cy="8" r="7" fill="#67f6d3" stroke="#0affc8" stroke-width="2"/></svg>`,
      className: '',
      iconSize: [16, 16],
      iconAnchor: [8, 8],
    })
    L.marker([points[0].lat, points[0].lng], { icon: startIcon, interactive: false }).addTo(trajectoryGroup)

    const endIcon = L.divIcon({
      html: `<svg width="16" height="16" viewBox="0 0 16 16" style="display:block"><rect x="1" y="1" width="14" height="14" rx="3" fill="#ff6b6b" stroke="#ff3b3b" stroke-width="2"/></svg>`,
      className: '',
      iconSize: [16, 16],
      iconAnchor: [8, 8],
    })
    L.marker([points[points.length - 1].lat, points[points.length - 1].lng], { icon: endIcon, interactive: false }).addTo(trajectoryGroup)

    // Fit map bounds to include trajectory
    const bounds = L.latLngBounds(latlngs as [number, number][])
    map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 })

    trajectoryVisible.value = true
    ElMessage.success(`已显示 ${selectedRecord.value.vesselName || selectedRecord.value.mmsi} 的航行轨迹（${points.length} 个点位）`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '轨迹加载失败')
  } finally {
    trajectoryLoading.value = false
  }
}

function clearTrajectorySilent() {
  trajectoryGroup?.remove()
  trajectoryGroup = null
  trajectoryVisible.value = false
}

function clearTrajectory() {
  clearTrajectorySilent()
  ElMessage.info('已清除轨迹')
}

function resetFilter() {
  keyword.value = ''
  selectedDatasetDate.value = undefined
  void refreshMapData()
}

function setupRevealObserver() {
  if (!pageShellRef.value || typeof IntersectionObserver === 'undefined') {
    pageShellRef.value?.querySelectorAll<HTMLElement>('.reveal-section').forEach((el) => {
      el.classList.add('is-visible')
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
    { threshold: 0.18, rootMargin: '0px 0px -10% 0px' },
  )
  pageShellRef.value.querySelectorAll<HTMLElement>('.reveal-section').forEach((el) => {
    revealObserver?.observe(el)
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
    await loadDatasetDates()
    await loadMapData()
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
  trajectoryGroup?.remove()
  trajectoryGroup = null
  map?.remove()
  map = null
  markerLayer = null
  markerMap.clear()
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

.reveal-section--hero { transition-delay: 0.02s; }
.reveal-section--story { transition-delay: 0.12s; }
.reveal-section--grid { transition-delay: 0.2s; }

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
  0%, 100% { transform: translate3d(0, 0, 0) scale(1); }
  50% { transform: translate3d(-10px, -8px, 0) scale(1.04); }
}

.route-map {
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

.toolbar-search-icon {
  font-size: 12px;
  opacity: 0.6;
}

.ship-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ship-item {
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

.ship-item__topline,
.ship-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ship-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid rgba(156, 241, 255, 0.18);
  background: rgba(90, 232, 255, 0.12);
  color: #8befff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  font-family: 'Consolas', monospace;
}

.ship-item__time {
  color: rgba(206, 235, 247, 0.76);
  font-size: 12px;
}

.ship-item strong {
  color: #f1fcff;
  font-size: 18px;
  line-height: 1.25;
}

.ship-item__meta {
  padding-top: 6px;
  border-top: 1px solid rgba(150, 232, 255, 0.08);
}

.ship-item__meta span {
  color: rgba(203, 234, 247, 0.7);
  font-size: 12px;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.detail-stat-card {
  padding: 14px 12px;
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

.detail-vessel-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(150, 232, 255, 0.16);
  background:
    linear-gradient(160deg, rgba(91, 233, 255, 0.1), rgba(6, 26, 62, 0.6)),
    rgba(7, 28, 64, 0.7);
}

.detail-vessel-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-vessel-card__info span {
  color: rgba(197, 232, 246, 0.7);
  font-size: 12px;
}

.detail-vessel-card__info strong {
  color: #f4fdff;
  font-size: 18px;
}

.ship-item:hover,
.ship-item.is-active {
  transform: translateY(-2px);
  border-color: rgba(126, 237, 255, 0.28);
  background:
    linear-gradient(145deg, rgba(108, 244, 255, 0.18), rgba(14, 70, 149, 0.34)),
    rgba(11, 36, 82, 0.9);
  box-shadow:
    0 14px 28px rgba(0, 10, 34, 0.2),
    0 0 0 1px rgba(115, 238, 255, 0.08) inset;
}

.ship-item.is-active {
  border-color: rgba(155, 244, 255, 0.42);
  box-shadow:
    0 18px 34px rgba(0, 10, 34, 0.24),
    0 0 0 1px rgba(155, 244, 255, 0.14) inset,
    0 0 26px rgba(79, 216, 255, 0.12);
}

.ship-item.is-active strong { color: #ffffff; }
.ship-item.is-active .ship-item__meta span { color: rgba(227, 249, 255, 0.88); }

@media (max-width: 1080px) {
  .map-story-band,
  .map-grid {
    grid-template-columns: 1fr;
  }
  .route-map { min-height: 460px; }
  .detail-stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .map-story-band__stats { grid-template-columns: 1fr; }
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
