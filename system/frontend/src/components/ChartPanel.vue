<template>
  <div class="chart-shell">
    <div class="chart-shell__grid" />
    <div ref="chartRef" class="chart-panel" />
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  option: echarts.EChartsOption
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const render = () => {
  if (!chartRef.value) {
    return
  }
  chart ??= echarts.init(chartRef.value)
  chart.setOption(props.option, true)
}

onMounted(() => {
  render()
  window.addEventListener('resize', render)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', render)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.chart-shell {
  position: relative;
  min-height: 360px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.1), rgba(124, 60, 255, 0.08) 44%, rgba(32, 255, 159, 0.04)),
    linear-gradient(180deg, rgba(13, 18, 42, 0.58), rgba(5, 8, 22, 0.42));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.chart-shell::before {
  content: "";
  position: absolute;
  inset: -50%;
  background: conic-gradient(from 180deg, transparent, rgba(0, 229, 255, 0.18), transparent, rgba(124, 60, 255, 0.14), transparent);
  animation: chart-orbit 12s linear infinite;
  opacity: 0.72;
  pointer-events: none;
}

.chart-shell__grid {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(rgba(0, 229, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(124, 60, 255, 0.035) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.45), transparent 92%);
  pointer-events: none;
}

.chart-panel {
  position: relative;
  z-index: 2;
  min-height: 360px;
}

@keyframes chart-orbit {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .chart-shell::before {
    animation: none;
  }
}
</style>
