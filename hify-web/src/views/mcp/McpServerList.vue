<script setup lang="ts">
import { ref } from 'vue'
import { Plus, EditPen, Delete, Refresh } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess } from '@/utils/notify'
import type { PageResult, PageParams } from '@/components/HifyTable.vue'
import type { FormRules } from 'element-plus'
import {
  getMcpServerPage,
  createMcpServer,
  updateMcpServer,
  deleteMcpServer,
  syncMcpServerTools,
  type McpServer,
  type McpTransportType,
  type McpServerStatus,
} from '@/api/mcp'

const { confirmDelete } = useConfirm()

const transportTypeOptions = [
  { label: 'SSE', value: 'sse' },
  { label: 'STDIO', value: 'stdio' },
]

const statusTagType = (status?: McpServerStatus) => {
  switch (status) {
    case 'active': return 'success'
    case 'error': return 'danger'
    case 'offline': return 'info'
    default: return 'info'
  }
}

const statusLabel = (status?: McpServerStatus) => {
  switch (status) {
    case 'active': return '正常'
    case 'error': return '异常'
    case 'offline': return '离线'
    default: return status || '-'
  }
}

const transportLabel = (type?: McpTransportType) => {
  switch (type) {
    case 'sse': return 'SSE'
    case 'stdio': return 'STDIO'
    default: return type || '-'
  }
}

function formatDateTime(val: unknown) {
  if (!val) return '-'
  const d = new Date(String(val))
  if (Number.isNaN(d.getTime())) return String(val)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const columns = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'code', label: '编码', width: 140 },
  { prop: 'transportType', label: '传输类型', width: 100, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => transportLabel(val as McpTransportType) },
  { prop: 'baseUrl', label: '地址 / 命令', minWidth: 200, formatter: (row: any) => row.baseUrl || row.command || '-' },
  { prop: 'status', label: '状态', width: 90, align: 'center' as const, slot: 'status' },
  { prop: 'enabled', label: '启用', width: 80, align: 'center' as const, slot: 'enabled' },
  { prop: 'lastHeartbeatAt', label: '最后心跳', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(val) },
  { label: '操作', width: 240, align: 'center' as const, slot: 'action' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  transportType: [{ required: true, message: '请选择传输类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入基础 URL', trigger: 'blur' }],
  command: [{ required: true, message: '请输入命令', trigger: 'blur' }],
}

interface HifyTableExpose {
  refresh: () => void
}
interface HifyFormDialogExpose {
  open: (data?: any, editing?: boolean) => void
  close: () => void
  finish: () => void
}

const tableRef = ref<HifyTableExpose>()
const dialogRef = ref<HifyFormDialogExpose>()

async function fetchApi(params: PageParams): Promise<PageResult<McpServer>> {
  return getMcpServerPage(params)
}

function handleAdd() {
  dialogRef.value?.open({ transportType: 'sse', enabled: true }, false)
}

function formatJsonText(val: Record<string, unknown> | undefined): string {
  if (!val) return ''
  try {
    return JSON.stringify(val, null, 2)
  } catch {
    return ''
  }
}

function parseJsonText(text: string | undefined): Record<string, unknown> | undefined {
  if (!text || !text.trim()) return undefined
  try {
    return JSON.parse(text) as Record<string, unknown>
  } catch {
    return undefined
  }
}

function handleEdit(row: McpServer) {
  dialogRef.value?.open({
    ...row,
    argsJsonText: formatJsonText(row.argsJson),
    envJsonText: formatJsonText(row.envJson),
  }, true)
}

async function handleDelete(row: McpServer) {
  await confirmDelete(
    async () => {
      await deleteMcpServer(row.id!)
      notifySuccess('删除成功')
      tableRef.value?.refresh()
    },
    { title: '删除 MCP Server', message: `确定删除 "${row.name}" 吗？` }
  )
}

async function handleSyncTools(row: McpServer) {
  await syncMcpServerTools(row.id!)
  notifySuccess('工具同步已触发')
  tableRef.value?.refresh()
}

async function handleSubmit(data: McpServer & { argsJsonText?: string; envJsonText?: string }, isEdit: boolean) {
  const payload = {
    name: data.name,
    code: data.code,
    transportType: data.transportType,
    baseUrl: data.transportType === 'sse' ? data.baseUrl : undefined,
    command: data.transportType === 'stdio' ? data.command : undefined,
    argsJson: data.transportType === 'stdio' ? parseJsonText(data.argsJsonText) : undefined,
    envJson: data.transportType === 'stdio' ? parseJsonText(data.envJsonText) : undefined,
    enabled: data.enabled,
  }
  if (isEdit) {
    await updateMcpServer(data.id!, payload)
    notifySuccess('修改成功')
  } else {
    await createMcpServer(payload)
    notifySuccess('新增成功')
  }
  dialogRef.value?.finish()
  tableRef.value?.refresh()
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">MCP Server 管理</h1>
        <p class="page-desc">管理 MCP 外部工具服务器，支持 SSE 和 STDIO 两种传输协议</p>
      </div>
      <div class="page-header-right">
        <el-button :icon="Plus" type="primary" @click="handleAdd">新增 MCP Server</el-button>
      </div>
    </div>

    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #status="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>

      <template #enabled="{ row }">
        <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
          {{ row.enabled ? '是' : '否' }}
        </el-tag>
      </template>

      <template #action="{ row }">
        <el-button :icon="EditPen" type="primary" text @click="handleEdit(row)">编辑</el-button>
        <el-button :icon="Refresh" type="success" text @click="handleSyncTools(row)">同步工具</el-button>
        <el-button :icon="Delete" type="danger" text @click="handleDelete(row)">删除</el-button>
      </template>
    </HifyTable>

    <HifyFormDialog
      ref="dialogRef"
      title="MCP Server"
      width="560px"
      :rules="rules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：GitHub MCP" />
        </el-form-item>

        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一标识，如：github-mcp" />
        </el-form-item>

        <el-form-item label="传输类型" prop="transportType">
          <el-select v-model="form.transportType" placeholder="请选择传输类型" style="width: 100%">
            <el-option
              v-for="opt in transportTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.transportType === 'sse'" label="基础 URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如：http://localhost:3000/sse" />
        </el-form-item>

        <template v-if="form.transportType === 'stdio'">
          <el-form-item label="命令" prop="command">
            <el-input v-model="form.command" placeholder="如：npx" />
          </el-form-item>

          <el-form-item label="参数 JSON">
            <el-input
              v-model="form.argsJsonText"
              type="textarea"
              :rows="3"
              placeholder='{"arg1": "value1"}'
            />
          </el-form-item>

          <el-form-item label="环境变量 JSON">
            <el-input
              v-model="form.envJsonText"
              type="textarea"
              :rows="3"
              placeholder='{"ENV_KEY": "value"}'
            />
          </el-form-item>
        </template>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </template>
    </HifyFormDialog>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-6);
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

.page-header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}
</style>
