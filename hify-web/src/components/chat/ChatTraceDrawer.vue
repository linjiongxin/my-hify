<script setup lang="ts">
import { ref, watch } from 'vue'
import { getChatTrace } from '@/api/chat'
import type { ChatTrace, ChatTraceEvent } from '@/api/chat'

const props = defineProps<{
  visible: boolean
  traceId: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const trace = ref<ChatTrace | null>(null)
const loading = ref(false)
const expandedIndex = ref<number | null>(null)

watch(
  () => [props.visible, props.traceId],
  ([visible, traceId]) => {
    if (visible && traceId) {
      loadTrace(traceId as string)
    }
  },
  { immediate: true }
)

async function loadTrace(id: string) {
  loading.value = true
  expandedIndex.value = null
  try {
    trace.value = await getChatTrace(id)
  } catch {
    trace.value = null
  } finally {
    loading.value = false
  }
}

function toggleExpand(index: number) {
  expandedIndex.value = expandedIndex.value === index ? null : index
}

function getEventColor(type: ChatTraceEvent['type']): string {
  switch (type) {
    case 'user_message':
      return '#67C23A'
    case 'rag':
      return '#409EFF'
    case 'workflow':
      return '#E6A23C'
    case 'mcp':
      return '#909399'
    case 'llm_reply':
      return '#67C23A'
    default:
      return '#909399'
  }
}

function getStatusType(status?: string) {
  switch (status) {
    case 'completed':
    case 'success':
      return 'success'
    case 'error':
    case 'failed':
      return 'danger'
    case 'running':
      return 'warning'
    default:
      return 'info'
  }
}

function formatDuration(ms?: number): string {
  if (ms === undefined || ms === null) return ''
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function formatJson(data: unknown): string {
  try {
    return JSON.stringify(data, null, 2)
  } catch {
    return String(data)
  }
}

function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-drawer
    :model-value="visible"
    title="链路追踪"
    size="640px"
    direction="rtl"
    :destroy-on-close="false"
    @close="handleClose"
  >
    <div v-loading="loading" class="trace-drawer-content">
      <!-- 顶部信息区 -->
      <div v-if="trace" class="trace-header">
        <div class="trace-id">Trace ID: {{ trace.traceId }}</div>
        <div class="trace-summary">
          <span class="trace-message" :title="trace.userMessage">{{ trace.userMessage }}</span>
        </div>
        <div class="trace-meta">
          <el-tag size="small" type="info">{{ trace.agentName }}</el-tag>
          <span class="trace-duration">总耗时: {{ formatDuration(trace.totalDurationMs) }}</span>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && trace && trace.events.length === 0" class="trace-empty">
        <el-empty description="暂无链路数据" />
      </div>

      <!-- 时间线 -->
      <el-timeline v-if="trace && trace.events.length > 0">
        <el-timeline-item
          v-for="(event, index) in trace.events"
          :key="index"
          :type="getStatusType(event.status) as any"
          :color="getEventColor(event.type)"
          :hide-timestamp="true"
        >
          <div
            class="timeline-card"
            :class="{ expanded: expandedIndex === index }"
            @click="toggleExpand(index)"
          >
            <div class="timeline-card-header">
              <div class="timeline-title-row">
                <span class="timeline-title">{{ event.title }}</span>
                <el-tag
                  v-if="event.status"
                  size="small"
                  :type="getStatusType(event.status) as any"
                  class="timeline-status"
                >
                  {{ event.status }}
                </el-tag>
              </div>
              <div class="timeline-subtitle">
                <span class="timeline-type">{{ event.type }}</span>
                <span v-if="event.durationMs" class="timeline-duration">
                  {{ formatDuration(event.durationMs) }}
                </span>
                <span class="timeline-time">{{ event.time }}</span>
              </div>
            </div>

            <!-- 展开详情 -->
            <div v-if="expandedIndex === index" class="timeline-detail" @click.stop>
              <!-- MCP 详情 -->
              <template v-if="event.type === 'mcp'">
                <div v-if="event.details.serverUrl" class="detail-row">
                  <span class="detail-label">Server URL:</span>
                  <span class="detail-value">{{ event.details.serverUrl }}</span>
                </div>
                <div v-if="event.details.toolName" class="detail-row">
                  <span class="detail-label">工具名:</span>
                  <span class="detail-value">{{ event.details.toolName }}</span>
                </div>
                <div v-if="event.details.request" class="detail-block">
                  <div class="detail-label">请求:</div>
                  <pre class="detail-code">{{ formatJson(event.details.request) }}</pre>
                </div>
                <div v-if="event.details.response" class="detail-block">
                  <div class="detail-label">响应:</div>
                  <pre class="detail-code">{{ formatJson(event.details.response) }}</pre>
                </div>
              </template>

              <!-- 工作流详情 -->
              <template v-if="event.type === 'workflow'">
                <div v-if="event.details.nodes && Array.isArray(event.details.nodes)" class="detail-block">
                  <div class="detail-label">节点列表:</div>
                  <div class="workflow-nodes">
                    <div
                      v-for="(node, i) in event.details.nodes as any[]"
                      :key="i"
                      class="workflow-node"
                    >
                      <span class="node-name">{{ node.name }}</span>
                      <el-tag size="small" type="info">{{ node.type }}</el-tag>
                      <el-tag
                        v-if="node.status"
                        size="small"
                        :type="getStatusType(node.status) as any"
                      >
                        {{ node.status }}
                      </el-tag>
                      <span v-if="node.durationMs" class="node-duration">
                        {{ formatDuration(node.durationMs) }}
                      </span>
                    </div>
                  </div>
                </div>
              </template>

              <!-- RAG 详情 -->
              <template v-if="event.type === 'rag'">
                <div v-if="event.details.query" class="detail-row">
                  <span class="detail-label">查询词:</span>
                  <span class="detail-value">{{ event.details.query }}</span>
                </div>
                <div v-if="event.details.knowledgeBase" class="detail-row">
                  <span class="detail-label">知识库:</span>
                  <span class="detail-value">{{ event.details.knowledgeBase }}</span>
                </div>
                <div v-if="event.details.resultCount !== undefined" class="detail-row">
                  <span class="detail-label">返回条数:</span>
                  <span class="detail-value">{{ event.details.resultCount }}</span>
                </div>
              </template>

              <!-- 消息详情 -->
              <template v-if="event.type === 'user_message' || event.type === 'llm_reply'">
                <div v-if="event.details.content" class="detail-block">
                  <div class="detail-label">内容:</div>
                  <pre class="detail-code content-text">{{ event.details.content }}</pre>
                </div>
              </template>

              <!-- 通用详情兜底 -->
              <div
                v-if="Object.keys(event.details).length === 0 ||
                  (event.type !== 'mcp' && event.type !== 'workflow' && event.type !== 'rag' &&
                   event.type !== 'user_message' && event.type !== 'llm_reply')"
                class="detail-block"
              >
                <pre class="detail-code">{{ formatJson(event.details) }}</pre>
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-drawer>
</template>

<style scoped>
.trace-drawer-content {
  padding: 0 var(--space-2);
}

.trace-header {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border-subtle);
}

.trace-id {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-bottom: var(--space-2);
  font-family: monospace;
}

.trace-summary {
  margin-bottom: var(--space-3);
}

.trace-message {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.trace-meta {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.trace-duration {
  font-size: 12px;
  color: var(--text-secondary);
}

.trace-empty {
  padding: var(--space-12) 0;
}

.timeline-card {
  background-color: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-in-out);
}

.timeline-card:hover {
  border-color: var(--color-primary-300);
  background-color: var(--bg-hover);
}

.timeline-card.expanded {
  border-color: var(--color-primary-300);
}

.timeline-card-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.timeline-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.timeline-title {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
  flex: 1;
  word-break: break-word;
}

.timeline-status {
  flex-shrink: 0;
}

.timeline-subtitle {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: 12px;
  color: var(--text-tertiary);
}

.timeline-type {
  text-transform: uppercase;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary);
}

.timeline-duration {
  color: var(--color-primary-500);
  font-weight: 500;
}

.timeline-time {
  margin-left: auto;
}

.timeline-detail {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border-subtle);
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
}

.detail-label {
  color: var(--text-secondary);
  font-weight: 500;
  flex-shrink: 0;
}

.detail-value {
  color: var(--text-primary);
  word-break: break-word;
}

.detail-block {
  margin-bottom: var(--space-3);
}

.detail-code {
  background-color: var(--bg-base);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  font-size: 12px;
  font-family: 'SF Mono', Monaco, 'Cascadia Code', monospace;
  color: var(--text-primary);
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  margin-top: var(--space-1);
}

.content-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.workflow-nodes {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-2);
}

.workflow-node {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background-color: var(--bg-base);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
}

.node-name {
  font-weight: 500;
  color: var(--text-primary);
  flex: 1;
}

.node-duration {
  font-size: 11px;
  color: var(--text-tertiary);
}
</style>
