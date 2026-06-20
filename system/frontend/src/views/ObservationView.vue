<template>
  <div class="page-shell">
    <section class="page-hero ais-hero">
      <div>
        <h2>AIS 记录</h2>
        <p>记录接收时间、船舶位置、航运节点、航行参数和关联船舶信息，并支持按查询结果批量维护。</p>
      </div>
      <div class="page-hero__actions">
        <div v-if="canWrite" class="import-action-group">
          <div class="limited-import">
            <span>前</span>
            <el-input-number
              v-model="importLimit"
              :min="1"
              :max="1000"
              controls-position="right"
              class="import-limit"
            />
            <span>条</span>
          </div>
          <el-upload
            action="#"
            accept=".csv,.zst,.tgz,.gz"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleLimitedFileSelected"
          >
            <el-button type="primary" :loading="importingMode === 'limited'">
              <el-icon><Upload /></el-icon>
              <span>导入前 {{ importLimit }} 条</span>
            </el-button>
          </el-upload>
          <el-upload
            action="#"
            accept=".csv,.zst,.tgz,.gz"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAllFileSelected"
          >
            <el-button class="import-all-button" type="success" plain :loading="importingMode === 'all'">
              <el-icon><Upload /></el-icon>
              <span>导入全部数据</span>
            </el-button>
          </el-upload>
        </div>
      </div>
    </section>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar query-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="MMSI / 船名 / IMO / 呼号 / 坐标 / 文件 / 备注"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        />
        <el-date-picker
          v-model="query.observedRange"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          class="date-range"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>查询</span>
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          <span>重置</span>
        </el-button>
      </div>

      <div class="result-toolbar">
        <div class="result-summary">
          <el-tag effect="dark" round>查询结果 {{ pagination.total }}</el-tag>
          <el-tag v-if="selectedRows.length" type="success" effect="dark" round>已选 {{ selectedRows.length }}</el-tag>
          <span v-if="activeFilterText" class="filter-text">{{ activeFilterText }}</span>
        </div>
        <div v-if="canWrite" class="batch-actions">
          <el-button :disabled="!selectedRows.length" @click="openBatchEdit('selected')">
            <el-icon><Edit /></el-icon>
            <span>修改选中</span>
          </el-button>
          <el-button :disabled="!pagination.total" @click="openBatchEdit('matched')">
            <el-icon><Operation /></el-icon>
            <span>修改查询结果</span>
          </el-button>
          <el-button type="danger" plain :disabled="!selectedRows.length" @click="confirmDelete('selected')">
            <el-icon><Delete /></el-icon>
            <span>删除选中</span>
          </el-button>
          <el-button type="danger" plain :disabled="!pagination.total" @click="confirmDelete('matched')">
            <el-icon><RemoveFilled /></el-icon>
            <span>删除查询结果</span>
          </el-button>
        </div>
      </div>

      <div v-if="lastImport" class="import-result">
        <strong>{{ lastImport.sourceFile }}</strong>
        <span>已导入 {{ lastImport.imported }} 条</span>
        <span v-if="lastImport.skipped">跳过 {{ lastImport.skipped }} 条</span>
        <span>{{ lastImport.limit ? `限制前 ${lastImport.limit} 条` : '全量导入' }}</span>
      </div>

      <div v-if="importProgress" class="import-progress">
        <div class="import-progress__meta">
          <strong>{{ importProgress.sourceFile || 'AIS 文件导入' }}</strong>
          <span>{{ importProgressText }}</span>
        </div>
        <el-progress
          :percentage="importProgress.progress"
          :status="importProgressStatus"
          :stroke-width="10"
          striped
          striped-flow
        />
      </div>

      <div class="ais-table-wrap">
        <el-table
          :data="rows"
          v-loading="loading"
          stripe
          row-key="id"
          empty-text="暂无 AIS 记录"
          @selection-change="handleSelectionChange"
        >
          <el-table-column v-if="canWrite" type="selection" width="48" reserve-selection />
          <el-table-column label="航运节点" min-width="130">
            <template #default="{ row }">
              {{ routeNodeLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="采集人员" min-width="112">
            <template #default="{ row }">
              {{ row.importedByName || '系统导入' }}
            </template>
          </el-table-column>
          <el-table-column label="接收时间" min-width="168">
            <template #default="{ row }">
              {{ formatDateTime(row.baseDateTime) }}
            </template>
          </el-table-column>
          <el-table-column label="位置说明" min-width="210" show-overflow-tooltip>
            <template #default="{ row }">
              {{ locationLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="坐标" min-width="174">
            <template #default="{ row }">
              {{ coordinateLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column label="航行参数" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ navigationSummary(row) }}
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.note || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" width="172">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button link type="primary" @click="showDetail(row)">
                  <el-icon><View /></el-icon>
                  <span>详情</span>
                </el-button>
                <el-button v-if="canWrite" link type="primary" @click="openRowEdit(row)">
                  <el-icon><Edit /></el-icon>
                  <span>修改</span>
                </el-button>
                <el-button v-if="canWrite" link type="danger" @click="confirmDelete('selected', [row])">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="editDialogVisible" :title="editDialogTitle" width="640px" destroy-on-close>
      <el-form label-position="top" class="batch-form">
        <el-alert
          :title="editScope === 'matched' ? `将作用于当前查询结果 ${pagination.total} 条` : `将作用于选中记录 ${selectedRows.length || 1} 条`"
          type="info"
          show-icon
          :closable="false"
        />

        <div class="field-grid">
          <label v-for="field in editableFields" :key="field.key" class="field-toggle">
            <el-checkbox v-model="editForm.enabled[field.key]" />
            <span>{{ field.label }}</span>
          </label>
        </div>

        <el-form-item v-if="editForm.enabled.vesselName" label="船名">
          <el-input v-model="editForm.values.vesselName" clearable />
        </el-form-item>
        <el-form-item v-if="editForm.enabled.imo" label="IMO">
          <el-input v-model="editForm.values.imo" clearable />
        </el-form-item>
        <el-form-item v-if="editForm.enabled.callSign" label="呼号">
          <el-input v-model="editForm.values.callSign" clearable />
        </el-form-item>
        <el-form-item v-if="editForm.enabled.transceiver" label="AIS 类别">
          <el-input v-model="editForm.values.transceiver" clearable />
        </el-form-item>
        <el-form-item v-if="editForm.enabled.note" label="备注">
          <el-input v-model="editForm.values.note" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>

        <div class="number-grid">
          <el-form-item v-if="editForm.enabled.sog" label="航速 kn">
            <el-input-number v-model="editForm.values.sog" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="editForm.enabled.cog" label="航向 °">
            <el-input-number v-model="editForm.values.cog" :min="0" :max="360" :precision="1" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="editForm.enabled.heading" label="船首向 °">
            <el-input-number v-model="editForm.values.heading" :min="0" :max="511" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="editForm.enabled.status" label="状态">
            <el-input-number v-model="editForm.values.status" :min="0" :max="65535" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="editForm.enabled.vesselType" label="船舶类型">
            <el-input-number v-model="editForm.values.vesselType" :min="0" :max="65535" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="editForm.enabled.draft" label="吃水 m">
            <el-input-number v-model="editForm.values.draft" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchOperating" @click="submitBatchEdit">保存修改</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" size="680px" title="AIS 详情">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="MMSI">{{ detail.mmsi }}</el-descriptions-item>
          <el-descriptions-item label="船名">{{ detail.vesselName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IMO">{{ detail.imo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="呼号">{{ detail.callSign || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收时间">{{ formatDateTime(detail.baseDateTime) }}</el-descriptions-item>
          <el-descriptions-item label="坐标">{{ coordinateLabel(detail) }}</el-descriptions-item>
          <el-descriptions-item label="来源文件">{{ detail.sourceFile || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.note || '-' }}</el-descriptions-item>
          <el-descriptions-item label="导入人员">{{ detail.importedByName || '系统导入' }}</el-descriptions-item>
          <el-descriptions-item label="导入时间">{{ formatDateTime(detail.importedAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-divider>航行参数</el-divider>

        <div class="ais-metric-grid">
          <div v-for="item in metricEntries" :key="item.label" class="ais-metric">
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  Delete,
  Edit,
  Operation,
  RefreshRight,
  RemoveFilled,
  Search,
  Upload,
  View,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadFile } from 'element-plus'
import {
  deleteAisRecords,
  fetchAisImportProgress,
  fetchAisRecords,
  importAisRecords,
  updateAisRecords,
} from '@/api/aisRecords'
import { useAuthStore } from '@/stores/auth'
import type { AisBatchOperationPayload, AisImportProgress, AisRecordView } from '@/types/gsmv'
import type { AisImportResult } from '@/types/gsmv'

type EditScope = 'selected' | 'matched'
type ImportMode = 'limited' | 'all'
type EditableKey =
  | 'vesselName'
  | 'imo'
  | 'callSign'
  | 'transceiver'
  | 'note'
  | 'sog'
  | 'cog'
  | 'heading'
  | 'status'
  | 'vesselType'
  | 'draft'

const authStore = useAuthStore()

const loading = ref(false)
const importingMode = ref<ImportMode | ''>('')
const batchOperating = ref(false)
const rows = ref<AisRecordView[]>([])
const selectedRows = ref<AisRecordView[]>([])
const detail = ref<AisRecordView | null>(null)
const detailVisible = ref(false)
const editDialogVisible = ref(false)
const editScope = ref<EditScope>('selected')
const importLimit = ref(10)
const lastImport = ref<AisImportResult | null>(null)
const importProgress = ref<AisImportProgress | null>(null)
let importProgressTimer: ReturnType<typeof window.setInterval> | null = null

const editableFields: { key: EditableKey; label: string }[] = [
  { key: 'vesselName', label: '船名' },
  { key: 'imo', label: 'IMO' },
  { key: 'callSign', label: '呼号' },
  { key: 'transceiver', label: 'AIS 类别' },
  { key: 'note', label: '备注' },
  { key: 'sog', label: '航速' },
  { key: 'cog', label: '航向' },
  { key: 'heading', label: '船首向' },
  { key: 'status', label: '状态' },
  { key: 'vesselType', label: '船舶类型' },
  { key: 'draft', label: '吃水' },
]

const editForm = reactive({
  enabled: Object.fromEntries(editableFields.map((field) => [field.key, false])) as Record<EditableKey, boolean>,
  values: {
    vesselName: '',
    imo: '',
    callSign: '',
    transceiver: '',
    note: '',
    sog: null as number | null,
    cog: null as number | null,
    heading: null as number | null,
    status: null as number | null,
    vesselType: null as number | null,
    draft: null as number | null,
  },
})

const canWrite = computed(() => authStore.authorities.includes('OBS_WRITE'))
const editDialogTitle = computed(() => (editScope.value === 'matched' ? '批量修改查询结果' : '批量修改选中记录'))
const activeFilterText = computed(() => {
  const parts: string[] = []
  if (query.keyword.trim()) {
    parts.push(`关键词：${query.keyword.trim()}`)
  }
  const [from, to] = query.observedRange
  if (from || to) {
    parts.push(`${from || '不限'} 至 ${to || '不限'}`)
  }
  return parts.join(' / ')
})
const importProgressStatus = computed(() => {
  if (importProgress.value?.status === 'completed') {
    return 'success'
  }
  if (importProgress.value?.status === 'failed') {
    return 'exception'
  }
  return undefined
})
const importProgressText = computed(() => {
  if (!importProgress.value) {
    return ''
  }
  const progress = importProgress.value
  const parts = [`${progress.progress}%`]
  if (progress.imported || progress.skipped) {
    parts.push(`已导入 ${progress.imported} 条`)
  }
  if (progress.skipped) {
    parts.push(`跳过 ${progress.skipped} 条`)
  }
  if (progress.limit) {
    parts.push(`限制 ${progress.limit} 条`)
  }
  if (progress.message) {
    parts.push(progress.message)
  }
  return parts.join(' · ')
})
const metricEntries = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    { label: '航速', value: formatMetric(detail.value.sog, 'kn') },
    { label: '航向', value: formatMetric(detail.value.cog, '°') },
    { label: '船首向', value: formatMetric(detail.value.heading, '°') },
    { label: '吃水', value: formatMetric(detail.value.draft, 'm') },
    { label: '船长', value: formatMetric(detail.value.length, 'm') },
    { label: '船宽', value: formatMetric(detail.value.width, 'm') },
    { label: '船舶类型', value: nullableText(detail.value.vesselType) },
    { label: 'AIS 类别', value: detail.value.transceiver || '-' },
  ]
})

const query = reactive({
  keyword: '',
  observedRange: [] as string[],
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

async function loadData() {
  loading.value = true
  try {
    const [observedFrom, observedTo] = query.observedRange
    const pageData = await fetchAisRecords({
      keyword: query.keyword.trim() || undefined,
      observedFrom: observedFrom || undefined,
      observedTo: observedTo || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    rows.value = pageData.items
    pagination.total = pageData.total
    selectedRows.value = selectedRows.value.filter((selected) => pageData.items.some((row) => row.id === selected.id))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AIS 记录加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  void loadData()
}

function handleReset() {
  query.keyword = ''
  query.observedRange = []
  pagination.page = 1
  void loadData()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 1
  void loadData()
}

function handleSelectionChange(selection: AisRecordView[]) {
  selectedRows.value = selection
}

function handleLimitedFileSelected(uploadFile: UploadFile) {
  void handleFileSelected(uploadFile, 'limited')
}

function handleAllFileSelected(uploadFile: UploadFile) {
  void handleFileSelected(uploadFile, 'all')
}

async function handleFileSelected(uploadFile: UploadFile, mode: ImportMode) {
  if (!uploadFile.raw) {
    return
  }
  importingMode.value = mode
  const taskId = createImportTaskId()
  importProgress.value = {
    taskId,
    sourceFile: uploadFile.name,
    status: 'running',
    bytesRead: 0,
    totalBytes: uploadFile.size || 0,
    imported: 0,
    skipped: 0,
    limit: mode === 'limited' ? importLimit.value : 0,
    progress: 1,
    message: '准备上传',
    startedAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
  startImportProgressPolling(taskId)
  try {
    lastImport.value = await importAisRecords(uploadFile.raw, mode === 'limited' ? importLimit.value : 0, taskId)
    await refreshImportProgress(taskId, true)
    ElMessage.success(`${mode === 'all' ? '全量导入完成' : '导入完成'}：${lastImport.value.imported} 条 AIS 记录`)
    pagination.page = 1
    await loadData()
  } catch (error) {
    stopImportProgressPolling()
    if (importProgress.value) {
      importProgress.value = {
        ...importProgress.value,
        status: 'failed',
        progress: importProgress.value.progress || 1,
        message: error instanceof Error ? error.message : 'AIS 文件导入失败',
      }
    }
    ElMessage.error(error instanceof Error ? error.message : 'AIS 文件导入失败')
  } finally {
    importingMode.value = ''
  }
}

function createImportTaskId() {
  const random = Math.random().toString(36).slice(2, 10)
  return `ais-import-${Date.now()}-${random}`
}

function startImportProgressPolling(taskId: string) {
  stopImportProgressPolling()
  importProgressTimer = window.setInterval(() => {
    void refreshImportProgress(taskId)
  }, 1000)
}

function stopImportProgressPolling() {
  if (importProgressTimer) {
    window.clearInterval(importProgressTimer)
    importProgressTimer = null
  }
}

async function refreshImportProgress(taskId: string, finalRefresh = false) {
  try {
    const progress = await fetchAisImportProgress(taskId)
    importProgress.value = progress
    if (finalRefresh || progress.status === 'completed' || progress.status === 'failed') {
      stopImportProgressPolling()
    }
  } catch {
    if (finalRefresh) {
      stopImportProgressPolling()
    }
  }
}

function showDetail(row: AisRecordView) {
  detail.value = row
  detailVisible.value = true
}

function openRowEdit(row: AisRecordView) {
  resetEditForm()
  editScope.value = 'selected'
  selectedRows.value = [row]
  editForm.enabled.vesselName = true
  editForm.enabled.note = true
  editForm.values.vesselName = row.vesselName || ''
  editForm.values.imo = row.imo || ''
  editForm.values.callSign = row.callSign || ''
  editForm.values.transceiver = row.transceiver || ''
  editForm.values.note = row.note || ''
  editForm.values.sog = row.sog ?? null
  editForm.values.cog = row.cog ?? null
  editForm.values.heading = row.heading ?? null
  editForm.values.status = row.status ?? null
  editForm.values.vesselType = row.vesselType ?? null
  editForm.values.draft = row.draft ?? null
  editDialogVisible.value = true
}

function openBatchEdit(scope: EditScope) {
  if (scope === 'selected' && !selectedRows.value.length) {
    ElMessage.warning('请先勾选 AIS 记录')
    return
  }
  resetEditForm()
  editScope.value = scope
  editForm.enabled.note = true
  editDialogVisible.value = true
}

async function submitBatchEdit() {
  const fields = buildUpdateFields()
  if (!Object.keys(fields).length) {
    ElMessage.warning('请选择要修改的字段')
    return
  }
  batchOperating.value = true
  try {
    const result = await updateAisRecords({
      ...buildOperationPayload(editScope.value),
      fields,
    })
    ElMessage.success(`已修改 ${result.affected} 条 AIS 记录`)
    editDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AIS 记录修改失败')
  } finally {
    batchOperating.value = false
  }
}

async function confirmDelete(scope: EditScope, explicitRows?: AisRecordView[]) {
  const rowsToDelete = explicitRows || selectedRows.value
  if (scope === 'selected' && !rowsToDelete.length) {
    ElMessage.warning('请先勾选 AIS 记录')
    return
  }
  const countLabel = scope === 'matched' ? `${pagination.total} 条查询结果` : `${rowsToDelete.length} 条选中记录`
  try {
    await ElMessageBox.confirm(`确认删除 ${countLabel}？`, '删除 AIS 记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    batchOperating.value = true
    const result = await deleteAisRecords(buildOperationPayload(scope, rowsToDelete))
    ElMessage.success(`已删除 ${result.affected} 条 AIS 记录`)
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : 'AIS 记录删除失败')
    }
  } finally {
    batchOperating.value = false
  }
}

function buildOperationPayload(scope: EditScope, explicitRows?: AisRecordView[]): AisBatchOperationPayload {
  if (scope === 'matched') {
    const [observedFrom, observedTo] = query.observedRange
    return {
      allMatched: true,
      keyword: query.keyword.trim() || undefined,
      observedFrom: observedFrom || undefined,
      observedTo: observedTo || undefined,
    }
  }
  return {
    ids: (explicitRows || selectedRows.value).map((row) => row.id),
  }
}

function buildUpdateFields() {
  const fields: Record<string, string | number | null> = {}
  editableFields.forEach(({ key }) => {
    if (!editForm.enabled[key]) {
      return
    }
    fields[key] = editForm.values[key]
  })
  return fields
}

function resetEditForm() {
  editableFields.forEach(({ key }) => {
    editForm.enabled[key] = false
  })
  editForm.values.vesselName = ''
  editForm.values.imo = ''
  editForm.values.callSign = ''
  editForm.values.transceiver = ''
  editForm.values.note = ''
  editForm.values.sog = null
  editForm.values.cog = null
  editForm.values.heading = null
  editForm.values.status = null
  editForm.values.vesselType = null
  editForm.values.draft = null
}

function routeNodeLabel(row: AisRecordView) {
  if (row.sourceFile?.includes('2025')) {
    return 'AIS 数据集'
  }
  return row.sourceFile || '本地 AIS'
}

function locationLabel(row: AisRecordView) {
  const vessel = row.vesselName || '未知船舶'
  return `${vessel} / MMSI ${row.mmsi}`
}

function coordinateLabel(row: AisRecordView) {
  return `${formatCoordinate(row.latitude)}, ${formatCoordinate(row.longitude)}`
}

function navigationSummary(row: AisRecordView) {
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

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ') : '-'
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

function nullableText(value: number | string | null | undefined) {
  return value == null || value === '' ? '-' : String(value)
}

onMounted(() => {
  void loadData()
})

onBeforeUnmount(() => {
  stopImportProgressPolling()
})
</script>

<style scoped>
.page-shell {
  min-width: 0;
  overflow-x: hidden;
}

.panel-card {
  min-width: 0;
}

.panel-card :deep(.el-card__body) {
  min-width: 0;
}

.ais-hero {
  align-items: center;
}

.import-action-group,
.limited-import {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.limited-import {
  gap: 6px;
  flex-wrap: nowrap;
  color: rgba(222, 246, 255, 0.86);
  font-size: 13px;
  font-weight: 700;
}

.import-limit {
  width: 116px;
}

.import-all-button {
  --el-button-bg-color: rgba(30, 208, 126, 0.14);
  --el-button-border-color: rgba(80, 229, 155, 0.5);
  --el-button-text-color: #aaffd2;
  --el-button-hover-bg-color: rgba(30, 208, 126, 0.22);
  --el-button-hover-border-color: rgba(116, 255, 182, 0.72);
  --el-button-hover-text-color: #ffffff;
}

.query-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(340px, 1.4fr) auto auto;
  gap: 12px;
  align-items: center;
}

.keyword-input,
.date-range {
  width: 100%;
}

.result-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;
  min-width: 0;
}

.result-summary,
.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.batch-actions :deep(.el-button) {
  margin-left: 0;
}

.ais-table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.ais-table-wrap :deep(.el-table) {
  min-width: 1434px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 8px;
  align-items: center;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}

.filter-text {
  color: rgba(226, 247, 255, 0.72);
  font-size: 13px;
}

.import-result {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin: 0 0 16px;
  padding: 10px 14px;
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.1), rgba(124, 60, 255, 0.08)),
    rgba(255, 255, 255, 0.05);
  color: rgba(231, 248, 255, 0.9);
}

.import-result strong {
  color: #f5fdff;
}

.import-progress {
  display: grid;
  gap: 10px;
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid rgba(0, 229, 255, 0.22);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.1), rgba(124, 60, 255, 0.08)),
    rgba(255, 255, 255, 0.05);
}

.import-progress__meta {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  color: rgba(231, 248, 255, 0.86);
  font-size: 13px;
}

.import-progress__meta strong {
  color: #f5fdff;
}

.import-progress__meta span {
  color: rgba(214, 240, 255, 0.72);
}

.import-progress :deep(.el-progress-bar__outer) {
  background-color: rgba(9, 19, 45, 0.72);
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.batch-form {
  display: grid;
  gap: 16px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.field-toggle {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid rgba(117, 198, 255, 0.18);
  border-radius: 8px;
  background: rgba(12, 42, 86, 0.55);
  color: rgba(235, 249, 255, 0.9);
}

.number-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.number-grid :deep(.el-input-number) {
  width: 100%;
}

.ais-metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ais-metric {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 88px;
  padding: 16px 18px;
  border: 1px solid rgba(157, 233, 255, 0.16);
  border-radius: 20px;
  background:
    radial-gradient(circle at 12% 0%, rgba(110, 233, 255, 0.12), transparent 34%),
    linear-gradient(180deg, rgba(10, 41, 93, 0.92), rgba(5, 21, 58, 0.96));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    0 14px 28px rgba(2, 14, 44, 0.2);
}

.ais-metric strong {
  color: #f3fdff;
  font-size: 14px;
}

.ais-metric span {
  color: rgba(222, 246, 255, 0.9);
  font-size: 24px;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .query-toolbar {
    grid-template-columns: 1fr;
  }

  .result-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .field-grid,
  .number-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .field-grid,
  .number-grid,
  .ais-metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
