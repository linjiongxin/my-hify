<script setup lang="ts">
const config = defineModel<any>({ default: () => ({}) })
</script>

<template>
  <el-form-item label="模型">
    <el-input v-model="config.model" placeholder="如 gpt-4o-mini" />
  </el-form-item>
  <el-form-item label="Prompt">
    <el-input
      v-model="config.prompt"
      type="textarea"
      :rows="6"
      placeholder="输入提示词，支持 ${varName} 引用上下文变量"
    />
  </el-form-item>
  <el-form-item label="输出变量">
    <el-input v-model="config.outputVar" placeholder="如 result" />
  </el-form-item>
  <el-form-item label="错误分支">
    <el-input v-model="config.errorBranch" placeholder="错误时跳转的节点 ID（可选）" />
  </el-form-item>
</template>
