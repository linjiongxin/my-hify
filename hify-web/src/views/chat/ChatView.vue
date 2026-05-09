<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { Plus, ChatRound, Delete, Loading, Promotion, Link } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSessions, listMessages, createSession, deleteSession, streamChat } from '@/api/chat'
import { getAgentPage } from '@/api/agent'
import type { ChatSession, ChatMessage } from '@/api/chat'
import type { Agent } from '@/api/agent'
import type { PageResult } from '@/components/HifyTable.vue'
import ChatTraceDrawer from '@/components/chat/ChatTraceDrawer.vue'

interface DisplayMessage {
  id?: number
  role: 'user' | 'assistant' | 'system'
  content: string
  status?: string
  isStreaming?: boolean
  model?: string
  traceId?: string
  createdAt?: string
}

const sessions = ref<ChatSession[]>([])
const currentSession = ref<ChatSession | null>(null)
const messages = ref<DisplayMessage[]>([])
const inputMessage = ref('')
const loadingHistory = ref(false)
const sending = ref(false)
const sessionListLoading = ref(false)
const newSessionDialogVisible = ref(false)
const agentList = ref<Agent[]>([])
const agentLoading = ref(false)
const selectedAgentId = ref<number | null>(null)
const abortController = ref<(() => void) | null>(null)
const traceDrawerVisible = ref(false)
const selectedTraceId = ref('')

const messageListRef = ref<HTMLElement>()

onMounted(() => {
  loadSessions()
  loadAgents()
})

async function loadSessions() {
  sessionListLoading.value = true
  try {
    sessions.value = await listSessions()
  } finally {
    sessionListLoading.value = false
  }
}

async function selectSession(session: ChatSession) {
  currentSession.value = session
  messages.value = []
  loadingHistory.value = true
  try {
    const history = await listMessages(session.id)
    messages.value = history.map((m) => ({
      id: m.id,
      role: m.role as 'user' | 'assistant' | 'system',
      content: m.content,
      status: m.status,
      model: m.model,
      traceId: m.traceId,
      createdAt: m.createdAt,
    }))
    scrollToBottom()
  } finally {
    loadingHistory.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function openNewSessionDialog() {
  selectedAgentId.value = null
  newSessionDialogVisible.value = true
  loadAgents()
}

async function loadAgents() {
  agentLoading.value = true
  try {
    const res: PageResult<Agent> = await getAgentPage({ current: 1, size: 100 })
    agentList.value = res.records.filter((a: Agent) => a.enabled)
  } finally {
    agentLoading.value = false
  }
}

async function confirmNewSession() {
  if (!selectedAgentId.value) {
    ElMessage.warning('请选择一个 Agent')
    return
  }
  newSessionDialogVisible.value = false
  try {
    const session = await createSession({ agentId: selectedAgentId.value })
    sessions.value.unshift(session)
    await selectSession(session)
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

async function handleDeleteSession(session: ChatSession, event: Event) {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm(`确定删除会话「${session.title}」吗？`, '提示', {
      type: 'warning',
    })
    await deleteSession(session.id)
    ElMessage.success('删除成功')
    sessions.value = sessions.value.filter((s) => s.id !== session.id)
    if (currentSession.value?.id === session.id) {
      currentSession.value = null
      messages.value = []
    }
  } catch {
    // cancelled
  }
}

function handleSend() {
  const text = inputMessage.value.trim()
  if (!text || sending.value || !currentSession.value) return

  const sessionId = currentSession.value.id

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    status: 'completed',
  })

  // 添加助手占位消息
  messages.value.push({
    role: 'assistant',
    content: '',
    status: 'streaming',
    isStreaming: true,
  })

  inputMessage.value = ''
  sending.value = true
  scrollToBottom()

  const assistantMsg = messages.value[messages.value.length - 1]!

  abortController.value = streamChat(
    sessionId,
    text,
    (chunk) => {
      assistantMsg.content += chunk
      scrollToBottom()
    },
    () => {
      assistantMsg.status = 'completed'
      assistantMsg.isStreaming = false
      sending.value = false
      abortController.value = null
      // 刷新会话列表以更新标题和消息数
      loadSessions()
    },
    (error) => {
      assistantMsg.status = 'error'
      assistantMsg.isStreaming = false
      assistantMsg.content += '\n[错误: ' + error + ']'
      sending.value = false
      abortController.value = null
      ElMessage.error(error)
    }
  )
}

function handleKeydown(e: Event | KeyboardEvent) {
  if (!(e instanceof KeyboardEvent)) return
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function formatTime(iso?: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const currentSessionTitle = computed(() => {
  return currentSession.value?.title || '未选择会话'
})

// Agent 名称映射表
const agentsMap = computed(() => {
  const map = new Map<number, string>()
  for (const agent of agentList.value) {
    map.set(agent.id, agent.name)
  }
  return map
})

function getAgentName(agentId?: number): string {
  if (!agentId) return '未知 Agent'
  return agentsMap.value.get(agentId) || `Agent #${agentId}`
}

function openTraceDrawer(traceId: string) {
  selectedTraceId.value = traceId
  traceDrawerVisible.value = true
}
</script>

<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <aside class="session-sidebar">
      <div class="session-sidebar-header">
        <el-button type="primary" :icon="Plus" class="new-chat-btn" @click="openNewSessionDialog">
          新对话
        </el-button>
      </div>

      <div v-loading="sessionListLoading" class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSession?.id === session.id }"
          @click="selectSession(session)"
        >
          <div class="session-item-content">
            <el-icon class="session-icon"><ChatRound /></el-icon>
            <div class="session-info">
              <div class="session-title">{{ session.title }}</div>
              <div class="session-meta">
                <span class="session-agent">{{ getAgentName(session.agentId) }}</span>
                <span>{{ session.messageCount }} 条消息</span>
                <span class="session-time">{{ formatTime(session.lastMessageAt) }}</span>
              </div>
            </div>
          </div>
          <el-button
            class="session-delete-btn"
            type="danger"
            link
            :icon="Delete"
            @click="handleDeleteSession(session, $event)"
          />
        </div>

        <div v-if="sessions.length === 0 && !sessionListLoading" class="session-empty">
          暂无会话，点击上方按钮开始新对话
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区域 -->
    <main class="chat-main">
      <div v-if="!currentSession" class="chat-empty-state">
        <div class="chat-empty-content">
          <el-icon :size="64" class="chat-empty-icon"><ChatRound /></el-icon>
          <h2>开始新对话</h2>
          <p>选择一个 Agent，开启你的 AI 助手之旅</p>
          <el-button type="primary" :icon="Plus" size="large" @click="openNewSessionDialog">
            新对话
          </el-button>
        </div>
      </div>

      <template v-else>
        <header class="chat-header">
          <div class="chat-header-left">
            <div class="chat-header-title">{{ currentSessionTitle }}</div>
            <div class="chat-header-agent">{{ getAgentName(currentSession?.agentId) }}</div>
          </div>
          <div class="chat-header-meta">
            <span v-if="sending" class="streaming-indicator">
              <el-icon class="streaming-icon"><Loading /></el-icon>
              思考中...
            </span>
          </div>
        </header>

        <div ref="messageListRef" v-loading="loadingHistory" class="message-list">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-row"
            :class="msg.role"
          >
            <div class="message-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="36" class="avatar-user">
                我
              </el-avatar>
              <el-avatar v-else :size="36" class="avatar-assistant">
                AI
              </el-avatar>
            </div>
            <div class="message-body">
              <div class="message-bubble" :class="msg.role">
                <div class="message-text" v-html="msg.content.replace(/\n/g, '<br>')" />
                <div v-if="msg.isStreaming" class="streaming-cursor" />
              </div>
              <div class="message-footer">
                <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
                <span v-if="msg.model" class="message-model">{{ msg.model }}</span>
                <el-tag v-if="msg.status === 'error'" type="danger" size="small">错误</el-tag>
                <el-button
                  v-if="msg.traceId && msg.status === 'completed'"
                  :icon="Link"
                  text
                  size="small"
                  @click="openTraceDrawer(msg.traceId)"
                >
                  链路
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <footer class="chat-input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="2"
              placeholder="输入消息，按 Enter 发送，Shift+Enter 换行"
              resize="none"
              :disabled="sending"
              @keydown="handleKeydown"
            />
            <el-button
              type="primary"
              class="send-btn"
              :icon="Promotion"
              :loading="sending"
              :disabled="!inputMessage.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>
        </footer>
      </template>
    </main>

    <!-- 链路追踪 Drawer -->
    <ChatTraceDrawer v-model:visible="traceDrawerVisible" :trace-id="selectedTraceId" />

    <!-- 新建会话对话框 -->
    <el-dialog v-model="newSessionDialogVisible" title="选择 Agent" width="480px">
      <div v-loading="agentLoading" class="agent-select-list">
        <div
          v-for="agent in agentList"
          :key="agent.id"
          class="agent-select-item"
          :class="{ selected: selectedAgentId === agent.id }"
          @click="selectedAgentId = agent.id"
        >
          <div class="agent-select-name">{{ agent.name }}</div>
          <div class="agent-select-desc">{{ agent.description || '暂无描述' }}</div>
          <div class="agent-select-model">{{ agent.modelId }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="newSessionDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAgentId" @click="confirmNewSession">
          开始对话
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  height: 100%;
  background-color: var(--bg-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

/* 左侧会话列表 */
.session-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  background-color: var(--bg-surface);
}

.session-sidebar-header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--border-subtle);
}

.new-chat-btn {
  width: 100%;
  border-radius: var(--radius-md);
  font-weight: 500;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2);
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-in-out);
  margin-bottom: var(--space-1);
}

.session-item:hover {
  background-color: var(--bg-hover);
}

.session-item.active {
  background-color: var(--color-primary-50);
}

.session-item-content {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  overflow: hidden;
  flex: 1;
}

.session-icon {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.session-item.active .session-icon {
  color: var(--color-primary-500);
}

.session-info {
  overflow: hidden;
  flex: 1;
}

.session-title {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 2px;
  display: flex;
  gap: var(--space-2);
  align-items: center;
}

.session-agent {
  background-color: var(--color-primary-100);
  color: var(--color-primary-700);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

.session-time {
  margin-left: auto;
}

.session-delete-btn {
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-in-out);
}

.session-item:hover .session-delete-btn {
  opacity: 1;
}

.session-empty {
  padding: var(--space-8) var(--space-4);
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.chat-empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-empty-content {
  text-align: center;
  color: var(--text-tertiary);
}

.chat-empty-content h2 {
  color: var(--text-primary);
  margin: var(--space-4) 0 var(--space-2);
  font-size: var(--text-xl);
}

.chat-empty-content p {
  margin-bottom: var(--space-6);
  font-size: var(--text-base);
}

.chat-empty-icon {
  color: var(--color-primary-200);
}

.chat-header {
  height: 56px;
  border-bottom: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  flex-shrink: 0;
  background-color: var(--bg-surface);
}

.chat-header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-header-title {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
}

.chat-header-agent {
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.2;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-primary-500);
}

.streaming-icon {
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4) var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.message-row {
  display: flex;
  gap: var(--space-3);
  max-width: 85%;
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-row.assistant {
  align-self: flex-start;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-user {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-700));
  color: white;
  font-size: var(--text-sm);
  font-weight: 600;
}

.avatar-assistant {
  background: linear-gradient(135deg, var(--color-secondary-500), var(--color-secondary-700));
  color: white;
  font-size: var(--text-sm);
  font-weight: 600;
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.message-row.user .message-body {
  align-items: flex-end;
}

.message-bubble {
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  font-size: var(--text-base);
  line-height: 1.6;
  word-break: break-word;
  position: relative;
}

.message-bubble.user {
  background: linear-gradient(135deg, var(--color-primary-500), var(--color-primary-700));
  color: white;
  border-bottom-right-radius: 4px;
}

.message-bubble.assistant {
  background-color: var(--bg-surface);
  color: var(--text-primary);
  border: 1px solid var(--border-subtle);
  border-bottom-left-radius: 4px;
}

.message-text {
  white-space: pre-wrap;
}

.streaming-cursor::after {
  content: '';
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: var(--color-primary-500);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.message-footer {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: 11px;
  color: var(--text-tertiary);
}

.message-model {
  background-color: var(--bg-hover);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
}

/* 输入区域 */
.chat-input-area {
  border-top: 1px solid var(--border-subtle);
  padding: var(--space-4) var(--space-6);
  background-color: var(--bg-surface);
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  gap: var(--space-3);
  align-items: flex-end;
}

.input-wrapper :deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
  background-color: var(--bg-surface);
  resize: none;
  padding: var(--space-3);
  font-size: var(--text-base);
  line-height: 1.5;
}

.send-btn {
  border-radius: var(--radius-md);
  height: 56px;
  padding: 0 var(--space-6);
  font-weight: 500;
}

/* Agent 选择列表 */
.agent-select-list {
  max-height: 360px;
  overflow-y: auto;
}

.agent-select-item {
  padding: var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  margin-bottom: var(--space-3);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-in-out);
}

.agent-select-item:hover {
  border-color: var(--color-primary-300);
  background-color: var(--color-primary-50);
}

.agent-select-item.selected {
  border-color: var(--color-primary-500);
  background-color: var(--color-primary-50);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.agent-select-name {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}

.agent-select-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-1);
}

.agent-select-model {
  font-size: 12px;
  color: var(--text-tertiary);
  background-color: var(--bg-hover);
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}
</style>
