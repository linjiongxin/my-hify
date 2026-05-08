import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageParams as HifyPageParams } from '@/components/HifyTable.vue'

export interface Agent {
  id: number
  name: string
  description?: string
  modelId: string
  systemPrompt?: string
  temperature: number
  maxTokens: number
  topP: number
  welcomeMessage?: string
  enabled: boolean
  workflowId?: number
  executionMode?: string
  createdAt: string
  updatedAt: string
}

export interface AgentCreateRequest {
  name: string
  modelId: string
  description?: string
  systemPrompt?: string
  temperature?: number
  maxTokens?: number
  topP?: number
  welcomeMessage?: string
  enabled?: boolean
  workflowId?: number
  executionMode?: string
}

export interface AgentUpdateRequest {
  name?: string
  modelId?: string
  description?: string
  systemPrompt?: string
  temperature?: number
  maxTokens?: number
  topP?: number
  welcomeMessage?: string
  enabled?: boolean
  workflowId?: number
  executionMode?: string
}

export interface ToolItem {
  toolName: string
  toolType: string
  toolImpl: string
  configJson?: Record<string, unknown>
  enabled?: boolean
  sortOrder?: number
}

export interface AgentToolBatchRequest {
  tools: ToolItem[]
}

export function getAgentPage(params: HifyPageParams): Promise<PageResult<Agent>> {
  return get('/agent', {
    params: {
      pageNum: params.current,
      pageSize: params.size,
    },
  })
}

export function getAgentDetail(id: number): Promise<Agent> {
  return get(`/agent/${id}`)
}

export function createAgent(data: AgentCreateRequest): Promise<number> {
  return post('/agent', data)
}

export function updateAgent(id: number, data: AgentUpdateRequest): Promise<void> {
  return put(`/agent/${id}`, data)
}

export function deleteAgent(id: number): Promise<void> {
  return del(`/agent/${id}`)
}

export function getAgentTools(agentId: number): Promise<ToolItem[]> {
  return get(`/agent/${agentId}/tools`)
}

export function bindAgentTools(agentId: number, data: AgentToolBatchRequest): Promise<void> {
  return post(`/agent/${agentId}/tools`, data)
}

export function replaceAgentTools(agentId: number, data: AgentToolBatchRequest): Promise<void> {
  return put(`/agent/${agentId}/tools`, data)
}

export function unbindAgentTool(agentId: number, toolId: number): Promise<void> {
  return del(`/agent/${agentId}/tools/${toolId}`)
}

// ==================== 知识库绑定 ====================

export interface KnowledgeBaseOption {
  id: number
  name: string
}

export interface AgentKbBinding {
  id: number
  agentId: number
  kbId: number
  kbName?: string
  topK: number
  similarityThreshold: number
  enabled: boolean
}

export interface AgentKbBindRequest {
  agentId: number
  kbId: number
  topK: number
  similarityThreshold: number
}

const baseUrl = '/api'

function getToken(): string {
  return localStorage.getItem('token') || ''
}

export function getKnowledgeBaseOptions(): Promise<KnowledgeBaseOption[]> {
  return fetch(`${baseUrl}/rag/knowledge-bases?enabled=true`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  })
    .then((r) => r.json())
    .then((res: any) => res.records || [])
}

export function getAgentKbBindings(agentId: number): Promise<AgentKbBinding[]> {
  return fetch(`${baseUrl}/rag/agent-kb/agent/${agentId}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  }).then((r) => r.json())
}

export function bindAgentKb(data: AgentKbBindRequest): Promise<void> {
  return fetch(`${baseUrl}/rag/agent-kb/bind`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getToken()}`,
    },
    body: JSON.stringify(data),
  }).then((r) => {
    if (!r.ok) return Promise.reject(new Error('绑定失败'))
  })
}

export function updateAgentKbBinding(
  agentId: number,
  kbId: number,
  topK: number,
  similarityThreshold: number
): Promise<void> {
  const params = new URLSearchParams({
    agentId: String(agentId),
    kbId: String(kbId),
    topK: String(topK),
    similarityThreshold: String(similarityThreshold),
  })
  return fetch(`${baseUrl}/rag/agent-kb/update?${params.toString()}`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${getToken()}` },
  }).then((r) => {
    if (!r.ok) return Promise.reject(new Error('更新失败'))
  })
}

export function unbindAgentKb(agentId: number, kbId: number): Promise<void> {
  const params = new URLSearchParams({
    agentId: String(agentId),
    kbId: String(kbId),
  })
  return fetch(`${baseUrl}/rag/agent-kb/unbind?${params.toString()}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${getToken()}` },
  }).then((r) => {
    if (!r.ok) return Promise.reject(new Error('解绑失败'))
  })
}