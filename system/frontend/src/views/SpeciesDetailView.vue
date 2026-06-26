<template>
  <div class="page-shell" />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  const id = route.params.id
  void router.replace(id ? `/vessels/${id}` : '/vessels')
})
</script>
