<script setup lang="ts">
defineProps<{ title: string; description?: string }>()
</script>

<template>
  <div class="placeholder">
    <h3>{{ title }}</h3>
    <p>{{ description ?? '页面开发中，等待后端联调' }}</p>
  </div>
</template>

<style scoped>
.placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}
</style>
