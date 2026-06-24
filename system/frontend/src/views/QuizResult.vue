<template>
  <div class="page-shell">
    <section class="page-hero quiz-result-hero">
      <div>
        <h2>答题结果</h2>
        <p>查看本次答题的得分、正确率和每道题的详细解析。</p>
      </div>
      <el-button link @click="router.push('/quiz')">
        <el-icon><ArrowLeft /></el-icon> 返回题库
      </el-button>
    </section>

    <div v-if="loading" class="panel-card-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <template v-else-if="result">
      <!-- Score Card -->
      <el-card class="panel-card" shadow="never">
        <div class="score-summary">
          <div class="score-summary__ring">
            <svg viewBox="0 0 120 120" width="120" height="120">
              <circle cx="60" cy="60" r="52" fill="none" stroke="rgba(255,255,255,0.08)" stroke-width="8" />
              <circle
                cx="60" cy="60" r="52"
                fill="none"
                :stroke="scoreColor"
                stroke-width="8"
                stroke-linecap="round"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="dashOffset"
                transform="rotate(-90 60 60)"
                style="transition: stroke-dashoffset 1s ease"
              />
            </svg>
            <div class="score-summary__text">
              <strong>{{ result.score }}</strong>
              <span>/ {{ result.total }}</span>
            </div>
          </div>
          <div class="score-summary__info">
            <div class="score-summary__grade" :class="'grade--' + gradeKey">{{ result.grade }}</div>
            <div class="score-summary__pct">正确率 {{ Math.round((result.score / result.total) * 100) }}%</div>
          </div>
        </div>
      </el-card>

      <!-- Detail List -->
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="panel-header">
            <strong>答题详情与解析</strong>
          </div>
        </template>
        <div
          v-for="(item, idx) in result.details"
          :key="item.questionId"
          class="result-item"
          :class="{ 'is-correct': item.correct, 'is-wrong': !item.correct }"
        >
          <div class="result-item__header">
            <span class="result-item__num">{{ idx + 1 }}</span>
            <span class="result-item__title">{{ item.title }}</span>
            <el-tag :type="item.correct ? 'success' : 'danger'" size="small" style="margin-left: auto">
              {{ item.correct ? '正确' : '错误' }}
            </el-tag>
          </div>

          <div class="result-item__meta" v-if="item.type !== 'FILL' && item.options">
            <template v-for="opt in parseOpts(item.options)" :key="opt.label">
              <div class="result-item__option"
                   :class="{
                     'option--chosen-correct': item.correct && isInAnswer(opt.label, item.userAnswer),
                     'option--chosen-wrong': !item.correct && isInAnswer(opt.label, item.userAnswer),
                     'option--correct-answer': isInAnswer(opt.label, item.correctAnswer),
                   }">
                <span class="option-dot">{{ opt.label }}</span>
                {{ opt.text }}
              </div>
            </template>
          </div>

          <div class="result-item__answers">
            <span>你的答案：<strong :class="item.correct ? 'text-success' : 'text-danger'">{{ item.userAnswer || '未作答' }}</strong></span>
            <span v-if="!item.correct" style="margin-left: 16px">正确答案：<strong class="text-success">{{ item.correctAnswer }}</strong></span>
          </div>

          <div v-if="item.explanation" class="result-item__explanation">
            <el-icon><InfoFilled /></el-icon>
            {{ item.explanation }}
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import { fetchExamResult } from '@/api/quiz'
import type { QuizResultResponse } from '@/api/quiz'

const route = useRoute()
const router = useRouter()

const result = ref<QuizResultResponse | null>(null)
const loading = ref(true)

const circumference = 2 * Math.PI * 52

const scoreColor = computed(() => {
  if (!result.value) return '#00e5ff'
  const pct = result.value.score / result.value.total
  if (pct >= 0.9) return '#4ff0b5'
  if (pct >= 0.75) return '#00e5ff'
  if (pct >= 0.6) return '#ffc857'
  return '#ff6b8a'
})

const dashOffset = computed(() => {
  if (!result.value) return circumference
  return circumference * (1 - result.value.score / result.value.total)
})

const gradeKey = computed(() => {
  if (!result.value) return 'fail'
  const pct = result.value.score / result.value.total
  if (pct >= 0.9) return 'excellent'
  if (pct >= 0.75) return 'good'
  if (pct >= 0.6) return 'pass'
  return 'fail'
})

function parseOpts(opts: string): { label: string; text: string }[] {
  try {
    return JSON.parse(opts)
  } catch {
    return []
  }
}

function isInAnswer(label: string, answer?: string): boolean {
  if (!answer) return false
  return answer.split(',').includes(label)
}

onMounted(async () => {
  const recordId = Number(route.query.recordId) || 0
  if (!recordId) {
    router.push('/quiz')
    return
  }
  try {
    result.value = await fetchExamResult(recordId)
  } catch {
    // ignore
  }
  loading.value = false
})
</script>

<style scoped>
.quiz-result-hero {
  align-items: center;
  padding: 18px 28px;
}

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

/* ===== Score Summary ===== */
.score-summary {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 8px;
}
.score-summary__ring {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}
.score-summary__ring svg circle:first-child {
  stroke: rgba(255, 255, 255, 0.08) !important;
}
.score-summary__text {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.score-summary__text strong {
  font-size: 34px;
  background: linear-gradient(90deg, var(--gsmv-primary), #4ff0b5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.score-summary__text span {
  font-size: 14px;
  color: var(--gsmv-muted);
}
.score-summary__grade { font-size: 30px; font-weight: 700; letter-spacing: 2px; }
.score-summary__pct { font-size: 16px; color: var(--gsmv-muted); margin-top: 4px; }
.grade--excellent { color: #4ff0b5; text-shadow: 0 0 20px rgba(79, 240, 181, 0.4); }
.grade--good      { color: var(--gsmv-primary); text-shadow: 0 0 20px rgba(0, 229, 255, 0.4); }
.grade--pass      { color: #ffc857; text-shadow: 0 0 16px rgba(255, 200, 87, 0.35); }
.grade--fail      { color: var(--gsmv-danger); text-shadow: 0 0 16px rgba(255, 107, 138, 0.35); }

/* ===== Result Items ===== */
.result-item {
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.03), rgba(124, 60, 255, 0.02)),
    rgba(10, 16, 35, 0.78);
  border-radius: 14px;
  padding: 22px;
  margin-bottom: 14px;
  border-left: 4px solid rgba(255, 255, 255, 0.12);
  transition: all 0.25s ease;
}
.result-item:hover {
  border-color: rgba(0, 229, 255, 0.3);
  box-shadow: 0 4px 24px rgba(0, 4, 18, 0.25);
}
.result-item.is-correct { border-left-color: #4ff0b5; }
.result-item.is-wrong   { border-left-color: #ff6b8a; }

.result-item__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}
.result-item__num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(79, 240, 181, 0.15);
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--gsmv-muted);
}
.result-item.is-correct .result-item__num {
  background: rgba(79, 240, 181, 0.15);
  color: #4ff0b5;
  border-color: rgba(79, 240, 181, 0.3);
}
.result-item.is-wrong .result-item__num {
  background: rgba(255, 107, 138, 0.15);
  color: #ff6b8a;
  border-color: rgba(255, 107, 138, 0.3);
}
.result-item__title {
  font-size: 15.5px;
  font-weight: 500;
  line-height: 1.65;
  color: #d0e8ff;
  padding-top: 2px;
}

/* Options */
.result-item__meta { padding-left: 40px; }
.result-item__option {
  padding: 7px 14px;
  margin: 4px 0;
  border-radius: 8px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.option-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(79, 240, 181, 0.15);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  color: rgba(224, 244, 235, 0.4);
}
/* chosen + correct */
.option--chosen-correct {
  background: rgba(79, 240, 181, 0.08);
  border-color: rgba(79, 240, 181, 0.25);
}
.option--chosen-correct .option-dot {
  background: linear-gradient(135deg, var(--gsmv-primary), #4ff0b5);
  color: #0a1628;
  border-color: transparent;
}
/* chosen but wrong */
.option--chosen-wrong {
  background: rgba(255, 107, 138, 0.08);
  border-color: rgba(255, 107, 138, 0.25);
}
.option--chosen-wrong .option-dot {
  background: #ff6b8a;
  color: #fff;
  border-color: transparent;
}
/* correct answer (for wrong items) */
.option--correct-answer {
  background: rgba(79, 240, 181, 0.06);
  border-color: rgba(79, 240, 181, 0.2);
}
.option--correct-answer .option-dot {
  background: rgba(79, 240, 181, 0.3);
  color: #4ff0b5;
  border-color: transparent;
}
/* option text */
.result-item__option:not(.option--chosen-correct):not(.option--chosen-wrong):not(.option--correct-answer) {
  color: rgba(224, 244, 235, 0.35);
}

/* Answers line */
.result-item__answers {
  padding-left: 40px;
  margin: 12px 0;
  font-size: 14px;
  color: var(--gsmv-muted);
}
.text-success { color: #4ff0b5 !important; font-weight: 600; }
.text-danger  { color: #ff6b8a !important; font-weight: 600; }

/* Explanation */
.result-item__explanation {
  padding: 12px 16px;
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.04), rgba(124, 60, 255, 0.03));
  border: 1px solid rgba(0, 229, 255, 0.1);
  border-radius: 10px;
  margin-top: 10px;
  margin-left: 26px;
  font-size: 13.5px;
  line-height: 1.75;
  color: rgba(224, 244, 235, 0.7);
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.result-item__explanation .el-icon {
  margin-top: 3px;
  color: var(--gsmv-primary);
  flex-shrink: 0;
}

@media (max-width: 720px) {
  .score-summary {
    flex-direction: column;
    text-align: center;
  }
}
</style>
