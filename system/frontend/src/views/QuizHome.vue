<template>
  <div class="page-shell">
    <section class="page-hero">
      <div>
        <h2>航海知识问答</h2>
        <p>测试你在船舶、天气和海域方面的专业知识水平。支持单选、多选、判断和填空题型，随机或顺序出题。</p>
      </div>
      <el-button
        v-if="canManage"
        type="primary"
        plain
        @click="router.push('/quiz/manage')"
      >
        <el-icon style="margin-right: 4px"><Setting /></el-icon>
        题库管理
      </el-button>
      <el-button
        type="primary"
        @click="router.push('/quiz/ai')"
      >
        <el-icon style="margin-right: 4px"><ChatDotRound /></el-icon>
        AI 知识助手
      </el-button>
    </section>

    <!-- Stats Cards -->
    <section class="quiz-metrics">
      <div class="quiz-metric quiz-metric--ship" @click="startQuick('SHIP')">
        <span class="quiz-metric__icon">🚢</span>
        <strong>{{ stats.ship }}</strong>
        <span>船舶题库</span>
      </div>
      <div class="quiz-metric quiz-metric--weather" @click="startQuick('WEATHER')">
        <span class="quiz-metric__icon">🌊</span>
        <strong>{{ stats.weather }}</strong>
        <span>天气题库</span>
      </div>
      <div class="quiz-metric quiz-metric--sea" @click="startQuick('SEA_AREA')">
        <span class="quiz-metric__icon">🗺️</span>
        <strong>{{ stats.seaArea }}</strong>
        <span>海域题库</span>
      </div>
    </section>

    <!-- Quick Start -->
    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="panel-header">
          <strong>快速开始答题</strong>
        </div>
      </template>
      <el-form label-width="100px" label-position="left">
        <el-form-item label="题目分类">
          <el-checkbox-group v-model="examConfig.categories">
            <el-checkbox value="SHIP">船舶知识</el-checkbox>
            <el-checkbox value="WEATHER">天气知识</el-checkbox>
            <el-checkbox value="SEA_AREA">海域知识</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="题目数量">
          <el-slider v-model="examConfig.count" :min="5" :max="50" :step="5" show-input />
        </el-form-item>
        <el-form-item label="难度筛选">
          <el-select v-model="examConfig.difficulty" placeholder="不限难度" clearable style="width: 160px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="答题模式">
          <el-radio-group v-model="examConfig.mode">
            <el-radio value="RANDOM">随机抽题</el-radio>
            <el-radio value="SEQUENTIAL">顺序出题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="startExamAction" :loading="starting">
            开始答题
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- History -->
    <el-card class="panel-card" shadow="never">
      <template #header>
        <div class="panel-header">
          <strong>我的答题记录</strong>
        </div>
      </template>
      <el-table :data="records" stripe v-loading="loadingHistory" empty-text="暂无答题记录">
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.startedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="分类" width="200">
          <template #default="{ row }">
            <el-tag v-for="c in parseCategories(row.categories)" :key="c" size="small" style="margin-right: 4px">
              {{ catLabel(c) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模式" width="100">
          <template #default="{ row }">
            {{ row.mode === 'RANDOM' ? '随机' : '顺序' }}
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">
            <span :class="gradeClass(row.score, row.total)">
              {{ row.score }} / {{ row.total }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="正确率" width="100">
          <template #default="{ row }">
            {{ row.total > 0 ? Math.round((row.score / row.total) * 100) : 0 }}%
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-tag v-if="row.finishedAt" type="success" size="small">已完成</el-tag>
            <el-tag v-else type="warning" size="small">未完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.finishedAt" link type="primary" @click="viewResult(row.id)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-if="historyTotal > 0"
          v-model:current-page="historyPage"
          :page-size="10"
          :total="historyTotal"
          layout="prev, pager, next"
          @current-change="loadHistory"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting, ChatDotRound } from '@element-plus/icons-vue'
import { fetchQuestionStats, startExam, fetchRecords } from '@/api/quiz'
import { useAuthStore } from '@/stores/auth'
import type { QuizRecord } from '@/types/gsmv'

const router = useRouter()
const authStore = useAuthStore()

const canManage = computed(() => authStore.authorities.includes('QUIZ_WRITE'))

const stats = reactive({ ship: 0, weather: 0, seaArea: 0 })
const starting = ref(false)
const examConfig = reactive({
  categories: ['SHIP', 'WEATHER', 'SEA_AREA'] as string[],
  count: 20,
  difficulty: '',
  mode: 'RANDOM',
})

const records = ref<QuizRecord[]>([])
const loadingHistory = ref(false)
const historyPage = ref(1)
const historyTotal = ref(0)

function catLabel(c: string): string {
  const map: Record<string, string> = { SHIP: '船舶', WEATHER: '天气', SEA_AREA: '海域' }
  return map[c] || c
}

function parseCategories(c?: string): string[] {
  return c ? c.split(',') : ['综合']
}

function gradeClass(score: number, total: number): string {
  if (total === 0) return ''
  const pct = score / total
  if (pct >= 0.9) return 'grade--excellent'
  if (pct >= 0.75) return 'grade--good'
  if (pct >= 0.6) return 'grade--pass'
  return 'grade--fail'
}

function formatTime(s: string): string {
  return new Date(s).toLocaleString('zh-CN')
}

async function startQuick(cat: string) {
  starting.value = true
  try {
    const res = await startExam({ categories: [cat], count: 10, mode: 'RANDOM' })
    sessionStorage.setItem(`quiz_${res.recordId}`, JSON.stringify(res.questions))
    router.push({ path: '/quiz/exam', query: { recordId: res.recordId } })
  } catch {
    ElMessage.error('启动答题失败')
  } finally {
    starting.value = false
  }
}

async function startExamAction() {
  if (examConfig.categories.length === 0) {
    ElMessage.warning('请至少选择一个分类')
    return
  }
  starting.value = true
  try {
    const res = await startExam({
      categories: examConfig.categories,
      count: examConfig.count,
      mode: examConfig.mode,
      difficulty: examConfig.difficulty || undefined,
    })
    sessionStorage.setItem(`quiz_${res.recordId}`, JSON.stringify(res.questions))
    router.push({ path: '/quiz/exam', query: { recordId: res.recordId } })
  } catch {
    ElMessage.error('启动答题失败')
  } finally {
    starting.value = false
  }
}

async function loadStats() {
  try {
    const s = await fetchQuestionStats()
    stats.ship = s.ship
    stats.weather = s.weather
    stats.seaArea = s.seaArea
  } catch { /* ignore */ }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await fetchRecords({ page: historyPage.value, size: 10 })
    records.value = res.items
    historyTotal.value = res.total
  } catch { /* ignore */ }
  loadingHistory.value = false
}

function viewResult(id: number) {
  router.push({ path: '/quiz/result', query: { recordId: id } })
}

onMounted(() => {
  loadStats()
  loadHistory()
})
</script>

<style scoped>
/* ===== Stat Metrics ===== */
.quiz-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.quiz-metric {
  min-height: 92px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.12), rgba(124, 60, 255, 0.08)),
    rgba(255, 255, 255, 0.06);
  box-shadow: var(--gsmv-shadow-soft);
  text-align: center;
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease, box-shadow 0.25s ease;
}

.quiz-metric:hover {
  transform: translateY(-3px);
}

.quiz-metric__icon {
  font-size: 28px;
  display: block;
  margin-bottom: 8px;
}

.quiz-metric strong {
  display: block;
  font-size: 28px;
  letter-spacing: -1px;
  color: #f2fdff;
}

.quiz-metric span {
  display: block;
  color: var(--gsmv-muted);
  font-size: 13px;
  margin-top: 4px;
}

/* Ship - cyan */
.quiz-metric--ship {
  border-color: rgba(0, 229, 255, 0.18);
}
.quiz-metric--ship:hover {
  border-color: rgba(0, 229, 255, 0.38);
  box-shadow: 0 0 28px rgba(0, 229, 255, 0.16), 0 18px 48px rgba(0, 4, 18, 0.36);
}
.quiz-metric--ship strong { color: #00e5ff; text-shadow: 0 0 20px rgba(0, 229, 255, 0.3); }

/* Weather - green */
.quiz-metric--weather {
  border-color: rgba(79, 240, 181, 0.18);
}
.quiz-metric--weather:hover {
  border-color: rgba(79, 240, 181, 0.38);
  box-shadow: 0 0 28px rgba(79, 240, 181, 0.16), 0 18px 48px rgba(0, 4, 18, 0.36);
}
.quiz-metric--weather strong { color: #4ff0b5; text-shadow: 0 0 20px rgba(79, 240, 181, 0.3); }

/* Sea Area - gold */
.quiz-metric--sea {
  border-color: rgba(255, 200, 87, 0.18);
}
.quiz-metric--sea:hover {
  border-color: rgba(255, 200, 87, 0.38);
  box-shadow: 0 0 28px rgba(255, 200, 87, 0.16), 0 18px 48px rgba(0, 4, 18, 0.36);
}
.quiz-metric--sea strong { color: #ffc857; text-shadow: 0 0 20px rgba(255, 200, 87, 0.3); }

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

/* ===== Table Footer ===== */
.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

/* ===== Grade Colors ===== */
.grade--excellent { color: #4ff0b5 !important; font-weight: bold; text-shadow: 0 0 12px rgba(79, 240, 181, 0.3); }
.grade--good      { color: #00e5ff !important; font-weight: bold; text-shadow: 0 0 12px rgba(0, 229, 255, 0.3); }
.grade--pass      { color: #ffc857 !important; font-weight: bold; }
.grade--fail      { color: #ff6b8a !important; font-weight: bold; }

@media (max-width: 720px) {
  .quiz-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
