<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <h2>航运网络</h2>
        <p>管理港口、锚地、主航道、外海航线和重点水域等航运节点，为 AIS 记录、航线地图和态势分析提供基础底座。</p>
      </div>
      <el-button v-if="canWrite" type="primary" @click="openCreate">新增航运节点</el-button>
    </section>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar toolbar--wrap">
        <el-input v-model="query.keyword" placeholder="名称 / 类型" clearable style="max-width: 220px" />
        <el-select v-model="query.type" placeholder="航运节点类型" clearable style="width: 180px">
          <el-option v-for="item in shippingZoneTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="type" label="类型" min-width="140">
          <template #default="{ row }">
            {{ shippingZoneTypeLabelMap[row.type] || row.type || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="320" show-overflow-tooltip />
        <el-table-column label="操作" fixed="right" :width="canWrite ? 180 : 90">
          <template #default="{ row }">
            <el-space>
              <el-button v-if="canWrite" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="canWrite" link type="danger" @click="removeShippingZone(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          layout="total, prev, pager, next"
          :total="pagination.total"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑航运节点' : '新增航运节点'" width="560px">
      <el-form label-position="top">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" clearable style="width: 100%" placeholder="请选择航运节点类型">
            <el-option v-for="item in shippingZoneTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createShippingZone, deleteShippingZone, fetchShippingZones, updateShippingZone } from '@/api/ecosystems'
import { useAuthStore } from '@/stores/auth'
import { notifyDataChanged } from '@/utils/dataSync'
import type { ShippingZone } from '@/types/gsmv'

const shippingZoneTypeOptions = [
  { label: '近海航区', value: 'OFFSHORE' },
  { label: '主航道', value: 'REEF' },
  { label: '锚地', value: 'MANGROVE' },
  { label: '港口', value: 'SEAGRASS' },
  { label: '外海航线', value: 'DEEP_SEA' },
  { label: '河口航道', value: 'ESTUARY' },
  { label: '湾区水域', value: 'BAY' },
  { label: '管制水域', value: 'WETLAND' },
]

const shippingZoneTypeLabelMap = Object.fromEntries(shippingZoneTypeOptions.map((item) => [item.value, item.label]))

const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<ShippingZone[]>([])

const canWrite = computed(() => (authStore.authorities || []).includes('ECOSYSTEM_WRITE'))

const query = reactive({
  keyword: '',
  type: '',
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const form = reactive({
  name: '',
  type: '',
  description: '',
})

function resetForm() {
  form.name = ''
  form.type = ''
  form.description = ''
}

async function loadData() {
  loading.value = true
  try {
    const pageData = await fetchShippingZones({
      keyword: query.keyword || undefined,
      type: query.type || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    rows.value = pageData.items
    pagination.total = pageData.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '航运网络加载失败')
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
  query.type = ''
  pagination.page = 1
  void loadData()
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ShippingZone) {
  editingId.value = row.id
  form.name = row.name
  form.type = row.type || ''
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入航运节点名称')
    return
  }

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      type: form.type || undefined,
      description: form.description.trim() || undefined,
    }

    if (editingId.value) {
      await updateShippingZone(editingId.value, payload)
      ElMessage.success('航运节点已更新')
    } else {
      await createShippingZone(payload)
      ElMessage.success('航运节点已创建')
    }

    notifyDataChanged('shippingZone')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

async function removeShippingZone(row: ShippingZone) {
  try {
    await ElMessageBox.confirm(
      `删除后将移除航运节点“${row.name}”的档案信息。若已有 AIS 记录引用它，系统会阻止删除。是否继续？`,
      '删除航运节点',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteShippingZone(row.id)
    notifyDataChanged('shippingZone')
    ElMessage.success('航运节点已删除')
    await loadData()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped>
.toolbar--wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
