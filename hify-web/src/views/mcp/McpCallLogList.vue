<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, View } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import { getMcpCallLogs, type McpCallLog } from '@/api/mcp'
import type { PageResult, PageParams } from '@/components/HifyTable.vue'

const router = useRouter()

const statusOptions = [
  { label: '全部', value: '' },
  { label: '成功', value: 'success' },
  { label: '失败', value: 'failed' },
  { label: '运行中', value: 'running' },
]

const statusTagType = (status?: string) => {
  switch (status) {
    case 'success': return 'success'
    case 'failed': return 'danger'
    case 'running': return 'warning'
    default: return 'info'
  }
}

const statusLabel = (status?: string) => {
  switch (status) {
    case 'success': return '成功'
    case 'failed': return '失败'
    case 'running': return '运行中'
    default: return status || '-'
  }
}

function formatDateTime(val?: string) {
  if (!val) return '-'
  const d = new Date(val)
  if (Number.isNaN(d.getTime())) return val
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function durationText(ms?: number) {
  if (ms === undefined || ms === null) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function tryFormatJson(text?: string): string {
  if (!text) return ''
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

const selectedStatus = ref('')
const filterServerUrl = ref('')
const filterToolName = ref('')
const tableRef = ref()

const detailVisible = ref(false)
const detailRow = ref<McpCallLog | null>(null)

const columns = [
  { prop: 'id', label: 'ID', width: 80, align: 'center' as const },
  { prop: 'serverUrl', label: 'Server URL', minWidth: 220 },
  { prop: 'toolName', label: '工具名', width: 160 },
  { prop: 'status', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'durationMs', label: '耗时', width: 90, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => durationText(val as number) },
  { prop: 'traceId', label: 'Trace ID', width: 160 },
  { prop: 'createdAt', label: '调用时间', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(String(val)) },
  { label: '操作', width: 100, align: 'center' as const, slot: 'action' },
]

async function fetchApi(params: PageParams): Promise<PageResult<McpCallLog>> {
  return getMcpCallLogs({
    current: params.current,
    size: params.size,
    serverUrl: filterServerUrl.value || undefined,
    toolName: filterToolName.value || undefined,
    status: selectedStatus.value || undefined,
  })
}

function handleFilter() {
  tableRef.value?.refresh()
}

function handleBack() {
  router.push('/mcp-servers')
}

function handleViewDetail(row: McpCallLog) {
  detailRow.value = row
  detailVisible.value = true
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="back-row">
          <el-button :icon="ArrowLeft" text @click="handleBack">返回 MCP Server 列表</el-button>
        </div>
        <h1 class="page-title">MCP 调用记录</h1>
        <p class="page-desc">查看 MCP 工具调用的完整请求、响应与耗时记录</p>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">状态：</span>
        <el-select v-model="selectedStatus" placeholder="全部" style="width: 120px" @change="handleFilter">
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">Server URL：</span>
        <el-input v-model="filterServerUrl" placeholder="筛选 URL" style="width: 200px" clearable @change="handleFilter" />
      </div>
      <div class="filter-item">
        <span class="filter-label">工具名：</span>
        <el-input v-model="filterToolName" placeholder="筛选工具名" style="width: 160px" clearable @change="handleFilter" />
      </div>
    </div>

    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #status="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>

      <template #action="{ row }">
        <el-button :icon="View" type="primary" text @click="handleViewDetail(row)">查看详情</el-button>
      </template>
    </HifyTable>

    <el-dialog v-model="detailVisible" title="调用详情" width="720px">
      <div v-if="detailRow" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">Server URL</span>
          <span class="detail-value">{{ detailRow.serverUrl }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">工具名</span>
          <span class="detail-value">{{ detailRow.toolName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="statusTagType(detailRow.status)" size="small">
            {{ statusLabel(detailRow.status) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">耗时</span>
          <span class="detail-value">{{ durationText(detailRow.durationMs) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Trace ID</span>
          <span class="detail-value">{{ detailRow.traceId || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">调用时间</span>
          <span class="detail-value">{{ formatDateTime(detailRow.createdAt) }}</span>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">请求参数</div>
          <pre class="json-block">{{ tryFormatJson(detailRow.requestJson) }}</pre>
        </div>

        <div v-if="detailRow.responseJson" class="detail-section">
          <div class="detail-section-title">响应结果</div>
          <pre class="json-block">{{ tryFormatJson(detailRow.responseJson) }}</pre>
        </div>

        <div v-if="detailRow.errorMsg" class="detail-section">
          <div class="detail-section-title">错误信息</div>
          <pre class="json-block error">{{ detailRow.errorMsg }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
}

.back-row {
  margin-bottom: 8px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  gap: var(--space-4);
}

.page-header-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.page-desc {
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: 1.5;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.filter-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-label {
  width: 80px;
  font-size: 14px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.detail-value {
  font-size: 14px;
  color: var(--text-primary);
  word-break: break-all;
}

.detail-section {
  margin-top: 8px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.json-block {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  color: #2c3e50;
}

.json-block.error {
  background: #fef0f0;
  color: #f56c6c;
}
</style>
