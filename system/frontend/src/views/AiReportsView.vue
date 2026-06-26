<template>
  <div class="page-shell ai-report-page">
    <section class="page-hero ai-report-hero">
      <div class="ai-report-hero__copy">
        <h2>分析报告</h2>
        <p>把近期 AIS 动态、重点航运节点、船舶风险信号和潜在异常汇总成可归档的交通态势报告。</p>
      </div>
      <el-space class="ai-report-hero__actions" wrap>
        <el-select v-model="reportForm.reportType" class="report-type" aria-label="报告类型" :disabled="generating">
          <el-option label="周报" value="WEEKLY" />
          <el-option label="月报" value="MONTHLY" />
          <el-option label="专题报告" value="CUSTOM" />
        </el-select>
        <el-select
          v-model="reportForm.days"
          class="day-filter"
          aria-label="统计范围"
          :disabled="generating || reportForm.customRange"
        >
          <el-option label="近 7 天" :value="7" />
          <el-option label="近 30 天" :value="30" />
          <el-option label="近 90 天" :value="90" />
        </el-select>
        <el-button
          class="custom-time-button"
          :type="reportForm.customRange ? 'success' : 'default'"
          :icon="Calendar"
          :disabled="generating"
          @click="toggleCustomRange"
        >
          自定义时间
        </el-button>
        <el-date-picker
          v-if="reportForm.customRange"
          v-model="reportForm.observedRange"
          class="report-range-picker"
          type="datetimerange"
          unlink-panels
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :default-time="rangeDefaultTime"
          :disabled="generating"
        />
        <el-button type="primary" :icon="DocumentAdd" :loading="generating" @click="generateReport">
          生成报告
        </el-button>
      </el-space>
    </section>

    <section class="ai-report-grid">
      <el-card class="panel-card report-history-card" shadow="never">
        <template #header>
          <div class="panel-header">
            <strong>报告历史</strong>
            <el-button text type="primary" :icon="Refresh" :loading="loading" @click="loadReports()">
              刷新
            </el-button>
          </div>
        </template>

        <el-table
          v-if="rows.length || loading"
          :data="rows"
          v-loading="loading"
          stripe
          empty-text="暂无报告"
          :row-class-name="historyRowClassName"
          @row-click="openDetail"
        >
          <el-table-column prop="title" label="报告标题" min-width="230" show-overflow-tooltip />
          <el-table-column label="类型" width="108">
            <template #default="{ row }">
              <el-tag effect="dark">{{ reportTypeLabel(row.reportType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="范围" min-width="190" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="range-text">{{ reportRangeLabel(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="creatorName" label="生成人" width="116" show-overflow-tooltip />
          <el-table-column label="生成时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">查看</el-button>
              <el-button
                link
                type="success"
                :icon="Download"
                :loading="isExporting(row.id)"
                @click.stop="exportPdf(row.id)"
              >
                PDF
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-else class="report-empty-state">
          <div class="report-empty-state__icon">
            <el-icon><DocumentAdd /></el-icon>
          </div>
          <strong>还没有分析报告</strong>
          <p>生成一份 AIS 交通态势报告后，历史记录会显示在这里。</p>
          <el-button type="primary" :icon="DocumentAdd" :loading="generating" @click="generateReport">
            生成第一份报告
          </el-button>
        </div>

        <div v-if="pagination.total > 0" class="table-footer">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            layout="total, prev, pager, next"
            :total="pagination.total"
            @current-change="loadReports()"
          />
        </div>
      </el-card>

      <el-card class="panel-card ai-report-detail" shadow="never">
        <template #header>
          <div class="panel-header">
            <strong>报告预览</strong>
            <el-button
              v-if="detail"
              text
              type="success"
              :icon="Download"
              :loading="isExporting(detail.id)"
              @click="exportPdf(detail.id)"
            >
              导出 PDF
            </el-button>
          </div>
        </template>

        <div v-if="detailLoading" class="report-detail-loading" v-loading="detailLoading" />

        <template v-else-if="detail">
          <div class="report-cover">
            <div class="report-cover__meta">
              <span>{{ reportTypeLabel(detail.reportType) }} / {{ detailRangeLabel }}</span>
              <span v-if="detail.metrics?.latestDatasetDate">最新数据集 {{ detail.metrics.latestDatasetDate }}</span>
            </div>
            <h3>{{ detail.title }}</h3>
            <p>{{ detail.summary }}</p>
            <div v-if="periodLabel" class="report-cover__period">{{ periodLabel }}</div>
          </div>

          <div v-if="metricCards.length" class="report-metric-grid">
            <div v-for="item in metricCards" :key="item.label" class="report-metric">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.hint }}</em>
            </div>
          </div>

          <div v-if="detail.metrics" class="risk-signal-band" :class="{ 'is-clean': detail.metrics.riskSignalCount === 0 }">
            <el-icon><Warning /></el-icon>
            <div>
              <strong>{{ riskSignalTitle }}</strong>
              <span>{{ riskSignalText }}</span>
            </div>
          </div>

          <div class="report-sections">
            <article>
              <h4>重点发现</h4>
              <ul>
                <li v-for="item in detail.highlights" :key="item">{{ item }}</li>
              </ul>
            </article>
            <article>
              <h4>风险提示</h4>
              <ul>
                <li v-for="item in detail.risks" :key="item">{{ item }}</li>
              </ul>
            </article>
            <article>
              <h4>建议行动</h4>
              <ul>
                <li v-for="item in detail.recommendations" :key="item">{{ item }}</li>
              </ul>
            </article>
            <article v-if="detail.metrics?.topDates?.length" class="report-rank-section">
              <h4>AIS 日期峰值</h4>
              <div class="report-rank-list">
                <div v-for="item in detail.metrics.topDates.slice(0, 5)" :key="item.datasetDate" class="report-rank-row">
                  <span>{{ item.datasetDate }}</span>
                  <strong>{{ formatNumber(item.recordCount) }}</strong>
                </div>
              </div>
            </article>
            <article class="report-evidence-section">
              <h4>数据依据</h4>
              <div class="report-evidence-list">
                <div v-for="item in detail.evidence" :key="item" class="report-evidence-card">
                  <span>Evidence</span>
                  <p>{{ item }}</p>
                </div>
              </div>
            </article>
          </div>
        </template>

        <div v-else class="report-empty-state report-empty-state--detail">
          <div class="report-empty-state__icon">
            <el-icon><DocumentAdd /></el-icon>
          </div>
          <strong>选择一份报告查看详情</strong>
          <p>也可以先生成新的 AIS 交通态势报告。</p>
          <el-button type="primary" :icon="DocumentAdd" :loading="generating" @click="generateReport">
            生成报告
          </el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, DocumentAdd, Download, Refresh, View, Warning } from '@element-plus/icons-vue'
import {
  downloadAiReportPdf,
  fetchAiReportDetail,
  fetchAiReports,
  generateAiReport,
} from '@/api/aiReports'
import { triggerBlobDownload } from '@/utils/download'
import type { AiReportDetailView, AiReportView } from '@/types/gsmv'

const loading = ref(false)
const detailLoading = ref(false)
const generating = ref(false)
const rows = ref<AiReportView[]>([])
const detail = ref<AiReportDetailView | null>(null)
const selectedId = ref<number | null>(null)
const exportingIds = ref<number[]>([])
const rangeDefaultTime: [Date, Date] = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59),
]

const reportForm = reactive({
  reportType: 'MONTHLY',
  days: 30,
  customRange: false,
  observedRange: [] as string[],
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const metricCards = computed(() => {
  const metrics = detail.value?.metrics
  if (!metrics) {
    return []
  }
  return [
    { label: 'AIS 记录', value: formatNumber(metrics.totalRecords), hint: '窗口内总量' },
    { label: '唯一船舶', value: formatNumber(metrics.uniqueVesselCount), hint: 'MMSI 去重' },
    { label: '风险信号', value: formatNumber(metrics.riskSignalCount), hint: '低速/停泊/异常' },
    { label: '异常备注', value: formatNumber(metrics.abnormalNoteCount), hint: '备注命中' },
  ]
})

const periodLabel = computed(() => {
  const periodStart = detail.value?.metrics?.periodStart || detail.value?.periodStart
  const periodEnd = detail.value?.metrics?.periodEnd || detail.value?.periodEnd
  if (!periodStart || !periodEnd) {
    return ''
  }
  return `统计窗口：${formatDateTime(periodStart)} - ${formatDateTime(periodEnd)}`
})

const detailRangeLabel = computed(() => {
  if (!detail.value) {
    return ''
  }
  return reportRangeLabel(detail.value)
})

const riskSignalTitle = computed(() => {
  const count = detail.value?.metrics?.riskSignalCount ?? 0
  return count > 0 ? `发现 ${formatNumber(count)} 条风险信号` : '未发现显性风险信号'
})

const riskSignalText = computed(() => {
  const metrics = detail.value?.metrics
  if (!metrics) {
    return ''
  }
  return `低速 ${formatNumber(metrics.lowSpeedCount)} 条，停泊/近静止 ${formatNumber(metrics.stoppedCount)} 条，异常备注 ${formatNumber(metrics.abnormalNoteCount)} 条。`
})

function reportTypeLabel(value: string) {
  switch (value) {
    case 'WEEKLY':
      return '周报'
    case 'CUSTOM':
      return '专题报告'
    default:
      return '月报'
  }
}

async function loadReports() {
  loading.value = true
  try {
    const pageData = await fetchAiReports({ page: pagination.page, size: pagination.size })
    rows.value = pageData.items
    pagination.total = pageData.total
    if (rows.value.length && (!detail.value || !rows.value.some((row) => row.id === selectedId.value))) {
      await openDetail(rows.value[0])
    }
    if (!rows.value.length) {
      detail.value = null
      selectedId.value = null
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 报告加载失败')
  } finally {
    loading.value = false
  }
}

async function generateReport() {
  const payload = buildGeneratePayload()
  if (!payload) {
    return
  }
  generating.value = true
  try {
    const generated = await generateAiReport(payload)
    detail.value = generated
    selectedId.value = generated.id
    pagination.page = 1
    await loadReports()
    ElMessage.success('AIS 分析报告已生成')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 报告生成失败')
  } finally {
    generating.value = false
  }
}

function buildGeneratePayload() {
  if (!reportForm.customRange) {
    return {
      reportType: reportForm.reportType,
      days: reportForm.days,
    }
  }
  const [observedFrom, observedTo] = reportForm.observedRange
  if (!observedFrom || !observedTo) {
    ElMessage.warning('请选择完整的开始和结束时间')
    return null
  }
  return {
    reportType: reportForm.reportType,
    days: calculateRangeDays(observedFrom, observedTo),
    observedFrom,
    observedTo,
  }
}

function toggleCustomRange() {
  reportForm.customRange = !reportForm.customRange
  if (!reportForm.customRange) {
    reportForm.observedRange = []
  }
}

async function openDetail(row: AiReportView) {
  selectedId.value = row.id
  detailLoading.value = true
  try {
    detail.value = await fetchAiReportDetail(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '报告详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function exportPdf(id: number) {
  exportingIds.value = [...exportingIds.value, id]
  try {
    const { blob, fileName } = await downloadAiReportPdf(id)
    triggerBlobDownload(blob, fileName)
    ElMessage.success('AI 报告 PDF 已开始下载')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI 报告导出失败')
  } finally {
    exportingIds.value = exportingIds.value.filter((item) => item !== id)
  }
}

function isExporting(id: number) {
  return exportingIds.value.includes(id)
}

function historyRowClassName({ row }: { row: AiReportView }) {
  return row.id === selectedId.value ? 'is-selected-report' : ''
}

function reportRangeLabel(row: Pick<AiReportView, 'days' | 'periodStart' | 'periodEnd'>) {
  if (row.periodStart && row.periodEnd) {
    return `${formatDateTime(row.periodStart)} 至 ${formatDateTime(row.periodEnd)}`
  }
  return `近 ${row.days} 天`
}

function calculateRangeDays(observedFrom: string, observedTo: string) {
  const start = new Date(observedFrom)
  const end = new Date(observedTo)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start > end) {
    return reportForm.days
  }
  const startDay = new Date(start.getFullYear(), start.getMonth(), start.getDate()).getTime()
  const endDay = new Date(end.getFullYear(), end.getMonth(), end.getDate()).getTime()
  return Math.max(1, Math.floor((endDay - startDay) / 86_400_000) + 1)
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function formatNumber(value?: number | null) {
  return Number(value || 0).toLocaleString('zh-CN')
}

onMounted(() => {
  void loadReports()
})
</script>

<style scoped>
.ai-report-hero {
  align-items: center;
}

.ai-report-hero__copy {
  min-width: 280px;
}

.ai-report-hero__actions {
  position: relative;
  z-index: 1;
}

.report-type,
.day-filter {
  width: 140px;
}

.custom-time-button {
  min-width: 118px;
}

.report-range-picker {
  width: 360px;
  max-width: calc(100vw - 48px);
}

.range-text {
  white-space: nowrap;
}

.ai-report-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 0.92fr);
  gap: 18px;
  align-items: start;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-history-card :deep(.is-selected-report td.el-table__cell) {
  background: rgba(0, 229, 255, 0.12) !important;
}

.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.report-empty-state {
  display: grid;
  justify-items: center;
  gap: 12px;
  min-height: 280px;
  padding: 38px 20px;
  text-align: center;
  color: var(--gsmv-muted);
}

.report-empty-state--detail {
  min-height: 420px;
  align-content: center;
}

.report-empty-state__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 24px;
  border: 1px solid rgba(0, 229, 255, 0.24);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.16), rgba(124, 60, 255, 0.12)),
    rgba(255, 255, 255, 0.06);
  color: var(--gsmv-primary);
  font-size: 30px;
}

.report-empty-state strong {
  color: var(--gsmv-text);
  font-size: 18px;
}

.report-empty-state p {
  max-width: 320px;
  margin: 0;
  line-height: 1.7;
}

.report-detail-loading {
  min-height: 520px;
}

.report-cover {
  padding: 24px;
  border-radius: 24px;
  border: 1px solid rgba(173, 239, 255, 0.16);
  background:
    linear-gradient(145deg, rgba(72, 219, 251, 0.16), rgba(13, 66, 132, 0.36)),
    rgba(5, 28, 70, 0.72);
}

.report-cover__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  color: var(--gsmv-primary);
  font-size: 13px;
  font-weight: 700;
}

.report-cover h3 {
  margin: 12px 0;
  font-size: 24px;
  line-height: 1.28;
}

.report-cover p {
  margin: 0;
  color: var(--gsmv-muted);
  line-height: 1.8;
}

.report-cover__period {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(173, 239, 255, 0.12);
  color: rgba(232, 248, 255, 0.82);
  font-size: 13px;
}

.report-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.report-metric {
  min-width: 0;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(173, 239, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.03)),
    rgba(4, 23, 60, 0.56);
}

.report-metric span,
.report-metric em {
  display: block;
  color: var(--gsmv-muted);
  font-size: 12px;
  font-style: normal;
}

.report-metric strong {
  display: block;
  margin: 8px 0 4px;
  overflow-wrap: anywhere;
  color: #f3fdff;
  font-size: 22px;
  line-height: 1.1;
}

.risk-signal-band {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-top: 14px;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 79, 106, 0.24);
  background:
    linear-gradient(135deg, rgba(255, 79, 106, 0.16), rgba(124, 60, 255, 0.09)),
    rgba(8, 22, 54, 0.66);
}

.risk-signal-band.is-clean {
  border-color: rgba(32, 255, 159, 0.24);
  background:
    linear-gradient(135deg, rgba(32, 255, 159, 0.12), rgba(0, 229, 255, 0.08)),
    rgba(8, 22, 54, 0.66);
}

.risk-signal-band .el-icon {
  margin-top: 2px;
  color: var(--gsmv-warm);
}

.risk-signal-band.is-clean .el-icon {
  color: var(--gsmv-accent);
}

.risk-signal-band strong,
.risk-signal-band span {
  display: block;
}

.risk-signal-band strong {
  color: #fff;
}

.risk-signal-band span {
  margin-top: 4px;
  color: var(--gsmv-muted);
  line-height: 1.7;
}

.report-sections {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.report-sections article {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(173, 239, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02)),
    rgba(4, 23, 60, 0.68);
}

.report-sections h4 {
  margin: 0 0 10px;
}

.report-sections ul {
  margin: 0;
  padding-left: 18px;
  color: var(--gsmv-muted);
  line-height: 1.8;
}

.report-rank-list,
.report-evidence-list {
  display: grid;
  gap: 10px;
}

.report-rank-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(4, 18, 52, 0.45);
}

.report-rank-row span {
  color: var(--gsmv-muted);
}

.report-rank-row strong {
  color: var(--gsmv-primary);
}

.report-evidence-section {
  background:
    linear-gradient(135deg, rgba(76, 214, 255, 0.12), rgba(7, 25, 70, 0.72)),
    rgba(8, 33, 78, 0.54);
}

.report-evidence-card {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(125, 211, 252, 0.16);
  background: rgba(4, 18, 52, 0.52);
}

.report-evidence-card span {
  color: #70f3ff;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.report-evidence-card p {
  margin: 6px 0 0;
  color: var(--gsmv-muted);
  line-height: 1.75;
}

@media (max-width: 1280px) {
  .report-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .ai-report-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .ai-report-hero__actions,
  .report-type,
  .day-filter,
  .custom-time-button,
  .report-range-picker {
    width: 100%;
  }

  .report-metric-grid {
    grid-template-columns: 1fr;
  }

  .report-cover {
    padding: 20px;
  }
}
</style>
