<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <div class="placeholder">
    <el-empty description="页面开发中">
      <template #description>
        <p>页面开发中</p>
        <p class="route-name">{{ route.name }}</p>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.placeholder {
  padding: 40px;
}

.route-name {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 8px;
}
</style>
