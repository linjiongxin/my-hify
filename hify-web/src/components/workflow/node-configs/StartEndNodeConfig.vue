<script setup lang="ts">
const config = defineModel<any>({ default: () => ({}) })
</script>

<template>
  <el-form-item label="错误分支">
    <el-input v-model="config.errorBranch" placeholder="错误时跳转的节点 ID（可选）" />
  </el-form-item>
</template>
