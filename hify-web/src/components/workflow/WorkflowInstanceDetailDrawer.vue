<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { getWorkflowInstance, getWorkflowNodeExecutions, type WorkflowInstanceDTO, type WorkflowNodeExecutionDTO } from '@/api/workflow'

const props = defineProps<{
  visible: boolean
  instanceId?: number | string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

const instance = ref<WorkflowInstanceDTO | null>(null)
const executions = ref<WorkflowNodeExecutionDTO[]>([])
const loading = ref(false)
const activeNames = ref<string[]>([])

watch(() => [props.visible, props.instanceId], ([v, id]) => {
  if (v && id) {
    loadDetail(String(id))
  }
})

async function loadDetail(id: string) {
  loading.value = true
  try {
    const [inst, execs] = await Promise.all([
      getWorkflowInstance(String(id)),
      getWorkflowNodeExecutions(id),
    ])
    instance.value = inst
    executions.value = execs
    activeNames.value = []
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('update:visible', false)
}

const statusType = (status?: string) => {
  switch (status) {
    case 'completed': return 'success'
    case 'failed': return 'danger'
    case 'running': return 'warning'
    default: return 'info'
  }
}

const statusLabel = (status?: string) => {
  switch (status) {
    case 'completed': return '成功'
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

function durationText(start?: string, end?: string) {
  if (!start || !end) return '-'
  const s = new Date(start).getTime()
  const e = new Date(end).getTime()
  const diff = e - s
  if (diff < 1000) return `${diff}ms`
  return `${(diff / 1000).toFixed(1)}s`
}

function tryParseJson(json?: string) {
  if (!json) return null
  try {
    return JSON.parse(json)
  } catch {
    return json
  }
}

const overallDuration = computed(() => {
  if (!instance.value?.startedAt) return '-'
  const end = instance.value.finishedAt || new Date().toISOString()
  return durationText(instance.value.startedAt, end)
})
</script>

<template>
  <el-drawer
    :model-value="visible"
    title="工作流执行详情"
    size="680px"
    :close-on-click-modal="true"
    @close="handleClose"
  >
    <div v-loading="loading" class="drawer-content">
      <div v-if="instance" class="instance-header">
        <div class="instance-meta">
          <span class="meta-label">实例 ID</span>
          <span class="meta-value">#{{ instance.id }}</span>
        </div>
        <div class="instance-meta">
          <span class="meta-label">状态</span>
          <el-tag :type="statusType(instance.status)" size="small">
            {{ statusLabel(instance.status) }}
          </el-tag>
        </div>
        <div class="instance-meta">
          <span class="meta-label">开始时间</span>
          <span class="meta-value">{{ formatDateTime(instance.startedAt) }}</span>
        </div>
        <div class="instance-meta">
          <span class="meta-label">总耗时</span>
          <span class="meta-value">{{ overallDuration }}</span>
        </div>
        <div v-if="instance.errorMsg" class="instance-meta error">
          <span class="meta-label">错误</span>
          <span class="meta-value">{{ instance.errorMsg }}</span>
        </div>
      </div>

      <div class="section-title">节点执行时间线</div>

      <el-timeline v-if="executions.length > 0">
        <el-timeline-item
          v-for="(exec, idx) in executions"
          :key="exec.id"
          :type="statusType(exec.status)"
          :hollow="exec.status !== 'completed'"
          :timestamp="formatDateTime(exec.startedAt)"
        >
          <div class="timeline-card">
            <div class="card-header">
              <div class="node-info">
                <span class="node-name">{{ exec.nodeId }}</span>
                <el-tag size="small" :type="statusType(exec.status)" effect="light">
                  {{ statusLabel(exec.status) }}
                </el-tag>
              </div>
              <div class="node-meta">
                <span class="node-type">{{ exec.nodeType }}</span>
                <span class="duration">耗时 {{ durationText(exec.startedAt, exec.endedAt) }}</span>
              </div>
            </div>

            <div v-if="exec.errorMsg" class="error-block">
              <el-icon><Warning /></el-icon>
              <span>{{ exec.errorMsg }}</span>
            </div>

            <el-collapse v-model="activeNames">
              <el-collapse-item
                v-if="exec.inputJson"
                :name="`input-${exec.id}`"
                title="输入参数"
              >
                <pre class="json-block">{{ JSON.stringify(tryParseJson(exec.inputJson), null, 2) }}</pre>
              </el-collapse-item>
              <el-collapse-item
                v-if="exec.outputJson"
                :name="`output-${exec.id}`"
                title="输出结果"
              >
                <pre class="json-block">{{ JSON.stringify(tryParseJson(exec.outputJson), null, 2) }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-else description="暂无节点执行记录" />
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-content {
  padding: 0 8px;
}

.instance-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
  padding: 16px;
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  margin-bottom: 24px;
}

.instance-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.instance-meta.error .meta-value {
  color: var(--el-color-danger);
}

.meta-label {
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.meta-value {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.timeline-card {
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
}

.card-header {
  margin-bottom: 8px;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.node-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.node-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.node-type {
  background-color: var(--el-fill-color-light);
  padding: 2px 8px;
  border-radius: 4px;
}

.error-block {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 12px;
  background-color: var(--el-color-danger-light-9);
  border-radius: 6px;
  color: var(--el-color-danger);
  font-size: 13px;
  margin-bottom: 8px;
}

.json-block {
  background-color: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

:deep(.el-collapse) {
  border-top: none;
  border-bottom: none;
}

:deep(.el-collapse-item__header) {
  font-size: 13px;
  height: 36px;
  line-height: 36px;
  color: var(--text-secondary);
}

:deep(.el-collapse-item__content) {
  padding-bottom: 8px;
}
</style>
