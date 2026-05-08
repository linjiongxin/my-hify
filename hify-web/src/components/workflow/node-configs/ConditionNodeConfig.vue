<script setup lang="ts">
const config = defineModel<any>({ default: () => ({}) })
</script>

<template>
  <el-form-item label="条件表达式">
    <el-input
      v-model="config.expression"
      placeholder="如 ${intent} == '售前'"
    />
  </el-form-item>
  <el-form-item label="真分支">
    <el-input v-model="config.trueBranch" placeholder="条件为真时跳转的节点 ID" />
  </el-form-item>
  <el-form-item label="假分支">
    <el-input v-model="config.falseBranch" placeholder="条件为假时跳转的节点 ID" />
  </el-form-item>
  <el-form-item label="错误分支">
    <el-input v-model="config.errorBranch" placeholder="错误时跳转的节点 ID（可选）" />
  </el-form-item>
</template>
