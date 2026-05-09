<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, View } from '@element-plus/icons-vue'
import HifyTable from '@/components/HifyTable.vue'
import WorkflowInstanceDetailDrawer from '@/components/workflow/WorkflowInstanceDetailDrawer.vue'
import { getWorkflowInstancePage, type WorkflowInstanceDTO, type InstanceStatus } from '@/api/workflow'

type InstanceStatusFilter = InstanceStatus | ''
import type { PageResult, PageParams } from '@/components/HifyTable.vue'

const route = useRoute()
const router = useRouter()

const workflowId = ref<string | undefined>(
  route.query.workflowId ? String(route.query.workflowId) : undefined
)

const statusOptions = [
  { label: '全部', value: '' },
  { label: '运行中', value: 'running' },
  { label: '已完成', value: 'completed' },
  { label: '失败', value: 'failed' },
  { label: '已取消', value: 'cancelled' },
]

const statusTagType = (status?: InstanceStatus) => {
  switch (status) {
    case 'completed': return 'success'
    case 'failed': return 'danger'
    case 'running': return 'warning'
    case 'cancelled': return 'info'
    default: return 'info'
  }
}

const statusLabel = (status?: InstanceStatus) => {
  switch (status) {
    case 'completed': return '已完成'
    case 'failed': return '失败'
    case 'running': return '运行中'
    case 'cancelled': return '已取消'
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

function durationText(start?: string, end?: string) {
  if (!start) return '-'
  const s = new Date(start).getTime()
  const e = end ? new Date(end).getTime() : Date.now()
  const diff = e - s
  if (diff < 1000) return `${diff}ms`
  if (diff < 60000) return `${(diff / 1000).toFixed(1)}s`
  return `${Math.floor(diff / 60000)}m ${Math.floor((diff % 60000) / 1000)}s`
}

const selectedStatus = ref<InstanceStatusFilter>('')
const tableRef = ref()
const drawerVisible = ref(false)
const selectedInstanceId = ref<number | string | undefined>(undefined)

const columns = [
  { prop: 'id', label: '实例 ID', width: 90, align: 'center' as const },
  { prop: 'workflowId', label: '工作流 ID', width: 100, align: 'center' as const },
  { prop: 'status', label: '状态', width: 100, align: 'center' as const, slot: 'status' },
  { prop: 'currentNodeId', label: '当前节点', minWidth: 140 },
  { prop: 'startedAt', label: '开始时间', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(String(val)) },
  { prop: 'finishedAt', label: '结束时间', width: 180, align: 'center' as const, formatter: (_row: any, _col: any, val: unknown) => formatDateTime(String(val)) },
  { label: '耗时', width: 100, align: 'center' as const, slot: 'duration' },
  { label: '操作', width: 110, align: 'center' as const, slot: 'action' },
]

async function fetchApi(params: PageParams): Promise<PageResult<WorkflowInstanceDTO>> {
  return getWorkflowInstancePage({
    current: params.current,
    size: params.size,
    workflowId: workflowId.value,
    status: (selectedStatus.value as InstanceStatus) || undefined,
  })
}

function handleFilter() {
  tableRef.value?.refresh()
}

function handleBack() {
  router.push('/workflows')
}

function handleViewDetail(row: WorkflowInstanceDTO) {
  selectedInstanceId.value = row.id
  drawerVisible.value = true
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="back-row">
          <el-button :icon="ArrowLeft" text @click="handleBack">返回工作流列表</el-button>
        </div>
        <h1 class="page-title">工作流执行记录</h1>
        <p class="page-desc">查看工作流实例的执行历史与节点详情</p>
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
    </div>

    <HifyTable ref="tableRef" :columns="columns" :api="fetchApi">
      <template #status="{ row }">
        <el-tag :type="statusTagType(row.status)" size="small">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>

      <template #duration="{ row }">
        <span>{{ durationText(row.startedAt, row.finishedAt) }}</span>
      </template>

      <template #action="{ row }">
        <el-button :icon="View" type="primary" text @click="handleViewDetail(row)">查看详情</el-button>
      </template>
    </HifyTable>

    <WorkflowInstanceDetailDrawer v-model:visible="drawerVisible" :instance-id="selectedInstanceId" />
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
</style>
