<template>
  <div class="page-shell">
    <!-- Header -->
    <section class="page-hero quiz-exam-hero">
      <div class="exam-header__info">
        <el-button link @click="goBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <span class="exam-header__progress">
          第 {{ currentIndex + 1 }} / {{ questions.length }} 题
        </span>
      </div>
      <div class="exam-header__timer" v-if="!submitted">
        <el-icon><Timer /></el-icon>
        {{ formattedTime }}
      </div>
    </section>

    <!-- Submitted Result -->
    <el-card v-if="submitted" class="panel-card" shadow="never">
      <div class="result-card">
        <div class="result-card__score">
          <strong>{{ result?.score }}</strong>
          <span>/ {{ result?.total }}</span>
        </div>
        <div class="result-card__grade" :class="'grade--' + gradeKey">{{ result?.grade }}</div>
        <div class="result-card__pct">{{ Math.round(((result?.score || 0) / (result?.total || 1)) * 100) }}%</div>
        <el-button type="primary" @click="viewDetail" style="margin-top: 12px">
          查看详情与解析
        </el-button>
      </div>
    </el-card>

    <!-- Question Card -->
    <el-card v-if="!submitted && currentQuestion" class="panel-card" shadow="never">
      <template #header>
        <div class="panel-header">
          <strong>答题区</strong>
          <div class="question-card__meta">
            <el-tag :type="catTagType(currentQuestion.category)" size="small">
              {{ catLabel(currentQuestion.category) }}
            </el-tag>
            <el-tag :type="typeTagType(currentQuestion.type)" size="small">
              {{ typeLabel(currentQuestion.type) }}
            </el-tag>
            <el-tag :type="diffTagType(currentQuestion.difficulty)" size="small" effect="plain">
              {{ diffLabel(currentQuestion.difficulty) }}
            </el-tag>
          </div>
        </div>
      </template>

      <!-- Title -->
      <div class="question-card__title">
        <span class="question-card__num">{{ currentIndex + 1 }}.</span>
        {{ currentQuestion.title }}
      </div>

      <!-- Options / Fill-in input -->
      <div class="question-card__options" v-if="currentQuestion.type !== 'FILL'">
        <div
          v-for="opt in parsedOptions"
          :key="opt.label"
          class="option-item"
          :class="{
            'is-selected': isOptionSelected(opt.label),
            'option--single': currentQuestion.type === 'SINGLE',
            'option--multi':  currentQuestion.type === 'MULTI',
            'option--judge':  currentQuestion.type === 'JUDGE',
          }"
          @click="toggleOption(opt.label)"
        >
          <span class="option-item__label">{{ opt.label }}</span>
          <span class="option-item__text">{{ opt.text }}</span>
        </div>
      </div>
      <div class="question-card__fill" v-else>
        <div v-if="fillBlankCount > 1" class="fill-hints">
          <el-alert type="info" :closable="false" show-icon>
            本题有 <strong>{{ fillBlankCount }}</strong> 个空，请依次填写，答案间用 <code>|</code> 分隔（如：<code>答案1|答案2</code>）
          </el-alert>
        </div>
        <div class="fill-inputs">
          <div v-for="(_, idx) in fillBlankCount" :key="idx" class="fill-input-row">
            <span class="fill-input-label">第{{ idx + 1 }}空：</span>
            <el-input
              v-model="fillAnswers[idx]"
              :placeholder="'请输入第' + (idx + 1) + '空的答案'"
              size="large"
              clearable
              @input="onFillInput"
              class="fill-input"
            />
          </div>
        </div>
        <div v-if="fillBlankCount > 1" class="fill-preview">
          <span class="fill-preview__label">预览提交值：</span>
          <code class="fill-preview__value">{{ fillAnswersPreview }}</code>
        </div>
      </div>

      <!-- Navigation -->
      <div class="question-card__nav">
        <el-button @click="prev" :disabled="currentIndex === 0" plain>上一题</el-button>
        <div class="question-card__dots">
          <span
            v-for="(_, i) in questions"
            :key="i"
            class="question-dot"
            :class="{ 'is-current': i === currentIndex, 'is-answered': userAnswers.has(questions[i].id) }"
            @click="currentIndex = i"
          />
        </div>
        <el-button
          v-if="currentIndex < questions.length - 1"
          type="primary"
          @click="next"
        >
          下一题
        </el-button>
        <el-button v-else type="success" @click="submitExamAction" :loading="submitting">
          提交答卷
        </el-button>
      </div>
    </el-card>

    <!-- Question Overview -->
    <el-card v-if="!submitted" class="panel-card" shadow="never">
      <template #header>
        <div class="panel-header">
          <strong>答题卡</strong>
          <span class="overview-count">已答 {{ answeredCount }} / {{ questions.length }}</span>
        </div>
      </template>
      <div class="question-overview__grid">
        <span
          v-for="(q, i) in questions"
          :key="q.id"
          class="overview-dot"
          :class="{ 'is-answered': userAnswers.has(q.id), 'is-current': i === currentIndex }"
          @click="currentIndex = i"
        >
          {{ i + 1 }}
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Timer } from '@element-plus/icons-vue'
import { submitExam } from '@/api/quiz'
import type { QuizResultResponse } from '@/api/quiz'
import type { QuizQuestion } from '@/types/gsmv'

const route = useRoute()
const router = useRouter()

const recordId = ref(Number(route.query.recordId) || 0)
const questions = ref<QuizQuestion[]>([])
const currentIndex = ref(0)
const userAnswers = ref<Map<number, string>>(new Map())
const fillAnswers = ref<string[]>([])
const submitted = ref(false)
const submitting = ref(false)
const result = ref<QuizResultResponse | null>(null)

// Timer
const startTime = Date.now()
const elapsed = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const formattedTime = computed(() => {
  const s = Math.floor(elapsed.value / 1000)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`
})

const answeredCount = computed(() => userAnswers.value.size)

// Parse stored options
function parseStoredOptions(q: QuizQuestion): { label: string; text: string }[] {
  try {
    return JSON.parse(q.options)
  } catch {
    return []
  }
}

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

const fillBlankCount = computed(() => {
  if (!currentQuestion.value || currentQuestion.value.type !== 'FILL') return 1
  const ans = currentQuestion.value.answer || ''
  return ans.split('|').length || 1
})

const fillAnswersPreview = computed(() => {
  return fillAnswers.value.filter(Boolean).join('|') || '（未填写）'
})

const parsedOptions = computed(() =>
  currentQuestion.value ? parseStoredOptions(currentQuestion.value) : [],
)

function isOptionSelected(label: string): boolean {
  if (!currentQuestion.value) return false
  const ans = userAnswers.value.get(currentQuestion.value.id)
  if (!ans) return false
  if (currentQuestion.value.type === 'SINGLE' || currentQuestion.value.type === 'JUDGE') {
    return ans === label
  }
  return ans.split(',').includes(label)
}

function toggleOption(label: string) {
  if (!currentQuestion.value) return
  const q = currentQuestion.value
  if (q.type === 'SINGLE' || q.type === 'JUDGE') {
    userAnswers.value.set(q.id, label)
    setTimeout(() => {
      if (currentIndex.value < questions.value.length - 1) {
        next()
      }
    }, 400)
  } else {
    // MULTI
    const cur = userAnswers.value.get(q.id) || ''
    const labels = cur ? cur.split(',') : []
    if (labels.includes(label)) {
      userAnswers.value.set(q.id, labels.filter((l: string) => l !== label).join(','))
    } else {
      userAnswers.value.set(q.id, [...labels, label].sort().join(','))
    }
  }
}

function onFillInput() {
  if (!currentQuestion.value) return
  const q = currentQuestion.value
  if (q.type === 'FILL') {
    // 将 fillAnswers 数组用 | 连接后存入 userAnswers
    const joined = fillAnswers.value.filter(Boolean).join('|')
    userAnswers.value.set(q.id, joined)
  }
}

function loadFillAnswer() {
  if (!currentQuestion.value) return
  const q = currentQuestion.value
  if (q.type === 'FILL') {
    const stored = userAnswers.value.get(q.id) || ''
    // 将存储的答案按 | 分割，填充到 fillAnswers 数组
    fillAnswers.value = stored ? stored.split('|') : []
    // 确保数组长度等于空的个数
    while (fillAnswers.value.length < fillBlankCount.value) {
      fillAnswers.value.push('')
    }
  } else {
    fillAnswers.value = []
  }
}

function prev() {
  if (currentIndex.value > 0) currentIndex.value--
}

function next() {
  if (currentIndex.value < questions.value.length - 1) currentIndex.value++
}

async function submitExamAction() {
  const unanswered = questions.value.filter((q) => !userAnswers.value.has(q.id))
  if (unanswered.length > 0) {
    try {
      await ElMessageBox.confirm(
        `还有 ${unanswered.length} 题未作答，确定提交吗？`,
        '确认提交',
        { confirmButtonText: '确定提交', cancelButtonText: '继续答题', type: 'warning' },
      )
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    const answers = Array.from(userAnswers.value.entries()).map(([qid, ans]) => ({
      questionId: qid,
      userAnswer: ans,
    }))
    result.value = await submitExam({ recordId: recordId.value, answers })
    submitted.value = true
    ElMessage.success('答卷提交成功！')
  } catch {
    ElMessage.error('提交失败')
  }
  submitting.value = false
}

function viewDetail() {
  router.push({ path: '/quiz/result', query: { recordId: recordId.value } })
}

function goBack() {
  router.push('/quiz')
}

// Labels
function catLabel(c: string) {
  const map: Record<string, string> = { SHIP: '船舶', WEATHER: '天气', SEA_AREA: '海域' }
  return map[c] || c
}
function catTagType(c: string) {
  const map: Record<string, string> = { SHIP: 'primary', WEATHER: 'success', SEA_AREA: 'warning' }
  return (map[c] || 'info') as 'primary' | 'success' | 'warning' | 'info'
}
function typeLabel(t: string) {
  const map: Record<string, string> = { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空' }
  return map[t] || t
}
function typeTagType(t: string) {
  const map: Record<string, string> = { SINGLE: '', MULTI: 'danger', JUDGE: 'info', FILL: 'warning' }
  return (map[t] || 'info') as '' | 'danger' | 'info' | 'warning'
}
function diffLabel(d: string) {
  const map: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[d] || d
}
function diffTagType(d: string) {
  const map: Record<string, string> = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return (map[d] || 'info') as 'success' | 'warning' | 'danger' | 'info'
}

const gradeKey = computed(() => {
  if (!result.value) return 'fail'
  const pct = result.value.score / result.value.total
  if (pct >= 0.9) return 'excellent'
  if (pct >= 0.75) return 'good'
  if (pct >= 0.6) return 'pass'
  return 'fail'
})

onMounted(() => {
  timer = setInterval(() => {
    elapsed.value = Date.now() - startTime
  }, 1000)

  // Watch question navigation to load/save fill-in answer
  watch(currentIndex, () => {
    loadFillAnswer()
  })

  // Initial load
  loadFillAnswer()

  if (!recordId.value) {
    router.push('/quiz')
    return
  }

  try {
    const stored = sessionStorage.getItem(`quiz_${recordId.value}`)
    if (stored) {
      questions.value = JSON.parse(stored)
      // Don't clear session storage until submit, so user can refresh
      return
    }
    ElMessage.warning('答题会话已过期，请重新开始')
    router.push('/quiz')
  } catch {
    ElMessage.error('无法加载题目')
    router.push('/quiz')
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ===== Exam Hero ===== */
.quiz-exam-hero {
  align-items: center;
  padding: 18px 28px;
}
.exam-header__info {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}
.exam-header__progress {
  font-weight: 600;
  color: var(--gsmv-text);
}
.exam-header__timer {
  position: relative;
  z-index: 1;
  font-size: 18px;
  font-weight: 700;
  color: var(--gsmv-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== Panel Header ===== */
.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}
.panel-header strong {
  font-size: 16px;
  letter-spacing: 0.01em;
}
.overview-count {
  color: var(--gsmv-muted);
  font-size: 13px;
}
.question-card__meta {
  display: flex;
  gap: 8px;
}

/* ===== Result Card ===== */
.result-card {
  text-align: center;
  padding: 24px;
}
.result-card__score strong {
  font-size: 48px;
  color: var(--gsmv-primary);
  text-shadow: 0 0 24px rgba(0, 229, 255, 0.4);
}
.result-card__score span {
  font-size: 20px;
  color: var(--gsmv-muted);
}
.result-card__grade {
  font-size: 22px;
  font-weight: 700;
  margin: 8px 0;
}
.result-card__pct {
  font-size: 18px;
  color: var(--gsmv-muted);
  margin-bottom: 8px;
}
.grade--excellent { color: #4ff0b5; }
.grade--good { color: var(--gsmv-primary); }
.grade--pass { color: #ffc857; }
.grade--fail { color: var(--gsmv-danger); }

/* ===== Question Card ===== */
.question-card__title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.7;
  color: var(--gsmv-text);
  margin-bottom: 24px;
}
.question-card__num {
  color: var(--gsmv-primary);
  margin-right: 4px;
}
.question-card__options { display: flex; flex-direction: column; gap: 12px; }
.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid rgba(79, 240, 181, 0.2);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: rgba(255, 255, 255, 0.03);
}
.option-item:hover {
  border-color: rgba(0, 229, 255, 0.6);
  background: rgba(0, 229, 255, 0.08);
}
.option-item.is-selected {
  border-color: var(--gsmv-primary);
  background: rgba(0, 229, 255, 0.12);
  box-shadow: 0 0 0 2px rgba(0, 229, 255, 0.25), 0 0 20px rgba(0, 229, 255, 0.12);
}
.option-item__label {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(79, 240, 181, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #4ff0b5;
  flex-shrink: 0;
  transition: all 0.2s;
}
.option-item.is-selected .option-item__label {
  background: linear-gradient(135deg, var(--gsmv-primary), #4ff0b5);
  color: #0a1628;
  border-color: transparent;
}
.option-item__text {
  font-size: 15px;
  line-height: 1.5;
  color: #d0e8ff;
}

.question-card__nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(79, 240, 181, 0.15);
}
.question-card__dots {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}
.question-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  cursor: pointer;
  transition: all 0.2s;
}
.question-dot.is-current { background: var(--gsmv-primary); transform: scale(1.3); box-shadow: 0 0 8px rgba(0, 229, 255, 0.5); }
.question-dot.is-answered { background: rgba(79, 240, 181, 0.5); }

/* ===== Overview Grid ===== */
.question-overview__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.overview-dot {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(79, 240, 181, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--gsmv-muted);
  cursor: pointer;
  transition: all 0.2s;
}
.overview-dot:hover {
  border-color: rgba(0, 229, 255, 0.4);
  color: var(--gsmv-text);
  background: rgba(0, 229, 255, 0.06);
}
.overview-dot.is-current {
  background: linear-gradient(135deg, var(--gsmv-primary), #4ff0b5);
  color: #0a1628;
  border-color: transparent;
  box-shadow: 0 0 16px rgba(0, 229, 255, 0.35);
}
.overview-dot.is-answered {
  background: rgba(79, 240, 181, 0.15);
  color: #4ff0b5;
  border-color: rgba(79, 240, 181, 0.4);
}

/* ===== Fill-in ===== */
.question-card__fill {
  margin-top: 8px;
}
.fill-hints {
  margin-bottom: 16px;
}
.fill-hints .el-alert {
  background: rgba(0, 229, 255, 0.06);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: 10px;
}
.fill-inputs {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}
.fill-input-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.fill-input-label {
  font-weight: 600;
  color: #4ff0b5;
  min-width: 52px;
  flex-shrink: 0;
  font-size: 14px;
}
.fill-input {
  flex: 1;
  max-width: 280px;
}
.fill-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  border: none;
  border-bottom: 2px solid rgba(79, 240, 181, 0.4);
  border-radius: 0;
  padding: 4px 8px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.fill-input :deep(.el-input__inner) {
  background: transparent;
  color: var(--gsmv-text);
  font-size: 16px;
  padding: 6px 4px;
  height: auto;
  line-height: 1.5;
}
.fill-input :deep(.el-input__wrapper.is-focus) {
  border-bottom-color: var(--gsmv-primary) !important;
  box-shadow: 0 3px 0 0 rgba(0, 229, 255, 0.25) !important;
}
.fill-input :deep(.el-input__inner)::placeholder {
  color: rgba(224, 244, 235, 0.3);
  font-style: italic;
}
.fill-input :deep(.el-input__clear) {
  color: rgba(79, 240, 181, 0.5);
}
.fill-preview {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(79, 240, 181, 0.15);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.fill-preview__label {
  color: var(--gsmv-muted);
  font-size: 13px;
}
.fill-preview__value {
  color: #4ff0b5;
  background: rgba(0, 0, 0, 0.3);
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 13px;
}
</style>
