<template>
  <div class="page-shell assistant-page">
    <div class="assistant-grid">
      <el-card class="panel-card assistant-workbench" shadow="never">
        <template #header>
          <div class="assistant-header">
            <div>
              <strong>对话区</strong>
              <p>像平常聊天一样询问 AIS 数据、船舶档案、风险态势和航线变化。</p>
            </div>
            <div class="assistant-header__actions">
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

        <div ref="messagesContainerRef" class="assistant-messages">
          <div
            v-for="(item, index) in messages"
            :key="index"
            class="assistant-message"
            :class="item.role === 'user' ? 'assistant-message--user' : 'assistant-message--assistant'"
          >
            <div class="assistant-message__meta">
              <span>{{ item.role === 'user' ? '你' : 'AIS 态势分析助手' }}</span>
            </div>
            <div class="assistant-message__content">{{ item.content }}</div>
          </div>
        </div>

        <div class="assistant-prompts">
          <el-button v-for="prompt in quickPrompts" :key="prompt" plain size="small" @click="applyPrompt(prompt)">
            {{ prompt }}
          </el-button>
        </div>

        <div class="assistant-composer">
          <el-input
            v-model="input"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 8 }"
            resize="none"
            placeholder="例如：近 30 天谁录入的 AIS 记录最多？"
            @keydown.ctrl.enter.prevent="sendMessage()"
          />
          <div class="assistant-composer__actions">
            <span>按 Ctrl + Enter 发送</span>
            <el-button type="primary" :loading="loading" @click="sendMessage()">发送提问</el-button>
          </div>
        </div>
      </el-card>

      <div class="assistant-side">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <strong>数据包与口径</strong>
          </template>

          <template v-if="lastResponse">
            <div class="assistant-response-meta">
              <el-tag :type="lastResponse.cacheHit ? 'success' : 'primary'" effect="plain">
                {{ lastResponse.cacheHit ? '缓存命中' : '实时生成' }}
              </el-tag>
            </div>

            <div class="query-tags">
              <el-tag v-for="entry in structuredQueryEntries" :key="entry.label" effect="plain" round>
                {{ entry.label }}：{{ entry.value }}
              </el-tag>
            </div>

            <el-divider />

            <div class="assistant-side__section">
              <h3>已查询的数据包</h3>
              <ul v-if="lastResponse.highlights.length" class="assistant-list">
                <li v-for="item in lastResponse.highlights" :key="item">{{ item }}</li>
              </ul>
              <el-empty v-else description="本次回答没有返回摘要" />
            </div>

            <el-divider />

            <div class="assistant-side__section">
              <h3>参考证据</h3>
              <div v-if="lastResponse.evidence.length" class="evidence-list">
                <article v-for="item in lastResponse.evidence" :key="`${item.type}-${item.title}-${item.sourceId}`" class="evidence-item">
                  <div class="evidence-item__top">
                    <strong>{{ item.title || '数据线索' }}</strong>
                    <el-tag v-if="item.score !== undefined" effect="plain" round>评分 {{ formatScore(item.score) }}</el-tag>
                  </div>
                  <span>{{ item.description || item.type || '-' }}</span>
                  <small v-if="item.sourcePath">{{ item.sourcePath }}</small>
                </article>
              </div>
              <el-empty v-else description="这次回答没有返回额外证据" />
            </div>
          </template>

          <el-empty v-else description="提问后这里会展示本次查过的数据、统计口径和证据" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { askAiAssistantStream, clearAiAssistantHistory, getAiAssistantHistory } from '@/api/ai'
import type { AiAssistantChatResponse, AiAssistantMessage } from '@/types/gsmv'

const quickPrompts = [
  '昨晚湛江港附近有哪些船舶异常减速？',
  '近 7 天哪条航线的 AIS 记录最活跃？',
  '当前有哪些高风险船舶需要复核？',
  '近 30 天谁录入的 AIS 记录最多？',
]

const welcomeMessage: AiAssistantMessage = {
  role: 'assistant',
  content: '我是 ShipInsight AIS 态势分析助手。你可以问我船舶档案、AIS 动态、航线趋势、风险信号和数据录入情况。',
}

const messages = ref<AiAssistantMessage[]>([{ ...welcomeMessage }])
const input = ref('')
const loading = ref(false)
const loadingHistory = ref(false)
const lastResponse = ref<AiAssistantChatResponse | null>(null)
const messagesContainerRef = ref<HTMLDivElement | null>(null)

const structuredQueryEntries = computed(() => {
  if (!lastResponse.value) {
    return []
  }
  const query = lastResponse.value.structuredQuery
  const entries = [
    { label: '意图', value: query.intent },
    { label: '地点', value: query.locationKeyword || '' },
    { label: '航线/海域', value: query.routeKeyword || '' },
    { label: '船舶/MMSI', value: query.vesselKeyword || '' },
    { label: '风险等级', value: query.riskLevel || '' },
    { label: '航行状态', value: query.navigationStatus || '' },
    { label: '近年范围', value: query.yearsBack ? `${query.yearsBack} 年` : '' },
    { label: '近天范围', value: query.recentDays ? `${query.recentDays} 天` : '' },
    { label: '日期', value: query.dateKeyword || '' },
    { label: '指标', value: query.metric || '' },
    { label: '分组', value: query.groupBy || '' },
    { label: '开始时间', value: query.observedFrom || '' },
    { label: '结束时间', value: query.observedTo || '' },
    { label: '趋势分析', value: query.includeTrend ? '是' : '' },
    { label: '风险筛选', value: query.riskOnly ? '是' : '' },
  ]
  return entries.filter((item) => item.value)
})

watch(
  messages,
  () => {
    void nextTick(scrollToBottom)
  },
  { deep: true },
)

onMounted(() => {
  void loadHistory()
})

function applyPrompt(prompt: string) {
  input.value = prompt
  void sendMessage(prompt)
}

async function loadHistory(showToast = false) {
  loadingHistory.value = true
  try {
    const history = await getAiAssistantHistory()
    messages.value = history.messages.length ? history.messages.map((item) => ({ role: item.role, content: item.content })) : [{ ...welcomeMessage }]
    lastResponse.value = history.lastResponse || null
    if (showToast) {
      ElMessage.success('历史记录已刷新')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '历史记录加载失败')
  } finally {
    loadingHistory.value = false
  }
}

async function sendMessage(prefilled?: string) {
  const content = (prefilled || input.value).trim()
  if (!content || loading.value) {
    return
  }

  const history = messages.value.filter((item) => item.content !== welcomeMessage.content)
  messages.value.push({ role: 'user', content })
  input.value = ''
  loading.value = true
  const assistantMessageIndex = messages.value.push({ role: 'assistant', content: '正在检索系统数据...' }) - 1

  try {
    await askAiAssistantStream({ message: content, history }, (event) => {
      if (event.type === 'status' && event.content) {
        updateAssistantMessage(assistantMessageIndex, event.content)
      }
      if (event.type === 'delta' && event.content) {
        appendAssistantMessage(assistantMessageIndex, event.content)
      }
      if (event.type === 'final' && event.response) {
        lastResponse.value = event.response
        updateAssistantMessage(assistantMessageIndex, event.response.answer)
      }
      if (event.type === 'error') {
        throw new Error(event.content || '智能助手暂时不可用')
      }
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智能助手暂时不可用')
    updateAssistantMessage(assistantMessageIndex, '这次回答失败了，请稍后再试，或换一种问法。')
  } finally {
    loading.value = false
  }
}

async function clearHistory() {
  if (loading.value || loadingHistory.value) {
    return
  }
  try {
    await ElMessageBox.confirm('只会清空当前登录账号的智能分析对话记录。', '清空对话记录', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await clearAiAssistantHistory()
    messages.value = [{ ...welcomeMessage }]
    lastResponse.value = null
    ElMessage.success('对话记录已清空')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '清空对话失败')
    }
  }
}

function updateAssistantMessage(index: number, content: string) {
  messages.value[index] = { role: 'assistant', content }
}

function appendAssistantMessage(index: number, content: string) {
  const current = messages.value[index]?.content || ''
  messages.value[index] = { role: 'assistant', content: current + content }
}

function scrollToBottom() {
  const container = messagesContainerRef.value
  if (container) {
    container.scrollTop = container.scrollHeight
  }
}

function formatScore(value: number) {
  return `${Math.round(value * 100)}%`
}
</script>

<style scoped>
.assistant-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.assistant-header,
.assistant-header__actions,
.assistant-composer__actions,
.evidence-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assistant-header p {
  margin: 6px 0 0;
  color: var(--gsmv-muted);
}

.assistant-messages {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 360px;
  max-height: 560px;
  overflow-y: auto;
  padding: 8px 4px 16px;
}

.assistant-message {
  max-width: 78%;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(142, 206, 255, 0.16);
  background: rgba(255, 255, 255, 0.04);
  white-space: pre-wrap;
  line-height: 1.7;
}

.assistant-message--user {
  align-self: flex-end;
  background: rgba(64, 158, 255, 0.14);
}

.assistant-message--assistant {
  align-self: flex-start;
}

.assistant-message__meta {
  margin-bottom: 6px;
  color: var(--gsmv-muted);
  font-size: 12px;
}

.assistant-prompts,
.query-tags,
.evidence-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.assistant-composer {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.assistant-composer__actions span {
  color: var(--gsmv-muted);
  font-size: 13px;
}

.assistant-response-meta {
  margin-bottom: 12px;
}

.assistant-side__section h3 {
  margin: 0 0 12px;
  font-size: 15px;
}

.assistant-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}

.evidence-item {
  width: 100%;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(142, 206, 255, 0.16);
  background: rgba(255, 255, 255, 0.04);
}

.evidence-item span,
.evidence-item small {
  display: block;
  margin-top: 6px;
  color: var(--gsmv-muted);
}

@media (max-width: 1080px) {
  .assistant-grid {
    grid-template-columns: 1fr;
  }

  .assistant-message {
    max-width: 100%;
  }
}
</style>
