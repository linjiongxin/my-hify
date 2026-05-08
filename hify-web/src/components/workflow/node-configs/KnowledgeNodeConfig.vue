<script setup lang="ts">
const config = defineModel<any>({ default: () => ({}) })
</script>

<template>
  <el-form-item label="知识库 ID">
    <el-input-number v-model="config.knowledgeBaseId" :min="1" style="width: 100%" />
  </el-form-item>
  <el-form-item label="查询内容">
    <el-input
      v-model="config.query"
      placeholder="支持 ${varName} 引用变量"
    />
  </el-form-item>
  <el-form-item label="Top K">
    <el-input-number v-model="config.topK" :min="1" :max="20" :value-on-clear="5" style="width: 100%" />
  </el-form-item>
  <el-form-item label="相似度阈值">
    <el-slider v-model="config.threshold" :min="0" :max="1" :step="0.05" show-stops />
  </el-form-item>
  <el-form-item label="输出变量">
    <el-input v-model="config.outputVar" placeholder="如 result" />
  </el-form-item>
  <el-form-item label="错误分支">
    <el-input v-model="config.errorBranch" placeholder="错误时跳转的节点 ID（可选）" />
  </el-form-item>
</template>
