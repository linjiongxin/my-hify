<script setup lang="ts">
import { computed } from 'vue'

const config = defineModel<any>({ default: () => ({}) })

const params = computed({
  get() {
    const p = config.value.params
    if (p && typeof p === 'object' && !Array.isArray(p)) {
      return Object.entries(p).map(([key, value]) => ({ key, value: String(value) }))
    }
    return []
  },
  set(val) {
    const record: Record<string, unknown> = {}
    val.forEach((item: { key: string; value: string }) => {
      if (item.key.trim()) {
        record[item.key.trim()] = item.value
      }
    })
    config.value = { ...config.value, params: record }
  },
})

function addParam() {
  params.value = [...params.value, { key: '', value: '' }]
}

function removeParam(index: number) {
  const arr = [...params.value]
  arr.splice(index, 1)
  params.value = arr
}
</script>

<template>
  <el-form-item label="工具名称">
    <el-input v-model="config.toolName" placeholder="如 search、calculator" />
  </el-form-item>
  <el-form-item label="参数">
    <div class="kv-list">
      <div v-for="(item, index) in params" :key="index" class="kv-row">
        <el-input v-model="item.key" placeholder="参数名" />
        <el-input v-model="item.value" placeholder="参数值" />
        <el-button type="danger" text @click="removeParam(index)">删除</el-button>
      </div>
      <el-button type="primary" text @click="addParam">+ 添加参数</el-button>
    </div>
  </el-form-item>
  <el-form-item label="输出变量">
    <el-input v-model="config.outputVar" placeholder="如 result" />
  </el-form-item>
  <el-form-item label="错误分支">
    <el-input v-model="config.errorBranch" placeholder="错误时跳转的节点 ID（可选）" />
  </el-form-item>
</template>

<style scoped>
.kv-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.kv-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kv-row .el-input {
  flex: 1;
}
</style>
