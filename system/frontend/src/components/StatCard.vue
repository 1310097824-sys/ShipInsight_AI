<template>
  <article class="stat-card">
    <div class="stat-card__halo stat-card__halo--one" />
    <div class="stat-card__halo stat-card__halo--two" />
    <div class="stat-card__content">
      <div class="stat-card__eyebrow">{{ eyebrow }}</div>
      <div class="stat-card__value">{{ value }}</div>
      <div class="stat-card__hint">{{ hint }}</div>
    </div>
  </article>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string
  value: string | number
  hint: string
}>()
</script>

<style scoped>
.stat-card {
  position: relative;
  min-height: 172px;
  padding: 22px 22px 20px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background:
    linear-gradient(135deg, rgba(0, 229, 255, 0.14), rgba(124, 60, 255, 0.1) 48%, rgba(32, 255, 159, 0.05)),
    linear-gradient(180deg, rgba(13, 18, 42, 0.86), rgba(5, 8, 22, 0.92)),
    rgba(255, 255, 255, 0.065);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 0 28px rgba(0, 229, 255, 0.12),
    0 18px 44px rgba(0, 4, 18, 0.32);
  overflow: hidden;
  transition:
    transform 0.22s ease,
    border-color 0.22s ease,
    box-shadow 0.22s ease;
}

.stat-card:hover {
  border-color: rgba(0, 229, 255, 0.38);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 0 34px rgba(0, 229, 255, 0.18),
    0 24px 58px rgba(0, 4, 18, 0.42);
}

.stat-card__content {
  position: relative;
  z-index: 1;
}

.stat-card__halo {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.stat-card__halo--one {
  inset: auto -8% -28% auto;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.22), transparent 72%);
}

.stat-card__halo--two {
  inset: -20% auto auto -12%;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(124, 60, 255, 0.18), transparent 74%);
}

.stat-card__eyebrow {
  color: var(--gsmv-muted);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.stat-card__value {
  margin: 14px 0 8px;
  font-size: 42px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: transparent;
  background: linear-gradient(92deg, #ffffff, var(--gsmv-primary), var(--gsmv-accent));
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow: 0 0 24px rgba(0, 229, 255, 0.16);
}

.stat-card__hint {
  color: var(--gsmv-muted);
  line-height: 1.72;
}
</style>
