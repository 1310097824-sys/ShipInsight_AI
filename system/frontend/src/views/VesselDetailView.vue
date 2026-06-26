<template>
  <div class="page-shell" v-loading="loading">
    <section class="page-hero vessel-detail-hero">
      <div>
        <h2>{{ detail?.vesselName || '船舶详情' }}</h2>
        <p>{{ detail ? formatHeroSubtitle(detail) : '查看船舶主档、运营信息、风险状态、航线范围、图片资料和版本历史。' }}</p>
      </div>
      <div class="detail-actions">
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          返回列表
        </el-button>
      </div>
    </section>

    <template v-if="detail">
      <section class="detail-summary">
        <div class="summary-tile">
          <span>MMSI</span>
          <strong>{{ detail.mmsi || '-' }}</strong>
        </div>
        <div class="summary-tile">
          <span>IMO</span>
          <strong>{{ detail.imo || '-' }}</strong>
        </div>
        <div class="summary-tile">
          <span>风险等级</span>
          <el-tag :type="riskTagType(detail.riskLevel)" effect="plain">{{ detail.riskLevel || '未评估' }}</el-tag>
        </div>
        <div class="summary-tile">
          <span>航行状态</span>
          <el-tag :type="navigationTagType(detail.navigationStatus)" effect="plain">{{ detail.navigationStatus || '未知' }}</el-tag>
        </div>
      </section>

      <el-card class="panel-card ais-link-card" shadow="never">
        <template #header>
          <div class="panel-header">
            <strong>AIS 动态</strong>
            <el-button type="primary" plain :disabled="!aisQueryKeyword" @click="openAllAisRecords">
              查看全部 AIS 记录
            </el-button>
          </div>
        </template>

        <el-alert
          v-if="aisError"
          title="AIS 动态暂时不可用"
          :description="aisError"
          type="warning"
          show-icon
          :closable="false"
        />
        <div v-else v-loading="aisLoading" class="ais-link-body">
          <section class="ais-summary-grid">
            <div class="ais-summary-item">
              <span>关联记录</span>
              <strong>{{ aisSummary?.totalRecords ?? 0 }}</strong>
            </div>
            <div class="ais-summary-item">
              <span>首次接收</span>
              <strong>{{ formatDateTime(aisSummary?.firstBaseDateTime) }}</strong>
            </div>
            <div class="ais-summary-item">
              <span>最近接收</span>
              <strong>{{ formatDateTime(aisSummary?.latestBaseDateTime) }}</strong>
            </div>
            <div class="ais-summary-item">
              <span>最近位置</span>
              <strong>{{ latestPositionLabel }}</strong>
            </div>
          </section>

          <el-table :data="aisRecords" stripe empty-text="暂无关联 AIS 记录">
            <el-table-column label="接收时间" min-width="170">
              <template #default="{ row }">
                {{ formatDateTime(row.baseDateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="位置" min-width="190">
              <template #default="{ row }">
                {{ coordinateLabel(row) }}
              </template>
            </el-table-column>
            <el-table-column label="航行参数" min-width="230" show-overflow-tooltip>
              <template #default="{ row }">
                {{ navigationSummary(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="sourceFile" label="来源文件" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </el-card>

      <el-card class="panel-card" shadow="never">
        <template #header>
          <strong>基础档案</strong>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="船名">{{ detail.vesselName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="呼号">{{ detail.callSign || '-' }}</el-descriptions-item>
          <el-descriptions-item label="船型">{{ detail.vesselTypePath || detail.vesselTypeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="船旗">{{ detail.flagState || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运营方">{{ detail.operatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所有方">{{ detail.ownerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="船舶尺度">{{ formatDimensions(detail) }}</el-descriptions-item>
          <el-descriptions-item label="吨位">{{ formatTonnage(detail) }}</el-descriptions-item>
          <el-descriptions-item label="母港 / 常驻港">{{ detail.homePort || '-' }}</el-descriptions-item>
          <el-descriptions-item label="档案状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ detail.status === 1 ? '启用' : '归档' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updatedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="常用区域" :span="2">{{ detail.usualRegion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="航线范围" :span="2">{{ detail.routeArea || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card v-if="detail.images.length" class="panel-card" shadow="never">
        <template #header>
          <strong>图片资料</strong>
        </template>

        <div class="image-gallery">
          <el-image
            v-for="item in detail.images"
            :key="item.id"
            :src="item.url"
            :preview-src-list="imageUrls"
            fit="cover"
            class="image-gallery__item"
          />
        </div>
      </el-card>

      <div class="detail-grid">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>运营与风险</strong>
          </template>

          <div class="detail-block">
            <h3>航行状态</h3>
            <p>{{ detail.navigationStatus || '暂无记录' }}</p>
          </div>
          <div class="detail-block">
            <h3>风险说明</h3>
            <p>{{ detail.riskLevel || '暂无记录' }}</p>
          </div>
          <div class="detail-block">
            <h3>常用区域</h3>
            <p>{{ detail.usualRegion || '暂无记录' }}</p>
          </div>
          <div class="detail-block">
            <h3>航线范围</h3>
            <p>{{ detail.routeArea || '暂无记录' }}</p>
          </div>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>备注与来源</strong>
          </template>

          <div class="detail-block">
            <h3>备注</h3>
            <p>{{ detail.note || '暂无记录' }}</p>
          </div>
          <div class="detail-block">
            <h3>资料来源</h3>
            <ul v-if="sourceItems.length" class="source-list">
              <li v-for="item in sourceItems" :key="item">{{ item }}</li>
            </ul>
            <p v-else>暂无记录</p>
          </div>
        </el-card>
      </div>

      <VersionHistoryPanel
        title="版本历史与回滚"
        description="查看每次船舶档案变更的字段差异、操作人和时间，并可回滚到指定历史版本。"
        empty-text="当前船舶档案还没有版本记录。"
        :versions="versions"
        :loading="versionsLoading"
        :can-rollback="canRollback"
        :rollbacking-version-id="rollbackingVersionId"
        @rollback="handleRollback"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { Back } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { fetchVesselAisRecords, fetchVesselAisSummary, fetchVesselDetail, fetchVesselVersions, rollbackVesselVersion } from '@/api/vessels'
import VersionHistoryPanel from '@/components/VersionHistoryPanel.vue'
import { useAuthStore } from '@/stores/auth'
import { listenDataChanged, notifyDataChanged } from '@/utils/dataSync'
import type { AisRecordView, AisVesselSummaryView, EntityVersionView, VesselDetailView } from '@/types/gsmv'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const detail = ref<VesselDetailView | null>(null)
const versions = ref<EntityVersionView[]>([])
const aisSummary = ref<AisVesselSummaryView | null>(null)
const aisRecords = ref<AisRecordView[]>([])
const versionsLoading = ref(false)
const aisLoading = ref(false)
const aisError = ref('')
const rollbackingVersionId = ref<number | null>(null)
let stopDataSync: (() => void) | undefined
let refreshTimer: number | undefined

const vesselId = computed(() => Number(route.params.id))
const aisQueryKeyword = computed(() => detail.value?.mmsi || detail.value?.imo || '')
const latestPositionLabel = computed(() => {
  const latest = aisSummary.value?.latestRecord
  return latest ? coordinateLabel(latest) : '-'
})
const imageUrls = computed(() => detail.value?.images.map((item) => item.url) || [])
const sourceItems = computed(() =>
  (detail.value?.sourceText || '')
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean),
)
const canRollback = computed(
  () =>
    authStore.authorities.includes('VESSEL_WRITE') ||
    authStore.profile?.roles.includes('ADMIN') ||
    authStore.authorities.includes('SPECIES_WRITE'),
)

async function loadDetail() {
  if (!vesselId.value || Number.isNaN(vesselId.value)) {
    router.replace('/vessels')
    return
  }

  loading.value = true
  try {
    detail.value = await fetchVesselDetail(vesselId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '船舶详情加载失败')
    router.replace('/vessels')
  } finally {
    loading.value = false
  }
}

async function loadVersions() {
  if (!vesselId.value || Number.isNaN(vesselId.value)) {
    return
  }

  versionsLoading.value = true
  try {
    versions.value = await fetchVesselVersions(vesselId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '版本历史加载失败')
  } finally {
    versionsLoading.value = false
  }
}

async function loadAisData() {
  if (!vesselId.value || Number.isNaN(vesselId.value)) {
    return
  }

  aisLoading.value = true
  aisError.value = ''
  try {
    const [summary, pageData] = await Promise.all([
      fetchVesselAisSummary(vesselId.value),
      fetchVesselAisRecords(vesselId.value, { page: 1, size: 5 }),
    ])
    aisSummary.value = summary
    aisRecords.value = pageData.items
  } catch (error) {
    aisSummary.value = null
    aisRecords.value = []
    aisError.value = error instanceof Error ? error.message : 'AIS 动态加载失败'
  } finally {
    aisLoading.value = false
  }
}

async function refreshPage() {
  await Promise.all([loadDetail(), loadVersions(), loadAisData()])
}

function goBack() {
  router.push('/vessels')
}

function openAllAisRecords() {
  if (!aisQueryKeyword.value) {
    return
  }
  router.push({ path: '/observations', query: { keyword: aisQueryKeyword.value } })
}

async function handleRollback(version: EntityVersionView) {
  if (!vesselId.value || Number.isNaN(vesselId.value)) {
    return
  }

  try {
    await ElMessageBox.confirm(
      `回滚后会将当前船舶档案恢复到 V${version.versionNo}，并生成一条新的回滚记录。确认继续吗？`,
      '回滚船舶档案',
      {
        type: 'warning',
        confirmButtonText: '确认回滚',
        cancelButtonText: '取消',
      },
    )
    rollbackingVersionId.value = version.id
    detail.value = await rollbackVesselVersion(vesselId.value, version.id)
    notifyDataChanged('vessel')
    ElMessage.success(`已回滚到 V${version.versionNo}`)
    await refreshPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '船舶档案回滚失败')
  } finally {
    rollbackingVersionId.value = null
  }
}

function formatHeroSubtitle(item: VesselDetailView) {
  return [item.vesselTypePath || item.vesselTypeName, item.flagState, item.operatorName].filter(Boolean).join(' · ') || '船舶主档详情'
}

function formatDimensions(item: Pick<VesselDetailView, 'lengthM' | 'widthM' | 'draftM'>) {
  const parts = [item.lengthM && `长 ${item.lengthM}m`, item.widthM && `宽 ${item.widthM}m`, item.draftM && `吃水 ${item.draftM}m`]
  return parts.filter(Boolean).join(' / ') || '-'
}

function formatTonnage(item: Pick<VesselDetailView, 'grossTonnage' | 'deadweightTonnage'>) {
  const parts = [item.grossTonnage && `总吨 ${item.grossTonnage}`, item.deadweightTonnage && `载重吨 ${item.deadweightTonnage}`]
  return parts.filter(Boolean).join(' / ') || '-'
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ') : '-'
}

function coordinateLabel(row: Pick<AisRecordView, 'latitude' | 'longitude'>) {
  return `${formatCoordinate(row.latitude)}, ${formatCoordinate(row.longitude)}`
}

function navigationSummary(row: Pick<AisRecordView, 'sog' | 'cog' | 'heading' | 'draft' | 'status'>) {
  return [
    `航速 ${formatMetric(row.sog, 'kn')}`,
    `航向 ${formatMetric(row.cog, '°')}`,
    `船首 ${formatMetric(row.heading, '°')}`,
    `吃水 ${formatMetric(row.draft, 'm')}`,
    row.status == null ? '' : `状态 ${row.status}`,
  ]
    .filter(Boolean)
    .join(' / ')
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

function riskTagType(value?: string) {
  if (!value) return 'info'
  if (value.includes('高') || value.includes('重点')) return 'danger'
  if (value.includes('中') || value.includes('普通')) return 'warning'
  return 'success'
}

function navigationTagType(value?: string) {
  if (!value || value === '未知') return 'info'
  if (value.includes('异常') || value.includes('失控')) return 'danger'
  if (value.includes('锚泊') || value.includes('靠泊')) return 'warning'
  return 'success'
}

function handleFocus() {
  void refreshPage()
}

function handleVisibilityChange() {
  if (!document.hidden) {
    void refreshPage()
  }
}

watch(
  () => route.params.id,
  () => {
    void refreshPage()
  },
)

onMounted(async () => {
  stopDataSync = listenDataChanged((detailEvent) => {
    if (detailEvent.type === 'vessel') {
      void refreshPage()
    }
  })
  window.addEventListener('focus', handleFocus)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  refreshTimer = window.setInterval(() => {
    if (!document.hidden) {
      void refreshPage()
    }
  }, 15000)
  await refreshPage()
})

onBeforeUnmount(() => {
  stopDataSync?.()
  window.removeEventListener('focus', handleFocus)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.vessel-detail-hero {
  align-items: flex-start;
}

.detail-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 12px;
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.ais-link-body {
  display: grid;
  gap: 16px;
}

.ais-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ais-summary-item {
  min-height: 82px;
  padding: 14px 16px;
  border: 1px solid rgba(157, 233, 255, 0.16);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.08), rgba(124, 60, 255, 0.06)),
    rgba(255, 255, 255, 0.04);
}

.ais-summary-item span {
  display: block;
  margin-bottom: 8px;
  color: var(--gsmv-muted);
  font-size: 13px;
}

.ais-summary-item strong {
  display: block;
  color: #f2fdff;
  font-size: 18px;
  line-height: 1.35;
  word-break: break-word;
}

.summary-tile {
  min-height: 92px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.12), rgba(124, 60, 255, 0.08)),
    rgba(255, 255, 255, 0.06);
  box-shadow: var(--gsmv-shadow-soft);
}

.summary-tile span {
  display: block;
  margin-bottom: 10px;
  color: var(--gsmv-muted);
  font-size: 13px;
}

.summary-tile strong {
  display: block;
  color: #f2fdff;
  font-size: 22px;
  word-break: break-word;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 14px;
}

.image-gallery__item {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 18px;
  border: 1px solid rgba(0, 229, 255, 0.18);
  overflow: hidden;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.detail-block + .detail-block {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.detail-block h3 {
  margin: 0 0 8px;
  color: var(--gsmv-primary);
  font-size: 15px;
}

.detail-block p {
  margin: 0;
  color: var(--gsmv-text);
  line-height: 1.8;
  white-space: pre-wrap;
}

.source-list {
  margin: 0;
  padding-left: 18px;
  color: var(--gsmv-text);
  line-height: 1.8;
}

@media (max-width: 1080px) {
  .detail-summary,
  .detail-grid,
  .ais-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .detail-summary,
  .detail-grid,
  .ais-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
