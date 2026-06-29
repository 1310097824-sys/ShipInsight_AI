<template>
  <div class="roles-page page-shell">
    <section class="roles-hero">
      <div class="roles-hero__content">
        <span class="roles-hero__eyebrow">Access Matrix</span>
        <h2>角色管理</h2>
        <p>集中维护系统角色、权限矩阵和账号覆盖范围，确保每类航运业务用户只看到需要的工作区。</p>
      </div>

      <div class="roles-hero__stats">
        <div class="roles-stat">
          <span>角色总数</span>
          <strong>{{ managedRoleCount }}</strong>
        </div>
        <div class="roles-stat">
          <span>权限节点</span>
          <strong>{{ permissionTotal }}</strong>
        </div>
        <div class="roles-stat">
          <span>已分配用户</span>
          <strong>{{ assignedUserTotal }}</strong>
        </div>
      </div>

      <el-button type="primary" class="roles-hero__action" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增角色
      </el-button>
    </section>

    <section class="roles-control-panel">
      <div class="roles-control-panel__search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索角色代码或名称"
          clearable
          @keyup.enter="loadRoles"
          @clear="loadRoles"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button plain @click="loadRoles">搜索</el-button>
      </div>
      <div class="roles-control-panel__hint">
        <span />
        角色管理仅系统管理员可见
      </div>
    </section>

    <section class="roles-table-panel">
      <header class="roles-table-panel__header">
        <div>
          <span>Role Directory</span>
          <strong>权限角色清单</strong>
        </div>
        <small>按角色代码排序，保留系统角色的删除保护</small>
      </header>

      <el-table
        v-loading="loading"
        :data="roles"
        row-key="id"
        class="roles-table"
        style="width: 100%"
      >
        <el-table-column label="角色代码" min-width="150">
          <template #default="{ row }">
            <div class="role-code-cell">
              <span class="role-code-cell__pulse" :class="roleTone(row)" />
              <strong>{{ row.code }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column label="描述" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="role-description">{{ row.description || '未填写描述' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="96" align="center">
          <template #default="{ row }">
            <span class="metric-pill metric-pill--permission">
              <el-icon><Key /></el-icon>
              {{ row.permissions?.length ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="用户数" width="96" align="center">
          <template #default="{ row }">
            <span class="metric-pill" :class="row.userCount > 0 ? 'metric-pill--active' : 'metric-pill--empty'">
              <el-icon><UserFilled /></el-icon>
              {{ row.userCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">
            <span class="created-time">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="118">
          <template #default="{ row }">
            <div class="table-actions">
              <el-tooltip content="编辑角色" placement="top">
                <el-button size="small" plain circle @click="openEdit(row)">
                  <el-icon><EditPen /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip :content="deleteDisabledReason(row) || '删除角色'" placement="top">
                <span>
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    circle
                    :disabled="!canDeleteRole(row)"
                    @click="confirmDelete(row)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadRoles"
          @current-change="loadRoles"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingRole ? '编辑角色' : '新增角色'"
      width="760px"
      :close-on-click-modal="false"
      class="role-dialog"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <div class="role-form-grid">
          <el-form-item label="角色代码" prop="code">
            <el-input v-model="form.code" :disabled="!!editingRole" placeholder="大写字母+下划线，例如 CONTROLLER" />
          </el-form-item>
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="form.name" placeholder="例如：交通管制员" />
          </el-form-item>
        </div>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="补充该角色负责的业务范围" />
        </el-form-item>
        <el-form-item label="权限分配">
          <div class="permission-grid">
            <template v-for="cat in permissionCategories" :key="cat.category">
              <div class="perm-category">
                <div class="perm-cat-header">
                  <el-checkbox
                    :model-value="isCategoryAllChecked(cat)"
                    :indeterminate="isCategoryIndeterminate(cat)"
                    @change="toggleCategory(cat, toChecked($event))"
                  >
                    {{ cat.category }}
                  </el-checkbox>
                  <span>{{ selectedCount(cat) }}/{{ cat.items.length }}</span>
                </div>
                <div class="perm-items">
                  <el-checkbox
                    v-for="p in cat.items"
                    :key="p.code"
                    :model-value="form.permissionIds.includes(p.id)"
                    @change="togglePermission(p, toChecked($event))"
                  >
                    <span class="perm-label">{{ p.label }}</span>
                    <span class="perm-route">{{ p.route }}</span>
                  </el-checkbox>
                </div>
              </div>
            </template>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Delete, EditPen, Key, Plus, Search, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createRole, deleteRole, fetchPermissions, fetchRoles, updateRole } from '@/api/roles'
import type { PermissionOption, RoleDetail } from '@/types/gsmv'

const loading = ref(false)
const saving = ref(false)
const roles = ref<RoleDetail[]>([])
const allPermissions = ref<PermissionOption[]>([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const editingRole = ref<RoleDetail | null>(null)
const formRef = ref<FormInstance>()
const form = ref({
  code: '',
  name: '',
  description: '',
  permissionIds: [] as number[],
})

const formRules: FormRules = {
  code: [
    { required: true, message: '请输入角色代码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '大写字母开头，仅支持大写字母、数字和下划线', trigger: 'blur' },
  ],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

const ROUTE_MAP: { category: string; items: { code: string; route: string; label: string }[] }[] = [
  {
    category: '态势感知',
    items: [
      { code: 'REPORT_READ', route: '/dashboard, /reports', label: '态势总览与报表' },
      { code: 'OBS_READ', route: '/route-map, /observations', label: '航线地图与 AIS 记录' },
      { code: 'AUDIT_READ', route: '/audits', label: '审计日志' },
    ],
  },
  {
    category: '船舶管理',
    items: [
      { code: 'VESSEL_READ', route: '/vessels', label: '查看船舶档案' },
      { code: 'VESSEL_WRITE', route: '/vessels', label: '维护船舶档案' },
      { code: 'MEDIA_READ', route: '附件', label: '查看附件' },
      { code: 'MEDIA_WRITE', route: '附件', label: '上传附件' },
    ],
  },
  {
    category: 'AI 智能分析',
    items: [
      { code: 'RAG_READ', route: '/rag-knowledge', label: '查看知识库' },
      { code: 'RAG_MANAGE', route: '/rag-knowledge', label: '管理知识库' },
      { code: 'AI_REVIEW_READ', route: '/ai-reviews', label: '查看 AI 复核' },
      { code: 'AI_REVIEW_WRITE', route: '/ai-reviews', label: '处理 AI 复核' },
    ],
  },
  {
    category: '知识问答',
    items: [
      { code: 'QUIZ_READ', route: '/quiz', label: '使用知识问答' },
      { code: 'QUIZ_WRITE', route: '/quiz/manage', label: '管理题库' },
    ],
  },
  {
    category: '系统管理',
    items: [
      { code: 'USER_ADMIN', route: '/users, /roles', label: '用户与角色管理' },
    ],
  },
]

const permissionCategories = computed(() => {
  return ROUTE_MAP.map((cat) => ({
    category: cat.category,
    items: cat.items.map((mapping) => {
      const perm = allPermissions.value.find((p) => p.code === mapping.code)
      return {
        id: perm?.id ?? 0,
        code: mapping.code,
        label: mapping.label,
        route: mapping.route,
      }
    }).filter((item) => item.id > 0),
  })).filter((cat) => cat.items.length > 0)
})

const managedRoleCount = computed(() => total.value || roles.value.length)
const permissionTotal = computed(() => allPermissions.value.length)
const assignedUserTotal = computed(() => roles.value.reduce((sum, role) => sum + (role.userCount || 0), 0))

async function loadRoles() {
  loading.value = true
  try {
    const data = await fetchRoles({
      keyword: searchKeyword.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    roles.value = data.items
    total.value = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '角色列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadPermissions() {
  try {
    allPermissions.value = await fetchPermissions()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '权限列表加载失败')
  }
}

function openCreate() {
  editingRole.value = null
  form.value = { code: '', name: '', description: '', permissionIds: [] }
  dialogVisible.value = true
}

function openEdit(role: RoleDetail) {
  editingRole.value = role
  form.value = {
    code: role.code,
    name: role.name,
    description: role.description ?? '',
    permissionIds: role.permissions?.map((p) => p.id) ?? [],
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingRole.value) {
      await updateRole(editingRole.value.id, {
        name: form.value.name,
        description: form.value.description || undefined,
        permissionIds: form.value.permissionIds,
      })
      ElMessage.success('角色已更新')
    } else {
      await createRole({
        code: form.value.code,
        name: form.value.name,
        description: form.value.description || undefined,
        permissionIds: form.value.permissionIds,
      })
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    await loadRoles()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存角色失败')
  } finally {
    saving.value = false
  }
}

async function confirmDelete(role: RoleDetail) {
  try {
    await ElMessageBox.confirm(`确定要删除角色“${role.name}”吗？此操作不可撤销。`, '删除角色', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
    })
    await deleteRole(role.id)
    ElMessage.success('角色已删除')
    await loadRoles()
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message)
    }
  }
}

function canDeleteRole(role: RoleDetail) {
  return role.code !== 'ADMIN' && role.userCount <= 0
}

function deleteDisabledReason(role: RoleDetail) {
  if (role.code === 'ADMIN') return '系统管理员角色不可删除'
  if (role.userCount > 0) return '该角色仍有用户使用'
  return ''
}

function roleTone(role: RoleDetail) {
  if (role.code === 'ADMIN') return 'is-admin'
  if (role.userCount > 0) return 'is-active'
  return 'is-idle'
}

function selectedCount(cat: (typeof permissionCategories.value)[number]) {
  return cat.items.filter((p) => form.value.permissionIds.includes(p.id)).length
}

function isCategoryAllChecked(cat: (typeof permissionCategories.value)[number]) {
  return cat.items.length > 0 && cat.items.every((p) => form.value.permissionIds.includes(p.id))
}

function isCategoryIndeterminate(cat: (typeof permissionCategories.value)[number]) {
  const checked = selectedCount(cat)
  return checked > 0 && checked < cat.items.length
}

function toggleCategory(
  cat: (typeof permissionCategories.value)[number],
  checked: boolean,
) {
  const ids = cat.items.map((p) => p.id)
  if (checked) {
    ids.forEach((id) => {
      if (!form.value.permissionIds.includes(id)) form.value.permissionIds.push(id)
    })
  } else {
    form.value.permissionIds = form.value.permissionIds.filter((id) => !ids.includes(id))
  }
}

function togglePermission(
  p: (typeof permissionCategories.value)[number]['items'][number],
  checked: boolean,
) {
  if (checked) {
    if (!form.value.permissionIds.includes(p.id)) form.value.permissionIds.push(p.id)
  } else {
    form.value.permissionIds = form.value.permissionIds.filter((id) => id !== p.id)
  }
}

function toChecked(value: string | number | boolean) {
  return value === true
}

function formatDate(value: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

onMounted(async () => {
  await Promise.all([loadRoles(), loadPermissions()])
})
</script>

<style scoped>
.roles-page {
  gap: 18px;
}

.roles-hero,
.roles-control-panel,
.roles-table-panel {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.13), rgba(124, 60, 255, 0.1) 46%, rgba(32, 255, 159, 0.04)),
    rgba(255, 255, 255, 0.06);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 0 28px rgba(0, 229, 255, 0.12),
    0 20px 54px rgba(0, 4, 18, 0.34);
  backdrop-filter: blur(24px);
  overflow: hidden;
}

.roles-hero::after,
.roles-table-panel::after {
  content: "";
  position: absolute;
  inset: auto -12% -54% 48%;
  height: 88%;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.17), transparent 72%);
  pointer-events: none;
}

.roles-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 20px;
  align-items: center;
  min-height: 156px;
  padding: 28px 30px;
  border-radius: 28px;
}

.roles-hero__content,
.roles-hero__stats,
.roles-hero__action {
  position: relative;
  z-index: 1;
}

.roles-hero__eyebrow,
.roles-table-panel__header span {
  display: inline-flex;
  width: fit-content;
  color: var(--gsmv-primary);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.roles-hero h2 {
  margin: 12px 0 10px;
  color: transparent;
  background: linear-gradient(92deg, #ffffff 0%, var(--gsmv-primary) 54%, var(--gsmv-warm) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.04;
}

.roles-hero p {
  max-width: 680px;
  margin: 0;
  color: rgba(232, 243, 255, 0.68);
  line-height: 1.72;
}

.roles-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(92px, 1fr));
  gap: 12px;
}

.roles-stat {
  min-width: 104px;
  padding: 15px 16px;
  border: 1px solid rgba(0, 229, 255, 0.18);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.035)),
    rgba(4, 14, 36, 0.42);
}

.roles-stat span {
  display: block;
  color: rgba(232, 243, 255, 0.6);
  font-size: 12px;
  font-weight: 700;
}

.roles-stat strong {
  display: block;
  margin-top: 8px;
  color: #f7fcff;
  font-size: 28px;
  line-height: 1;
}

.roles-hero__action {
  min-width: 132px;
  height: 48px;
}

.roles-control-panel {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 16px;
  border-radius: 24px;
}

.roles-control-panel__search {
  display: flex;
  gap: 12px;
  align-items: center;
  width: min(520px, 100%);
}

.roles-control-panel__search :deep(.el-input) {
  flex: 1;
}

.roles-control-panel__hint {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: rgba(232, 243, 255, 0.66);
  font-size: 13px;
  font-weight: 700;
}

.roles-control-panel__hint span {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--gsmv-accent);
  box-shadow: 0 0 16px rgba(32, 255, 159, 0.48);
}

.roles-table-panel {
  border-radius: 26px;
}

.roles-table-panel__header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(180deg, rgba(0, 229, 255, 0.08), transparent);
}

.roles-table-panel__header strong {
  display: block;
  margin-top: 6px;
  color: #f4fbff;
  font-size: 18px;
}

.roles-table-panel__header small {
  color: rgba(232, 243, 255, 0.58);
  font-size: 13px;
}

.roles-table {
  position: relative;
  z-index: 1;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(13, 31, 74, 0.72);
  --el-table-row-hover-bg-color: rgba(0, 229, 255, 0.08);
  --el-table-border-color: rgba(255, 255, 255, 0.08);
  color: var(--gsmv-text);
}

.roles-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.roles-table :deep(th.el-table__cell) {
  height: 48px;
  color: rgba(232, 243, 255, 0.84);
  font-weight: 800;
}

.roles-table :deep(td.el-table__cell) {
  height: 58px;
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.roles-table :deep(.el-table__row:nth-child(even)) {
  background: rgba(124, 60, 255, 0.07);
}

.role-code-cell {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.role-code-cell__pulse {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--gsmv-primary);
  box-shadow: 0 0 16px rgba(0, 229, 255, 0.5);
}

.role-code-cell__pulse.is-admin {
  background: var(--gsmv-warm);
  box-shadow: 0 0 18px rgba(255, 79, 216, 0.52);
}

.role-code-cell__pulse.is-active {
  background: var(--gsmv-accent);
  box-shadow: 0 0 16px rgba(32, 255, 159, 0.5);
}

.role-code-cell__pulse.is-idle {
  opacity: 0.6;
}

.role-code-cell strong {
  color: #f4fbff;
  font-size: 13px;
  letter-spacing: 0.04em;
}

.role-description,
.created-time {
  color: rgba(232, 243, 255, 0.74);
}

.metric-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 58px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  color: #f5fbff;
  font-size: 13px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.08);
}

.metric-pill--permission {
  border-color: rgba(0, 229, 255, 0.22);
  background: rgba(0, 229, 255, 0.12);
}

.metric-pill--active {
  border-color: rgba(32, 255, 159, 0.24);
  background: rgba(32, 255, 159, 0.1);
}

.metric-pill--empty {
  color: rgba(232, 243, 255, 0.58);
}

.table-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination-wrap {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: flex-end;
  padding: 16px 18px 18px;
}

.role-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.permission-grid {
  width: 100%;
  display: grid;
  gap: 12px;
}

.perm-category {
  border: 1px solid rgba(0, 229, 255, 0.16);
  border-radius: 18px;
  padding: 12px 14px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.08), rgba(124, 60, 255, 0.06)),
    rgba(255, 255, 255, 0.045);
}

.perm-cat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-weight: 800;
}

.perm-cat-header span {
  color: rgba(232, 243, 255, 0.58);
  font-size: 12px;
}

.perm-items {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
}

.perm-items :deep(.el-checkbox) {
  align-items: flex-start;
  min-height: 34px;
  margin-right: 0;
}

.perm-label {
  display: block;
  color: #f3fbff;
  font-size: 13px;
}

.perm-route {
  display: block;
  margin-top: 3px;
  color: rgba(232, 243, 255, 0.48);
  font-size: 11px;
}

@media (max-width: 1180px) {
  .roles-hero {
    grid-template-columns: 1fr;
  }

  .roles-hero__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .roles-hero__action {
    width: fit-content;
  }
}

@media (max-width: 760px) {
  .roles-hero,
  .roles-control-panel,
  .roles-table-panel__header {
    align-items: stretch;
    flex-direction: column;
  }

  .roles-control-panel {
    display: grid;
  }

  .roles-control-panel__search,
  .roles-hero__action {
    width: 100%;
  }

  .roles-hero__stats,
  .role-form-grid,
  .perm-items {
    grid-template-columns: 1fr;
  }
}
</style>
