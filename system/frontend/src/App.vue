<template>
  <GlobalFx v-if="!isImmersiveRoute" />
  <RouterView />
</template>

<script setup lang="ts">
import GlobalFx from '@/components/GlobalFx.vue'
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

const route = useRoute()
const isImmersiveRoute = computed(() => route.path === '/marine-traffic')
</script>
