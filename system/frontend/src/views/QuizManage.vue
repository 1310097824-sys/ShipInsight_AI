<template>
  <div class="quiz-manage">
    <div class="quiz-manage__header">
      <h2>题库管理</h2>
      <el-button type="primary" @click="showCreateDialog">+ 添加题目</el-button>
    </div>

    <!-- Filters -->
    <div class="quiz-manage__filters">
      <el-select v-model="filters.category" placeholder="分类" clearable @change="loadData" style="width: 130px">
        <el-option label="船舶" value="SHIP" />
        <el-option label="天气" value="WEATHER" />
        <el-option label="海域" value="SEA_AREA" />
      </el-select>
      <el-select v-model="filters.type" placeholder="题型" clearable @change="loadData" style="width: 130px">
        <el-option label="单选" value="SINGLE" />
        <el-option label="多选" value="MULTI" />
        <el-option label="判断" value="JUDGE" />
        <el-option label="填空" value="FILL" />
      </el-select>
      <el-select v-model="filters.difficulty" placeholder="难度" clearable @change="loadData" style="width: 130px">
        <el-option label="简单" value="EASY" />
        <el-option label="中等" value="MEDIUM" />
        <el-option label="困难" value="HARD" />
      </el-select>
      <el-input v-model="filters.keyword" placeholder="搜索题目" clearable @clear="loadData" @keyup.enter="loadData" style="width: 220px" />
    </div>

    <!-- Table -->
    <el-table :data="questions" stripe v-loading="loading">
      <el-table-column label="ID" width="60" prop="id" />
      <el-table-column label="分类" width="80">
        <template #default="{ row }">
          <el-tag :type="catTag(row.category)" size="small">{{ catLabel(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="题型" width="80">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="题目" min-width="300" show-overflow-tooltip prop="title" />
      <el-table-column label="难度" width="80">
        <template #default="{ row }">
          <el-tag :type="diffTag(row.difficulty)" size="small" effect="plain">
            {{ diffLabel(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 1"
            @change="toggleStatus(row)"
            size="small"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="editQuestion(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="deleteQuestionAction(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      layout="prev, pager, next"
      @current-change="loadData"
      style="margin-top: 16px; justify-content: center"
    />

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑题目' : '添加题目'"
      width="700px"
      destroy-on-close
    >
      <el-form :model="form" label-width="80px" label-position="left">
        <el-form-item label="分类" required>
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="船舶 SHIP" value="SHIP" />
            <el-option label="天气 WEATHER" value="WEATHER" />
            <el-option label="海域 SEA_AREA" value="SEA_AREA" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型" required>
          <el-select v-model="form.type" @change="onTypeChange" style="width: 100%">
            <el-option label="单选题 SINGLE" value="SINGLE" />
            <el-option label="多选题 MULTI" value="MULTI" />
            <el-option label="判断题 JUDGE" value="JUDGE" />
            <el-option label="填空题 FILL" value="FILL" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" required>
          <el-select v-model="form.difficulty" style="width: 100%">
            <el-option label="简单 EASY" value="EASY" />
            <el-option label="中等 MEDIUM" value="MEDIUM" />
            <el-option label="困难 HARD" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="题目" required>
          <el-input v-model="form.title" type="textarea" :rows="2" placeholder="输入题干" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'JUDGE' && form.type !== 'FILL'" label="选项" required>
          <div style="display: flex; flex-direction: column; gap: 8px; width: 100%">
            <div v-for="(opt, idx) in formOpts" :key="idx" style="display: flex; align-items: center; gap: 8px">
              <span style="font-weight: 700; width: 24px">{{ opt.label }}</span>
              <el-input v-model="opt.text" placeholder="选项文本" />
              <el-button v-if="formOpts.length > 2" link type="danger" @click="removeOption(idx)">×</el-button>
            </div>
            <el-button link type="primary" @click="addOption" v-if="formOpts.length < 6">+ 添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="正确答案" required>
          <el-input v-model="form.answer" :placeholder="answerPlaceholder" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.explanation" type="textarea" :rows="3" placeholder="答案解析（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchQuestions, createQuestion, updateQuestion, deleteQuestion, toggleQuestion } from '@/api/quiz'
import type { QuizQuestion } from '@/types/gsmv'

const questions = ref<QuizQuestion[]>([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = 20

const filters = reactive({
  category: '',
  type: '',
  difficulty: '',
  keyword: '',
})

const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({
  category: 'SHIP',
  type: 'SINGLE',
  difficulty: 'EASY',
  title: '',
  answer: '',
  explanation: '',
})
const formOpts = ref([
  { label: 'A', text: '' },
  { label: 'B', text: '' },
  { label: 'C', text: '' },
  { label: 'D', text: '' },
])

const answerPlaceholder = computed(() => {
  const map: Record<string, string> = {
    SINGLE: '单选题：填入选项字母，如 A',
    MULTI: '多选题：填入选项字母，如 A,B,C',
    JUDGE: '判断题：填入 A（正确）或 B（错误）',
    FILL: '填空题：填入正确答案关键词，多个答案用 | 分隔',
  }
  return map[form.type] || '填入正确答案'
})

function onTypeChange() {
  if (form.type === 'JUDGE') {
    formOpts.value = [
      { label: 'A', text: '正确' },
      { label: 'B', text: '错误' },
    ]
    if (form.answer && !['A', 'B'].includes(form.answer.trim().toUpperCase())) {
      form.answer = ''
    }
  } else if (form.type === 'FILL') {
    formOpts.value = []
    form.answer = ''
  } else if (form.type === 'SINGLE' || form.type === 'MULTI') {
    formOpts.value = [
      { label: 'A', text: '' },
      { label: 'B', text: '' },
      { label: 'C', text: '' },
      { label: 'D', text: '' },
    ]
  }
}

function addOption() {
  const next = String.fromCharCode(65 + formOpts.value.length)
  formOpts.value.push({ label: next, text: '' })
}

function removeOption(idx: number) {
  formOpts.value.splice(idx, 1)
  formOpts.value.forEach((o, i) => {
    o.label = String.fromCharCode(65 + i)
  })
}

async function loadData() {
  loading.value = true
  try {
    const res = await fetchQuestions({
      category: filters.category || undefined,
      type: filters.type || undefined,
      difficulty: filters.difficulty || undefined,
      keyword: filters.keyword || undefined,
      page: page.value,
      size,
    })
    questions.value = res.items
    total.value = res.total
  } catch { /* ignore */ }
  loading.value = false
}

function showCreateDialog() {
  isEditing.value = false
  editingId.value = null
  form.category = 'SHIP'
  form.type = 'SINGLE'
  form.difficulty = 'EASY'
  form.title = ''
  form.answer = ''
  form.explanation = ''
  formOpts.value = [
    { label: 'A', text: '' },
    { label: 'B', text: '' },
    { label: 'C', text: '' },
    { label: 'D', text: '' },
  ]
  dialogVisible.value = true
}

function editQuestion(q: QuizQuestion) {
  isEditing.value = true
  editingId.value = q.id
  form.category = q.category
  form.type = q.type
  form.difficulty = q.difficulty
  form.title = q.title
  form.answer = q.answer
  form.explanation = q.explanation || ''
  try {
    formOpts.value = JSON.parse(q.options)
  } catch {
    formOpts.value = []
  }
  dialogVisible.value = true
}

async function saveQuestion() {
  if (!form.title.trim()) { ElMessage.warning('请输入题目'); return }
  if (!form.answer.trim()) { ElMessage.warning('请输入答案'); return }

  saving.value = true
  try {
    const payload = {
      category: form.category,
      type: form.type,
      difficulty: form.difficulty,
      title: form.title.trim(),
      options: form.type === 'FILL' ? '[]' : JSON.stringify(formOpts.value),
      answer: form.answer.trim(),
      explanation: form.explanation.trim() || null,
    }

    if (isEditing.value && editingId.value) {
      await updateQuestion(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createQuestion(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

async function deleteQuestionAction(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该题目吗？', '删除确认', { type: 'warning' })
    await deleteQuestion(id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancelled */ }
}

async function toggleStatus(row: QuizQuestion) {
  try {
    await toggleQuestion(row.id)
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch { /* ignore */ }
}

// Labels
function catLabel(c: string) {
  const map: Record<string, string> = { SHIP: '船舶', WEATHER: '天气', SEA_AREA: '海域' }
  return map[c] || c
}
function catTag(c: string) {
  const map: Record<string, string> = { SHIP: 'primary', WEATHER: 'success', SEA_AREA: 'warning' }
  return (map[c] || 'info') as 'primary' | 'success' | 'warning' | 'info'
}
function typeLabel(t: string) {
  const map: Record<string, string> = { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空' }
  return map[t] || t
}
function typeTag(t: string) {
  const map: Record<string, string> = { SINGLE: '', MULTI: 'danger', JUDGE: 'info', FILL: 'warning' }
  return (map[t] || 'info') as '' | 'danger' | 'info' | 'warning'
}
function diffLabel(d: string) {
  const map: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[d] || d
}
function diffTag(d: string) {
  const map: Record<string, string> = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return (map[d] || 'info') as 'success' | 'warning' | 'danger' | 'info'
}

onMounted(loadData)
</script>

<style scoped>
.quiz-manage {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
}
.quiz-manage__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.quiz-manage__header h2 { margin: 0; }
.quiz-manage__filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
</style>
