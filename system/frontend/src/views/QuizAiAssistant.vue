<template>
  <div class="page-shell quiz-ai-page">
    <section class="page-hero">
      <div>
        <h2>AI 航海知识助手</h2>
        <p>
          基于 AI 大模型，可以为你出题、解答船舶 / 气象 / 海域三类航海知识问题。
          支持一键生成题目入库、实时天气查询。所有登录用户均可使用。
        </p>
        <div class="page-hero__actions">
          <el-button type="primary" @click="applyPrompt(quickPrompts[0])">让我出一道题</el-button>
          <el-button type="primary" plain @click="applyPrompt(quickPrompts[3])">问一个知识问题</el-button>
        </div>
      </div>
    </section>

    <div class="quiz-ai-grid">
      <el-card class="panel-card quiz-ai-workbench" shadow="never">
        <template #header>
          <div class="quiz-ai-header">
            <div>
              <strong>对话区</strong>
              <p>让我出题、解释知识点，或者回答你的航海知识问题。问天气问题时我会自动获取实时数据。</p>
            </div>
            <div class="quiz-ai-header__actions">
              <span>{{ messages.length }} 条消息</span>
              <el-tooltip content="刷新历史记录" placement="top">
                <el-button circle :icon="Refresh" :loading="loadingHistory" @click="loadHistory()" />
              </el-tooltip>
              <el-tooltip content="清空当前账号的对话记录" placement="top">
                <el-button circle :icon="Delete" :disabled="loading || loadingHistory" @click="clearHistory()" />
              </el-tooltip>
            </div>
          </div>
        </template>

        <div ref="messagesContainerRef" v-loading="loadingHistory" class="quiz-ai-messages" element-loading-text="加载历史记录...">
          <div
            v-for="(item, index) in messages"
            :key="`${item.role}-${index}`"
            class="quiz-ai-message"
            :class="item.role === 'user' ? 'quiz-ai-message--user' : 'quiz-ai-message--assistant'"
          >
            <div class="quiz-ai-message__meta">
              <span>{{ item.role === 'user' ? '你' : 'AI 知识助手' }}</span>
            </div>
            <div class="quiz-ai-message__content" v-html="renderMarkdown(item.content)"></div>
          </div>
        </div>

        <div class="quiz-ai-composer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="例如：给我出 3 道船舶知识单选题，中等难度。或者问：上海现在的天气怎么样？"
            @keydown.ctrl.enter.prevent="sendMessage()"
          />
          <div class="quiz-ai-composer__actions">
            <span>按 Ctrl + Enter 发送</span>
            <el-button type="primary" :loading="loading" @click="sendMessage()">发送</el-button>
          </div>
        </div>
      </el-card>

      <div class="quiz-ai-side">
        <!-- AI 出题入库 -->
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-header">
              <strong>AI 出题入库</strong>
            </div>
          </template>
          <el-form label-width="70px" label-position="left" size="small">
            <el-form-item label="分类">
              <el-select v-model="genForm.category" style="width: 100%">
                <el-option value="SHIP" label="船舶知识" />
                <el-option value="WEATHER" label="气象知识" />
                <el-option value="SEA_AREA" label="海域知识" />
              </el-select>
            </el-form-item>
            <el-form-item label="题型">
              <el-select v-model="genForm.type" style="width: 100%">
                <el-option value="SINGLE" label="单选题" />
                <el-option value="MULTI" label="多选题" />
                <el-option value="JUDGE" label="判断题" />
                <el-option value="FILL" label="填空题" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="genForm.difficulty" style="width: 100%">
                <el-option value="EASY" label="简单" />
                <el-option value="MEDIUM" label="中等" />
                <el-option value="HARD" label="困难" />
              </el-select>
            </el-form-item>
            <el-form-item label="数量">
              <el-slider v-model="genForm.count" :min="1" :max="10" :step="1" show-input style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                style="width: 100%"
                :loading="generating"
                @click="handleGenerate"
              >
                生成并入库
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 生成结果 -->
          <div v-if="genResult" class="gen-result">
            <el-divider />
            <div class="gen-result__summary">
              <el-tag type="success" size="small">入库 {{ genResult.totalSaved }} 题</el-tag>
              <el-tag v-if="genResult.totalDuplicates > 0" type="warning" size="small">重复跳过 {{ genResult.totalDuplicates }} 题</el-tag>
            </div>
            <div v-if="genResult.saved.length > 0" class="gen-result__list">
              <div v-for="(q, i) in genResult.saved" :key="i" class="gen-result__item gen-result__item--saved">
                <span class="gen-result__badge">{{ typeLabel(q.type) }}</span>
                <span class="gen-result__title">{{ q.title }}</span>
              </div>
            </div>
            <div v-if="genResult.duplicates.length > 0" class="gen-result__list">
              <div class="gen-result__dup-title">重复题目（已跳过）：</div>
              <div v-for="(q, i) in genResult.duplicates" :key="`d-${i}`" class="gen-result__item gen-result__item--dup">
                <span class="gen-result__badge gen-result__badge--dup">{{ typeLabel(q.type) }}</span>
                <span class="gen-result__title">{{ q.title }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>快捷问题</strong>
          </template>
          <div class="prompt-list">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt"
              class="prompt-chip"
              type="button"
              @click="applyPrompt(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </el-card>

        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>使用指南</strong>
          </template>
          <div class="guide-list">
            <div class="guide-item">
              <span class="guide-item__icon">📝</span>
              <div>
                <strong>AI 出题</strong>
                <p>对话出题获取 Markdown 格式题目。使用右侧"AI 出题入库"可直接生成结构化题目并存入题库。</p>
              </div>
            </div>
            <div class="guide-item">
              <span class="guide-item__icon">🌤️</span>
              <div>
                <strong>实时天气</strong>
                <p>问天气问题时（如"上海现在天气怎么样"），我会自动获取实时天气数据来回答。</p>
              </div>
            </div>
            <div class="guide-item">
              <span class="guide-item__icon">💡</span>
              <div>
                <strong>知识解答</strong>
                <p>提出任何船舶、气象、海域相关问题，我会给出准确的专业解答。</p>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  askQuizAiStream,
  clearQuizAiHistory,
  generateQuizQuestions,
  getQuizAiHistory,
  type GenerateQuestionsResponse,
  type QuizAiMessage,
} from '@/api/quiz'

const quickPrompts = [
  '给我出 3 道船舶知识单选题，中等难度',
  '出一道天气知识的判断题，简单难度',
  '出 2 道海域知识的多选题，困难难度',
  '船舶在雾中航行时应遵守哪些规则？',
  '上海现在的天气怎么样？适合出海吗？',
  '什么是潮汐不等？请详细说明',
]

const welcomeMessage: QuizAiMessage = {
  role: 'assistant',
  content: '你好！我是航海知识 AI 助手，可以为你出题或解答船舶、气象、海域相关的知识问题。试试问我"出一道船舶知识单选题"或"上海现在天气怎么样"吧！',
}

const messages = ref<QuizAiMessage[]>([{ ...welcomeMessage }])
const input = ref('')
const loading = ref(false)
const loadingHistory = ref(false)
const messagesContainerRef = ref<HTMLDivElement | null>(null)

// AI 出题入库
const generating = ref(false)
const genResult = ref<GenerateQuestionsResponse | null>(null)
const genForm = reactive({
  category: 'SHIP',
  type: 'SINGLE',
  difficulty: 'EASY',
  count: 3,
})

function typeLabel(type: string): string {
  const map: Record<string, string> = { SINGLE: '单选', MULTI: '多选', JUDGE: '判断', FILL: '填空' }
  return map[type] || type
}

async function handleGenerate() {
  if (generating.value) return
  generating.value = true
  genResult.value = null
  try {
    const result = await generateQuizQuestions({
      category: genForm.category,
      type: genForm.type,
      difficulty: genForm.difficulty,
      count: genForm.count,
    })
    genResult.value = result
    if (result.totalSaved > 0) {
      ElMessage.success(`成功入库 ${result.totalSaved} 道题目${result.totalDuplicates > 0 ? `，跳过 ${result.totalDuplicates} 道重复题` : ''}`)
    } else if (result.totalDuplicates > 0) {
      ElMessage.warning(`生成的 ${result.totalDuplicates} 道题目均已存在，未入库`)
    } else {
      ElMessage.warning('AI 未生成有效题目，请重试')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成题目失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function applyPrompt(prompt: string) {
  input.value = prompt
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  let html = text
  html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
  html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^# (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>.*<\/li>\n?)+/g, (match) => `<ul>${match}</ul>`)
  html = html.replace(/^---$/gm, '<hr/>')
  html = html.replace(/\n/g, '<br/>')
  html = html.replace(/(<ul>.*?<\/ul>)/gs, (match) => match.replace(/<br\/>/g, ''))
  html = html.replace(/<br\/><(h[34]|ul|hr)/g, '<$1')
  return html
}

async function scrollMessagesToBottom(behavior: ScrollBehavior = 'smooth') {
  await nextTick()
  if (messagesContainerRef.value) {
    messagesContainerRef.value.scrollTo({
      top: messagesContainerRef.value.scrollHeight,
      behavior,
    })
  }
}

watch(
  () => messages.value.length,
  (_, previousLength) => {
    void scrollMessagesToBottom(previousLength === undefined ? 'auto' : 'smooth')
  },
)

watch(loading, (value) => {
  if (value) {
    void scrollMessagesToBottom('smooth')
  }
})

onMounted(() => {
  void loadHistory()
})

async function sendMessage(prefilled?: string) {
  const message = (prefilled ?? input.value).trim()
  if (!message || loading.value) {
    return
  }

  const history = messages.value
    .filter((item) => item.role === 'user' || item.role === 'assistant')
    .filter((item) => item.content !== welcomeMessage.content)
    .slice(-6)

  messages.value.push({ role: 'user', content: message })
  const assistantMessageIndex = messages.value.push({
    role: 'assistant',
    content: '正在思考中...',
  }) - 1
  input.value = ''
  loading.value = true
  let answerStarted = false

  try {
    await askQuizAiStream({ message, history }, (event) => {
      if (event.type === 'status') {
        if (!answerStarted && event.content) {
          updateAssistantMessage(assistantMessageIndex, event.content)
        }
        return
      }

      if (event.type === 'delta') {
        if (!answerStarted) {
          answerStarted = true
          updateAssistantMessage(assistantMessageIndex, '')
        }
        appendAssistantMessage(assistantMessageIndex, event.content ?? '')
        return
      }

      if (event.type === 'final' && event.response) {
        updateAssistantMessage(assistantMessageIndex, event.response.answer)
        return
      }

      if (event.type === 'error') {
        throw new Error(event.content || 'AI 助手暂时不可用')
      }
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 助手暂时不可用')
    updateAssistantMessage(assistantMessageIndex, '这次回答失败了，请稍后再试，或者换一种问法。')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const history = await getQuizAiHistory()
    messages.value = history.messages.length
      ? history.messages.map((item) => ({ role: item.role, content: item.content }))
      : [{ ...welcomeMessage }]
    await scrollMessagesToBottom('auto')
  } catch {
    // ignore - keep welcome message
  } finally {
    loadingHistory.value = false
  }
}

async function clearHistory() {
  if (loading.value || loadingHistory.value) {
    return
  }
  try {
    await ElMessageBox.confirm('只会清空当前登录账号的 AI 对话记录，其他账号不受影响。', '清空对话记录', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await clearQuizAiHistory()
    messages.value = [{ ...welcomeMessage }]
    ElMessage.success('对话记录已清空')
    await scrollMessagesToBottom('auto')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '对话历史清空失败')
    }
  }
}

function updateAssistantMessage(index: number, content: string) {
  messages.value[index] = {
    role: 'assistant',
    content,
  }
  void scrollMessagesToBottom('smooth')
}

function appendAssistantMessage(index: number, content: string) {
  const current = messages.value[index]?.content ?? ''
  updateAssistantMessage(index, `${current}${content}`)
}
</script>

<style scoped>
.quiz-ai-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.8fr);
  gap: 18px;
}

.quiz-ai-workbench :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.quiz-ai-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.quiz-ai-header p {
  margin: 10px 0 0;
  color: var(--gsmv-muted);
  line-height: 1.68;
}

.quiz-ai-header span {
  color: var(--gsmv-muted);
  font-size: 13px;
}

.quiz-ai-header__actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.quiz-ai-header__actions :deep(.el-button.is-circle) {
  width: 34px;
  height: 34px;
  border-color: rgba(75, 241, 186, 0.24);
  background: rgba(5, 35, 43, 0.58);
  color: rgba(226, 255, 246, 0.9);
}

.quiz-ai-messages {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 440px;
  max-height: 680px;
  overflow: auto;
  padding: 4px 8px 4px 0;
  scrollbar-width: thin;
}

.quiz-ai-messages::-webkit-scrollbar {
  width: 8px;
}

.quiz-ai-messages::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(79, 240, 181, 0.24);
}

.quiz-ai-message {
  position: relative;
  max-width: 88%;
  padding: 16px 18px;
  border-radius: 24px;
  border: 1px solid rgba(75, 241, 186, 0.18);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.015)),
    rgba(5, 31, 40, 0.72);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 14px 28px rgba(0, 9, 34, 0.12);
}

.quiz-ai-message--user {
  margin-left: auto;
  background:
    linear-gradient(135deg, rgba(79, 240, 181, 0.24), rgba(255, 189, 99, 0.12)),
    rgba(4, 34, 39, 0.74);
}

.quiz-ai-message__meta {
  margin-bottom: 8px;
  color: var(--gsmv-primary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.quiz-ai-message__content {
  line-height: 1.8;
  word-break: break-word;
}

.quiz-ai-message__content :deep(h3),
.quiz-ai-message__content :deep(h4) {
  margin: 12px 0 8px;
  font-size: 15px;
  color: #4ff0b5;
}

.quiz-ai-message__content :deep(strong) {
  color: #e8f7ff;
}

.quiz-ai-message__content :deep(ul) {
  margin: 8px 0;
  padding-left: 20px;
}

.quiz-ai-message__content :deep(li) {
  margin: 4px 0;
  line-height: 1.7;
}

.quiz-ai-message__content :deep(hr) {
  border: none;
  border-top: 1px solid rgba(75, 241, 186, 0.2);
  margin: 12px 0;
}

.quiz-ai-composer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quiz-ai-composer__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--gsmv-muted);
  font-size: 13px;
}

.quiz-ai-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
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

/* AI 出题入库结果 */
.gen-result {
  margin-top: 8px;
}

.gen-result__summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.gen-result__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.gen-result__item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
}

.gen-result__item--saved {
  background: rgba(79, 240, 181, 0.08);
  border: 1px solid rgba(79, 240, 181, 0.16);
}

.gen-result__item--dup {
  background: rgba(255, 200, 87, 0.06);
  border: 1px solid rgba(255, 200, 87, 0.14);
}

.gen-result__badge {
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(79, 240, 181, 0.2);
  color: #4ff0b5;
}

.gen-result__badge--dup {
  background: rgba(255, 200, 87, 0.2);
  color: #ffc857;
}

.gen-result__title {
  flex: 1;
  word-break: break-word;
}

.gen-result__dup-title {
  font-size: 12px;
  color: var(--gsmv-muted);
  margin-top: 4px;
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.prompt-chip {
  position: relative;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(75, 241, 186, 0.14);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02)),
    rgba(6, 31, 39, 0.68);
  color: var(--gsmv-text);
  text-align: left;
  line-height: 1.6;
  cursor: pointer;
  overflow: hidden;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.prompt-chip::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 14px;
  width: 7px;
  height: 7px;
  border-top: 2px solid rgba(145, 239, 255, 0.78);
  border-right: 2px solid rgba(145, 239, 255, 0.78);
  transform: translateY(-50%) rotate(45deg);
  opacity: 0;
  transition:
    opacity 0.18s ease,
    right 0.18s ease;
}

.prompt-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(160, 235, 245, 0.24);
  background: rgba(17, 74, 140, 0.46);
  box-shadow: 0 14px 28px rgba(2, 15, 44, 0.14);
}

.prompt-chip:hover::after {
  right: 12px;
  opacity: 1;
}

.guide-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.guide-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.guide-item__icon {
  font-size: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}

.guide-item strong {
  display: block;
  font-size: 14px;
  margin-bottom: 4px;
}

.guide-item p {
  margin: 0;
  color: var(--gsmv-muted);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .quiz-ai-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .quiz-ai-composer__actions,
  .quiz-ai-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .quiz-ai-message {
    max-width: 100%;
  }
}
</style>
