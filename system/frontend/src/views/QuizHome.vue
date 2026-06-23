<template>
  <div class="quiz-home">
    <div class="quiz-home__hero">
      <div class="quiz-home__hero-text">
        <h2>航海知识问答</h2>
        <p>测试你在船舶、天气和海域方面的专业知识水平</p>
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
    </div>

    <!-- Stats Cards -->
    <div class="quiz-home__stats">
      <div class="stat-card stat-card--ship" @click="startQuick('SHIP')">
        <span class="stat-card__icon">🚢</span>
        <strong>{{ stats.ship }}</strong>
        <span>船舶题库</span>
      </div>
      <div class="stat-card stat-card--weather" @click="startQuick('WEATHER')">
        <span class="stat-card__icon">🌊</span>
        <strong>{{ stats.weather }}</strong>
        <span>天气题库</span>
      </div>
      <div class="stat-card stat-card--sea" @click="startQuick('SEA_AREA')">
        <span class="stat-card__icon">🗺️</span>
        <strong>{{ stats.seaArea }}</strong>
        <span>海域题库</span>
      </div>
    </div>

    <!-- Quick Start -->
    <div class="quiz-home__quick">
      <el-card shadow="hover">
        <template #header>
          <span>快速开始答题</span>
        </template>
        <el-form label-width="100px" label-position="left">
          <el-form-item label="题目分类">
            <el-checkbox-group v-model="examConfig.categories">
              <el-checkbox label="SHIP">船舶知识</el-checkbox>
              <el-checkbox label="WEATHER">天气知识</el-checkbox>
              <el-checkbox label="SEA_AREA">海域知识</el-checkbox>
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
              <el-radio label="RANDOM">随机抽题</el-radio>
              <el-radio label="SEQUENTIAL">顺序出题</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="startExamAction" :loading="starting">
              开始答题
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- History -->
    <div class="quiz-home__history">
      <h3>我的答题记录</h3>
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
      <el-pagination
        v-if="historyTotal > 0"
        v-model:current-page="historyPage"
        :page-size="10"
        :total="historyTotal"
        layout="prev, pager, next"
        @current-change="loadHistory"
        style="margin-top: 16px; justify-content: center"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
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
.quiz-home {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

/* ===== Hero ===== */
.quiz-home__hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.quiz-home__hero h2 {
  font-size: 28px;
  margin: 0 0 8px;
  background: linear-gradient(90deg, #ffffff, var(--gsmv-primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.quiz-home__hero p {
  color: rgba(224, 244, 235, 0.5);
  margin: 0;
}

/* ===== Stat Cards ===== */
.quiz-home__stats {
  display: flex;
  gap: 16px;
  margin: 24px 0;
}
.stat-card {
  flex: 1;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.05), rgba(124, 60, 255, 0.03)),
    rgba(10, 16, 35, 0.7);
  border-radius: 14px;
  padding: 24px 20px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  border: 1px solid transparent;
}
.stat-card:hover {
  transform: translateY(-3px);
}
.stat-card__icon { font-size: 30px; display: block; margin-bottom: 10px; }
.stat-card strong { 
  font-size: 30px; 
  display: block; 
  color: #e8f4ff;
  letter-spacing: -1px;
}
.stat-card span { 
  color: rgba(224, 244, 235, 0.45); 
  font-size: 13px; 
}

/* Ship - cyan */
.stat-card--ship {
  border-bottom: 3px solid #00e5ff;
  border-color: rgba(0, 229, 255, 0.15);
  box-shadow: 0 4px 24px rgba(0, 229, 255, 0.08);
}
.stat-card--ship:hover {
  border-color: rgba(0, 229, 255, 0.35);
  box-shadow:
    0 12px 40px rgba(0, 229, 255, 0.18),
    0 0 30px rgba(0, 229, 255, 0.06);
}
.stat-card--ship strong { color: #00e5ff; text-shadow: 0 0 20px rgba(0, 229, 255, 0.3); }

/* Weather - green */
.stat-card--weather {
  border-bottom: 3px solid #4ff0b5;
  border-color: rgba(79, 240, 181, 0.15);
  box-shadow: 0 4px 24px rgba(79, 240, 181, 0.08);
}
.stat-card--weather:hover {
  border-color: rgba(79, 240, 181, 0.35);
  box-shadow:
    0 12px 40px rgba(79, 240, 181, 0.18),
    0 0 30px rgba(79, 240, 181, 0.06);
}
.stat-card--weather strong { color: #4ff0b5; text-shadow: 0 0 20px rgba(79, 240, 181, 0.3); }

/* Sea Area - gold */
.stat-card--sea {
  border-bottom: 3px solid #ffc857;
  border-color: rgba(255, 200, 87, 0.15);
  box-shadow: 0 4px 24px rgba(255, 200, 87, 0.08);
}
.stat-card--sea:hover {
  border-color: rgba(255, 200, 87, 0.35);
  box-shadow:
    0 12px 40px rgba(255, 200, 87, 0.18),
    0 0 30px rgba(255, 200, 87, 0.06);
}
.stat-card--sea strong { color: #ffc857; text-shadow: 0 0 20px rgba(255, 200, 87, 0.3); }

/* ===== Quick Start Card ===== */
.quiz-home__quick { margin: 24px 0; }
.quiz-home__quick :deep(.el-card) {
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.04), rgba(124, 60, 255, 0.02)),
    rgba(10, 16, 35, 0.75);
  border: 1px solid rgba(0, 229, 255, 0.14);
  border-radius: 16px;
  box-shadow: 0 6px 32px rgba(0, 4, 18, 0.25);
}
.quiz-home__quick :deep(.el-card__header) {
  color: #e8f4ff;
  font-weight: 600;
  font-size: 15px;
  border-bottom: 1px solid rgba(79, 240, 181, 0.1);
}
.quiz-home__quick :deep(.el-form-item__label) {
  color: rgba(224, 244, 235, 0.65);
}

/* ===== History ===== */
.quiz-home__history { margin-top: 32px; }
.quiz-home__history h3 { 
  margin: 0 0 16px; 
  color: #e8f4ff; 
  font-size: 17px;
  font-weight: 600;
}
.quiz-home__history :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(0, 229, 255, 0.05);
  --el-table-header-text-color: rgba(224, 244, 235, 0.6);
  --el-table-text-color: rgba(224, 244, 235, 0.65);
  --el-table-border-color: rgba(79, 240, 181, 0.08);
}
.quiz-home__history :deep(.el-table th) {
  color: #e8f4ff !important;
  font-weight: 600;
}
.grade--excellent { color: #4ff0b5 !important; font-weight: bold; text-shadow: 0 0 12px rgba(79, 240, 181, 0.3); }
.grade--good      { color: #00e5ff !important; font-weight: bold; text-shadow: 0 0 12px rgba(0, 229, 255, 0.3); }
.grade--pass      { color: #ffc857 !important; font-weight: bold; }
.grade--fail      { color: #ff6b8a !important; font-weight: bold; }
</style>
