<script setup lang="ts">
const config = defineModel<any>({ default: () => ({}) })
</script>

<template>
  <el-form-item label="审批说明">
    <el-input
      v-model="config.prompt"
      type="textarea"
      :rows="4"
      placeholder="输入需要审批的内容说明"
    />
  </el-form-item>
  <el-form-item label="通过分支">
    <el-input v-model="config.approveBranch" placeholder="审批通过时跳转的节点 ID" />
  </el-form-item>
  <el-form-item label="拒绝分支">
    <el-input v-model="config.rejectBranch" placeholder="审批拒绝时跳转的节点 ID" />
  </el-form-item>
  <el-form-item label="错误分支">
    <el-input v-model="config.errorBranch" placeholder="错误时跳转的节点 ID（可选）" />
  </el-form-item>
</template>
