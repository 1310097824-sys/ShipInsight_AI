<template>
  <div class="page-shell">
    <section class="page-hero vessel-hero">
      <div>
        <h2>船舶档案</h2>
        <p>
          维护船名、MMSI、IMO、呼号、船型、船旗、运营方、风险等级、航行状态和常用航线范围。
          AIS 明细继续由 ClickHouse 承载，这里只保存低频、可审计的船舶主档。
        </p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" plain @click="openAiCreate">
          <el-icon><MagicStick /></el-icon>
          AI 档案补全
        </el-button>
        <el-button v-if="canWrite" type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon>
          新增船舶
        </el-button>
      </div>
    </section>

    <section class="vessel-metrics">
      <div class="vessel-metric">
        <span>当前结果</span>
        <strong>{{ pagination.total }}</strong>
      </div>
      <div class="vessel-metric">
        <span>启用档案</span>
        <strong>{{ activeCount }}</strong>
      </div>
      <div class="vessel-metric">
        <span>重点关注</span>
        <strong>{{ highRiskCount }}</strong>
      </div>
      <div class="vessel-metric">
        <span>船型覆盖</span>
        <strong>{{ typeCount }}</strong>
      </div>
    </section>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar toolbar--wrap">
        <el-input
          v-model="query.keyword"
          placeholder="船名 / MMSI / IMO / 呼号"
          clearable
          style="max-width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-cascader
          v-model="query.typeId"
          :options="typeOptions"
          :props="typeCascaderProps"
          filterable
          clearable
          style="width: 240px"
          placeholder="船型"
        />
        <el-select v-model="query.riskLevel" placeholder="风险等级" clearable style="width: 160px">
          <el-option v-for="option in riskLevelOptions" :key="option" :label="option" :value="option" />
        </el-select>
        <el-select v-model="query.navigationStatus" placeholder="航行状态" clearable style="width: 160px">
          <el-option v-for="option in navigationStatusOptions" :key="option" :label="option" :value="option" />
        </el-select>
        <el-input
          v-model="query.routeKeyword"
          placeholder="常用区域 / 航线范围"
          clearable
          style="max-width: 220px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.status" placeholder="档案状态" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="归档" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="vesselName" label="船名" min-width="170" fixed="left" show-overflow-tooltip />
        <el-table-column label="MMSI / IMO" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatIdentity(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="callSign" label="呼号" min-width="110" />
        <el-table-column label="船型" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.vesselTypePath || row.vesselTypeName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="flagState" label="船旗" min-width="110" />
        <el-table-column prop="operatorName" label="运营方" min-width="170" show-overflow-tooltip />
        <el-table-column label="尺度" min-width="150">
          <template #default="{ row }">
            {{ formatDimensions(row) }}
          </template>
        </el-table-column>
        <el-table-column label="风险等级" min-width="120">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ row.riskLevel || '未评估' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="航行状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="navigationTagType(row.navigationStatus)" effect="plain">{{ row.navigationStatus || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usualRegion" label="常用区域" min-width="180" show-overflow-tooltip />
        <el-table-column prop="routeArea" label="航线范围" min-width="220" show-overflow-tooltip />
        <el-table-column label="档案状态" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '归档' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" fixed="right" :width="canWrite ? 230 : 96">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" @click="openDetail(row.id)">
                <el-icon><View /></el-icon>
                详情
              </el-button>
              <el-button v-if="canWrite" link type="primary" @click="openEdit(row.id)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button v-if="canWrite" link type="danger" @click="archiveRow(row.id)">
                <el-icon><Delete /></el-icon>
                归档
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑船舶档案' : '新增船舶档案'" width="1080px" top="3vh">
      <div v-loading="dialogLoading">
        <div class="ai-panel">
          <div>
            <strong>AI 档案补全</strong>
            <p>根据已填写的船名、MMSI、IMO、船型、船旗和区域信息生成建议字段。建议只会写入当前表单，确认无误并保存后才会入库。</p>
          </div>
          <el-button type="primary" plain :loading="aiAutocompleting" @click="runAutocomplete">
            <el-icon><MagicStick /></el-icon>
            生成建议
          </el-button>
        </div>

        <div v-if="aiSuggestions.length" class="suggestion-list">
          <el-tag v-for="item in aiSuggestions" :key="item" effect="plain" round>{{ item }}</el-tag>
        </div>

        <el-form label-position="top">
          <div class="vessel-form__grid">
            <el-form-item label="船名">
              <el-input v-model="form.vesselName" placeholder="例如 APL HORIZON" />
            </el-form-item>
            <el-form-item label="MMSI">
              <el-input v-model="form.mmsi" maxlength="32" placeholder="9 位 MMSI，可为空" />
            </el-form-item>
            <el-form-item label="IMO">
              <el-input v-model="form.imo" maxlength="32" placeholder="IMO 编号，可为空" />
            </el-form-item>
            <el-form-item label="呼号">
              <el-input v-model="form.callSign" maxlength="32" />
            </el-form-item>
            <el-form-item label="船型">
              <el-cascader
                v-model="form.vesselTypeId"
                :options="typeOptions"
                :props="typeCascaderProps"
                filterable
                clearable
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="船旗">
              <el-input v-model="form.flagState" placeholder="例如 中国 / 新加坡 / 巴拿马" />
            </el-form-item>
            <el-form-item label="运营方">
              <el-input v-model="form.operatorName" />
            </el-form-item>
            <el-form-item label="所有方">
              <el-input v-model="form.ownerName" />
            </el-form-item>
            <el-form-item label="航行状态">
              <el-select v-model="form.navigationStatus" clearable filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="option in navigationStatusOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel" clearable filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="option in riskLevelOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </el-form-item>
            <el-form-item label="母港 / 常驻港">
              <el-input v-model="form.homePort" />
            </el-form-item>
            <el-form-item label="档案状态">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">归档</el-radio>
              </el-radio-group>
            </el-form-item>
          </div>

          <div class="vessel-form__grid vessel-form__grid--numbers">
            <el-form-item label="船长（m）">
              <el-input-number v-model="form.lengthM" :precision="2" :step="1" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="船宽（m）">
              <el-input-number v-model="form.widthM" :precision="2" :step="1" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="吃水（m）">
              <el-input-number v-model="form.draftM" :precision="2" :step="0.1" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="总吨">
              <el-input-number v-model="form.grossTonnage" :precision="2" :step="100" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="载重吨">
              <el-input-number v-model="form.deadweightTonnage" :precision="2" :step="100" :min="0" style="width: 100%" />
            </el-form-item>
          </div>

          <el-form-item label="常用区域">
            <el-input v-model="form.usualRegion" type="textarea" :rows="2" placeholder="例如 北美西岸 / 跨太平洋航线" />
          </el-form-item>
          <el-form-item label="航线范围">
            <el-input v-model="form.routeArea" type="textarea" :rows="3" placeholder="例如 洛杉矶港、长滩港至北太平洋主干线" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.note" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="资料来源">
            <el-input
              v-model="form.sourceText"
              type="textarea"
              :rows="3"
              placeholder="登记资料、AIS 导入来源、RAG 文档或人工核验说明"
            />
          </el-form-item>

          <el-form-item v-if="existingImages.length" label="已上传图片/资料">
            <div class="vessel-image-grid">
              <el-image
                v-for="item in existingImages"
                :key="item.id"
                :src="item.url"
                :preview-src-list="existingImageUrls"
                fit="cover"
                class="vessel-image-grid__item"
              />
            </div>
          </el-form-item>

          <el-form-item label="图片/资料上传">
            <el-upload
              v-model:file-list="pendingImageFiles"
              action="#"
              :auto-upload="false"
              list-type="picture-card"
              accept="image/*"
              multiple
            >
              <el-icon><UploadFilled /></el-icon>
            </el-upload>
            <div class="field-tip">保存船舶档案后会自动上传选中的图片资料。</div>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, Edit, MagicStick, Plus, Refresh, Search, UploadFilled, View } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CascaderOption, UploadUserFile } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  archiveVessel,
  createVessel,
  fetchVesselDetail,
  fetchVessels,
  fetchVesselTypes,
  updateVessel,
  uploadVesselImage,
} from '@/api/vessels'
import { useAuthStore } from '@/stores/auth'
import { listenDataChanged, notifyDataChanged } from '@/utils/dataSync'
import type { VesselDetailView, VesselImageView, VesselSavePayload, VesselTypeOption, VesselView } from '@/types/gsmv'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const dialogLoading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const aiAutocompleting = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<VesselView[]>([])
const vesselTypes = ref<VesselTypeOption[]>([])
const existingImages = ref<VesselImageView[]>([])
const pendingImageFiles = ref<UploadUserFile[]>([])
const aiSuggestions = ref<string[]>([])
let stopDataSync: (() => void) | undefined
let refreshTimer: number | undefined

const canWrite = computed(
  () =>
    (authStore.authorities || []).includes('VESSEL_WRITE') ||
    authStore.roleCodes.includes('ADMIN') ||
    (authStore.authorities || []).includes('SPECIES_WRITE'),
)
const existingImageUrls = computed(() => existingImages.value.map((item) => item.url))
const activeCount = computed(() => rows.value.filter((item) => item.status === 1).length)
const highRiskCount = computed(() => rows.value.filter((item) => item.riskLevel?.includes('重点') || item.riskLevel?.includes('高')).length)
const typeCount = computed(() => new Set(rows.value.map((item) => item.vesselTypeName).filter(Boolean)).size)

const riskLevelOptions = ['重点关注', '普通关注', '高风险', '中风险', '低风险', '白名单船舶']
const navigationStatusOptions = ['在航', '锚泊', '靠泊', '港内作业', '受限机动', '失控/异常', '未知']
const typeCascaderProps = {
  checkStrictly: true,
  emitPath: false,
  value: 'id',
  label: 'label',
  children: 'children',
}

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
  typeId: undefined as number | undefined,
  riskLevel: '',
  navigationStatus: '',
  routeKeyword: '',
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const form = reactive({
  vesselName: '',
  mmsi: '',
  imo: '',
  callSign: '',
  vesselTypeId: undefined as number | undefined,
  flagState: '',
  operatorName: '',
  ownerName: '',
  lengthM: null as number | null,
  widthM: null as number | null,
  draftM: null as number | null,
  grossTonnage: null as number | null,
  deadweightTonnage: null as number | null,
  riskLevel: '',
  navigationStatus: '',
  homePort: '',
  usualRegion: '',
  routeArea: '',
  note: '',
  sourceText: '',
  status: 1,
})

const typeOptions = computed<CascaderOption[]>(() => buildTypeOptions(vesselTypes.value))

function buildTypeOptions(source: VesselTypeOption[]) {
  const nodeMap = new Map<number, CascaderOption & { id: number }>()
  const roots: (CascaderOption & { id: number })[] = []

  source.forEach((item) => {
    nodeMap.set(item.id, {
      id: item.id,
      value: item.id,
      label: item.name,
      children: [],
    })
  })

  source.forEach((item) => {
    const node = nodeMap.get(item.id)
    if (!node) {
      return
    }
    if (item.parentId && nodeMap.has(item.parentId)) {
      ;(nodeMap.get(item.parentId)?.children as CascaderOption[]).push(node)
    } else {
      roots.push(node)
    }
  })

  return roots
}

function resetForm() {
  form.vesselName = ''
  form.mmsi = ''
  form.imo = ''
  form.callSign = ''
  form.vesselTypeId = undefined
  form.flagState = ''
  form.operatorName = ''
  form.ownerName = ''
  form.lengthM = null
  form.widthM = null
  form.draftM = null
  form.grossTonnage = null
  form.deadweightTonnage = null
  form.riskLevel = ''
  form.navigationStatus = ''
  form.homePort = ''
  form.usualRegion = ''
  form.routeArea = ''
  form.note = ''
  form.sourceText = ''
  form.status = 1
  existingImages.value = []
  pendingImageFiles.value = []
  aiSuggestions.value = []
}

function fillForm(detail: VesselDetailView) {
  form.vesselName = detail.vesselName || ''
  form.mmsi = detail.mmsi || ''
  form.imo = detail.imo || ''
  form.callSign = detail.callSign || ''
  form.vesselTypeId = detail.vesselTypeId ?? undefined
  form.flagState = detail.flagState || ''
  form.operatorName = detail.operatorName || ''
  form.ownerName = detail.ownerName || ''
  form.lengthM = detail.lengthM ?? null
  form.widthM = detail.widthM ?? null
  form.draftM = detail.draftM ?? null
  form.grossTonnage = detail.grossTonnage ?? null
  form.deadweightTonnage = detail.deadweightTonnage ?? null
  form.riskLevel = detail.riskLevel || ''
  form.navigationStatus = detail.navigationStatus || ''
  form.homePort = detail.homePort || ''
  form.usualRegion = detail.usualRegion || ''
  form.routeArea = detail.routeArea || ''
  form.note = detail.note || ''
  form.sourceText = detail.sourceText || ''
  form.status = detail.status
  existingImages.value = detail.images || []
  pendingImageFiles.value = []
  aiSuggestions.value = []
}

async function loadTypes() {
  vesselTypes.value = await fetchVesselTypes()
}

async function loadData() {
  if (loading.value) {
    return
  }

  loading.value = true
  try {
    const pageData = await fetchVessels({
      keyword: query.keyword || undefined,
      status: query.status,
      typeId: query.typeId,
      riskLevel: query.riskLevel || undefined,
      navigationStatus: query.navigationStatus || undefined,
      routeKeyword: query.routeKeyword || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    rows.value = pageData.items
    pagination.total = pageData.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '船舶档案加载失败')
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
  query.status = undefined
  query.typeId = undefined
  query.riskLevel = ''
  query.navigationStatus = ''
  query.routeKeyword = ''
  pagination.page = 1
  void loadData()
}

function handleSizeChange() {
  pagination.page = 1
  void loadData()
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openAiCreate() {
  openCreate()
  void runAutocomplete()
}

async function openEdit(id: number) {
  editingId.value = id
  resetForm()
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    fillForm(await fetchVesselDetail(id))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '船舶详情加载失败')
    dialogVisible.value = false
  } finally {
    dialogLoading.value = false
  }
}

function openDetail(id: number) {
  router.push(`/vessels/${id}`)
}

function getTypeName(id?: number) {
  if (!id) {
    return ''
  }
  return vesselTypes.value.find((item) => item.id === id)?.name || ''
}

async function runAutocomplete() {
  aiAutocompleting.value = true
  try {
    const suggestions: string[] = []
    const typeName = getTypeName(form.vesselTypeId)

    if (!form.riskLevel) {
      form.riskLevel = typeName.includes('拖轮') ? '低风险' : '普通关注'
      suggestions.push(`风险等级建议为 ${form.riskLevel}`)
    }
    if (!form.navigationStatus) {
      form.navigationStatus = typeName.includes('拖轮') ? '港内作业' : '在航'
      suggestions.push(`航行状态建议为 ${form.navigationStatus}`)
    }
    if (!form.usualRegion && form.homePort) {
      form.usualRegion = `${form.homePort}及周边水域`
      suggestions.push('已根据母港补全常用区域')
    }
    if (!form.routeArea && form.usualRegion) {
      form.routeArea = `${form.usualRegion}常用航线，建议结合 AIS 历史轨迹进一步核验`
      suggestions.push('已生成航线范围初稿')
    }
    if (!form.sourceText) {
      form.sourceText = '人工维护档案；建议结合 AIS 导入记录、船舶登记信息和 RAG 知识库资料复核。'
      suggestions.push('已补充资料来源说明')
    }
    if (!form.note) {
      const identity = [form.mmsi && `MMSI ${form.mmsi}`, form.imo && `IMO ${form.imo}`].filter(Boolean).join('，')
      form.note = `${form.vesselName || '该船'}${identity ? `（${identity}）` : ''}为船舶主档记录，当前字段由 AI 生成初稿，保存前需人工确认。`
      suggestions.push('已生成备注初稿')
    }

    aiSuggestions.value = suggestions.length ? suggestions : ['当前关键字段已较完整，未发现需要自动补全的空字段']
    ElMessage.success('已生成档案补全建议，请核对后保存')
  } finally {
    aiAutocompleting.value = false
  }
}

function buildPayload(): VesselSavePayload {
  return {
    vesselName: form.vesselName.trim(),
    mmsi: cleanText(form.mmsi),
    imo: cleanText(form.imo),
    callSign: cleanText(form.callSign),
    vesselTypeId: form.vesselTypeId ?? null,
    flagState: cleanText(form.flagState),
    operatorName: cleanText(form.operatorName),
    ownerName: cleanText(form.ownerName),
    lengthM: form.lengthM,
    widthM: form.widthM,
    draftM: form.draftM,
    grossTonnage: form.grossTonnage,
    deadweightTonnage: form.deadweightTonnage,
    riskLevel: cleanText(form.riskLevel),
    navigationStatus: cleanText(form.navigationStatus),
    homePort: cleanText(form.homePort),
    usualRegion: cleanText(form.usualRegion),
    routeArea: cleanText(form.routeArea),
    note: cleanText(form.note),
    sourceText: cleanText(form.sourceText),
    status: form.status,
  }
}

async function submit() {
  if (!form.vesselName.trim()) {
    ElMessage.warning('请填写船名')
    return
  }

  submitting.value = true
  try {
    const payload = buildPayload()
    const detail = editingId.value ? await updateVessel(editingId.value, payload) : await createVessel(payload)
    const files = pendingImageFiles.value.flatMap((item) => (item.raw ? [item.raw] : []))
    for (const file of files) {
      await uploadVesselImage(detail.id, file)
    }
    notifyDataChanged('vessel')
    ElMessage.success(editingId.value ? '船舶档案已更新' : '船舶档案已创建')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '船舶档案保存失败')
  } finally {
    submitting.value = false
  }
}

async function archiveRow(id: number) {
  try {
    await ElMessageBox.confirm('归档后该船舶不会从数据库物理删除，可通过档案状态筛选查看。确认归档吗？', '归档船舶档案', {
      type: 'warning',
      confirmButtonText: '确认归档',
      cancelButtonText: '取消',
    })
    await archiveVessel(id)
    notifyDataChanged('vessel')
    ElMessage.success('船舶档案已归档')
    await loadData()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '船舶档案归档失败')
  }
}

function cleanText(value: string) {
  const trimmed = value.trim()
  return trimmed || undefined
}

function formatIdentity(row: VesselView) {
  return [row.mmsi && `MMSI ${row.mmsi}`, row.imo && `IMO ${row.imo}`].filter(Boolean).join(' / ') || '-'
}

function formatDimensions(row: Pick<VesselView, 'lengthM' | 'widthM' | 'draftM'>) {
  const parts = [row.lengthM && `长 ${row.lengthM}m`, row.widthM && `宽 ${row.widthM}m`, row.draftM && `吃水 ${row.draftM}m`]
  return parts.filter(Boolean).join(' / ') || '-'
}

function riskTagType(value?: string) {
  if (!value) return 'info'
  if ((value || '').includes('高') || (value || '').includes('重点')) return 'danger'
  if ((value || '').includes('中') || (value || '').includes('普通')) return 'warning'
  return 'success'
}

function navigationTagType(value?: string) {
  if (!value || value === '未知') return 'info'
  if ((value || '').includes('异常') || (value || '').includes('失控')) return 'danger'
  if ((value || '').includes('锚泊') || (value || '').includes('靠泊')) return 'warning'
  return 'success'
}

function handleFocus() {
  void loadData()
}

function handleVisibilityChange() {
  if (!document.hidden) {
    void loadData()
  }
}

onMounted(async () => {
  stopDataSync = listenDataChanged((detail) => {
    if (detail.type === 'vessel') {
      void loadData()
    }
  })
  window.addEventListener('focus', handleFocus)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  refreshTimer = window.setInterval(() => {
    if (!document.hidden) {
      void loadData()
    }
  }, 15000)
  await loadTypes()
  await loadData()
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
.vessel-hero {
  align-items: flex-start;
}

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.vessel-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.vessel-metric {
  min-height: 92px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.12), rgba(124, 60, 255, 0.08)),
    rgba(255, 255, 255, 0.06);
  box-shadow: var(--gsmv-shadow-soft);
}

.vessel-metric span {
  display: block;
  color: var(--gsmv-muted);
  font-size: 13px;
}

.vessel-metric strong {
  display: block;
  margin-top: 10px;
  color: #f2fdff;
  font-size: 28px;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.ai-panel {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(0, 229, 255, 0.18);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.12), rgba(124, 60, 255, 0.08)),
    rgba(255, 255, 255, 0.05);
}

.ai-panel strong {
  display: block;
  color: #f2fdff;
  font-size: 17px;
}

.ai-panel p,
.field-tip {
  margin: 8px 0 0;
  color: var(--gsmv-muted);
  line-height: 1.7;
}

.suggestion-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.vessel-form__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px 16px;
}

.vessel-form__grid--numbers {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.vessel-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  width: 100%;
}

.vessel-image-grid__item {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 16px;
  border: 1px solid rgba(0, 229, 255, 0.18);
  overflow: hidden;
}

@media (max-width: 1180px) {
  .vessel-metrics,
  .vessel-form__grid,
  .vessel-form__grid--numbers {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .vessel-metrics,
  .vessel-form__grid,
  .vessel-form__grid--numbers,
  .ai-panel {
    grid-template-columns: 1fr;
  }

  .ai-panel {
    display: grid;
  }
}
</style>
