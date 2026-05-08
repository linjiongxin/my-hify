<script setup lang="ts">
import { computed } from 'vue'

const config = defineModel<any>({ default: () => ({}) })

const methodOptions = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH']

const headers = computed({
  get() {
    const h = config.value.headers
    if (h && typeof h === 'object' && !Array.isArray(h)) {
      return Object.entries(h).map(([key, value]) => ({ key, value: String(value) }))
    }
    return []
  },
  set(val) {
    const record: Record<string, string> = {}
    val.forEach((item: { key: string; value: string }) => {
      if (item.key.trim()) {
        record[item.key.trim()] = item.value
      }
    })
    config.value = { ...config.value, headers: record }
  },
})

function addHeader() {
  headers.value = [...headers.value, { key: '', value: '' }]
}

function removeHeader(index: number) {
  const arr = [...headers.value]
  arr.splice(index, 1)
  headers.value = arr
}
</script>

<template>
  <el-form-item label="请求 URL">
    <el-input v-model="config.url" placeholder="https://api.example.com/data" />
  </el-form-item>
  <el-form-item label="请求方法">
    <el-select v-model="config.method" placeholder="请选择" style="width: 100%">
      <el-option v-for="m in methodOptions" :key="m" :label="m" :value="m" />
    </el-select>
  </el-form-item>
  <el-form-item label="Headers">
    <div class="kv-list">
      <div v-for="(item, index) in headers" :key="index" class="kv-row">
        <el-input v-model="item.key" placeholder="Header 名" />
        <el-input v-model="item.value" placeholder="Header 值" />
        <el-button type="danger" text @click="removeHeader(index)">删除</el-button>
      </div>
      <el-button type="primary" text @click="addHeader">+ 添加 Header</el-button>
    </div>
  </el-form-item>
  <el-form-item label="请求体">
    <el-input
      v-model="config.body"
      type="textarea"
      :rows="4"
      placeholder="JSON 或文本，支持 ${varName} 引用变量"
    />
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
