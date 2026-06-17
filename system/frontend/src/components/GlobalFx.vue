<template>
  <div class="cyber-fx">
    <div class="cyber-fx__stage" aria-hidden="true">
      <canvas ref="canvasRef" class="cyber-fx__canvas" />
      <div class="cyber-fx__cursor" :style="cursorStyle" />
    </div>
    <div class="cyber-fx__progress" aria-hidden="true">
      <span :style="{ width: `${scrollProgress}%` }" />
    </div>
    <button
      class="cyber-fx__backtop"
      :class="{ 'is-visible': showBackTop }"
      type="button"
      aria-label="Back to top"
      @click="scrollToTop"
    >
      <el-icon><ArrowUp /></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ArrowUp } from '@element-plus/icons-vue'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  tone: string
}

const canvasRef = ref<HTMLCanvasElement>()
const scrollProgress = ref(0)
const showBackTop = ref(false)
const cursorX = ref(-999)
const cursorY = ref(-999)

const cursorStyle = computed(() => ({
  transform: `translate3d(${cursorX.value}px, ${cursorY.value}px, 0) translate(-50%, -50%)`,
}))

let ctx: CanvasRenderingContext2D | null = null
let particles: Particle[] = []
let rafId = 0
let resizeObserver: ResizeObserver | null = null
let mutationObserver: MutationObserver | null = null
let revealObserver: IntersectionObserver | null = null
let enhanceTimer = 0

const revealSelector = [
  '.page-hero',
  '.panel-card',
  '.stat-card',
  '.chart-shell',
  '.report-map-panel',
  '.dashboard-window',
  '.dashboard-focus-item',
  '.dashboard-route-card',
  '.dashboard-hero__summary-card',
  '.app-header',
  '.app-toolbar',
  '.brand-card',
  '.nav-card',
  '.auth-visual',
  '.auth-card',
].join(',')

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function updateScrollState() {
  const maxScroll = document.documentElement.scrollHeight - window.innerHeight
  scrollProgress.value = maxScroll > 0 ? Math.min(100, Math.max(0, (window.scrollY / maxScroll) * 100)) : 0
  showBackTop.value = window.scrollY > 560
}

function updateCursor(event: PointerEvent) {
  cursorX.value = event.clientX
  cursorY.value = event.clientY
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: prefersReducedMotion() ? 'auto' : 'smooth' })
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) {
    return
  }

  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const width = window.innerWidth
  const height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx = canvas.getContext('2d')
  ctx?.setTransform(dpr, 0, 0, dpr, 0, 0)
  createParticles(width, height)
}

function createParticles(width = window.innerWidth, height = window.innerHeight) {
  const tones = ['rgba(0, 229, 255, 0.62)', 'rgba(124, 60, 255, 0.5)', 'rgba(32, 255, 159, 0.52)']
  const count = Math.min(120, Math.max(38, Math.floor((width * height) / 18000)))
  particles = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.42,
    vy: (Math.random() - 0.5) * 0.42,
    r: Math.random() * 1.8 + 0.7,
    tone: tones[Math.floor(Math.random() * tones.length)],
  }))
}

function drawParticles() {
  if (!ctx || prefersReducedMotion()) {
    return
  }

  const width = window.innerWidth
  const height = window.innerHeight
  ctx.clearRect(0, 0, width, height)

  particles.forEach((particle) => {
    particle.x += particle.vx
    particle.y += particle.vy

    if (particle.x < 0 || particle.x > width) particle.vx *= -1
    if (particle.y < 0 || particle.y > height) particle.vy *= -1

    ctx?.beginPath()
    ctx?.arc(particle.x, particle.y, particle.r, 0, Math.PI * 2)
    if (ctx) ctx.fillStyle = particle.tone
    ctx?.fill()
  })

  for (let index = 0; index < particles.length; index += 1) {
    for (let next = index + 1; next < particles.length; next += 1) {
      const left = particles[index]
      const right = particles[next]
      const dx = left.x - right.x
      const dy = left.y - right.y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < 132 && ctx) {
        ctx.beginPath()
        ctx.moveTo(left.x, left.y)
        ctx.lineTo(right.x, right.y)
        ctx.strokeStyle = `rgba(0, 229, 255, ${(1 - distance / 132) * 0.34})`
        ctx.lineWidth = 0.6
        ctx.stroke()
      }
    }
  }

  rafId = window.requestAnimationFrame(drawParticles)
}

function enhanceElements() {
  document.querySelectorAll<HTMLElement>(revealSelector).forEach((element) => {
    element.classList.add('fx-reveal')
    const rect = element.getBoundingClientRect()
    const isInView = rect.bottom >= 0 && rect.top <= window.innerHeight
    if (isInView) {
      element.classList.add('is-revealed')
      return
    }
    revealObserver?.observe(element)
  })
}

function scheduleEnhance() {
  window.clearTimeout(enhanceTimer)
  enhanceTimer = window.setTimeout(enhanceElements, 80)
}

onMounted(() => {
  document.body.classList.add('cyber-ui')

  revealObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed')
          revealObserver?.unobserve(entry.target)
        }
      })
    },
    { threshold: 0.08, rootMargin: '0px 0px -6% 0px' },
  )

  resizeCanvas()
  if (!prefersReducedMotion()) {
    drawParticles()
  }

  window.addEventListener('resize', resizeCanvas)
  window.addEventListener('scroll', updateScrollState, { passive: true })
  window.addEventListener('pointermove', updateCursor, { passive: true })
  updateScrollState()
  scheduleEnhance()

  const appRoot = document.getElementById('app')
  if (appRoot) {
    mutationObserver = new MutationObserver(scheduleEnhance)
    mutationObserver.observe(appRoot, { childList: true, subtree: true })
    resizeObserver = new ResizeObserver(scheduleEnhance)
    resizeObserver.observe(appRoot)
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('cyber-ui')
  window.cancelAnimationFrame(rafId)
  window.clearTimeout(enhanceTimer)
  window.removeEventListener('resize', resizeCanvas)
  window.removeEventListener('scroll', updateScrollState)
  window.removeEventListener('pointermove', updateCursor)
  mutationObserver?.disconnect()
  resizeObserver?.disconnect()
  revealObserver?.disconnect()
})
</script>

<style scoped>
.cyber-fx {
  position: relative;
  z-index: 0;
}

.cyber-fx__stage {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.cyber-fx__canvas {
  display: block;
  width: 100vw;
  height: 100vh;
  opacity: 0.58;
  mix-blend-mode: screen;
}

.cyber-fx__cursor {
  position: fixed;
  left: 0;
  top: 0;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.2), rgba(124, 60, 255, 0.1) 38%, transparent 68%);
  filter: blur(10px);
  opacity: 0.85;
  mix-blend-mode: screen;
  pointer-events: none;
}

.cyber-fx__progress {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  height: 3px;
  pointer-events: none;
}

.cyber-fx__progress span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--gsmv-primary), var(--gsmv-secondary), var(--gsmv-accent));
  box-shadow: 0 0 18px rgba(0, 229, 255, 0.72);
}

.cyber-fx__backtop {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 90;
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid rgba(0, 229, 255, 0.28);
  border-radius: 16px;
  color: var(--gsmv-text);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.16), rgba(124, 60, 255, 0.18)),
    rgba(8, 12, 28, 0.72);
  box-shadow: 0 0 32px rgba(0, 229, 255, 0.18);
  backdrop-filter: blur(18px);
  opacity: 0;
  transform: translateY(12px) scale(0.94);
  pointer-events: none;
  transition:
    opacity 0.2s ease,
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.cyber-fx__backtop.is-visible {
  opacity: 1;
  transform: translateY(0) scale(1);
  pointer-events: auto;
}

.cyber-fx__backtop:hover {
  border-color: rgba(32, 255, 159, 0.4);
  box-shadow: 0 0 38px rgba(32, 255, 159, 0.22);
  transform: translateY(-2px) scale(1.02);
}

@media (prefers-reduced-motion: reduce) {
  .cyber-fx__canvas,
  .cyber-fx__cursor {
    display: none;
  }

  .cyber-fx__backtop,
  .cyber-fx__progress span {
    transition: none;
  }
}
</style>
