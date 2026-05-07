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

export function getKnowledgeBaseOptions(): Promise<KnowledgeBaseOption[]> {
  return get('/rag/knowledge-bases', { params: { enabled: true } }).then((res: any) => res.records || [])
}

export function getAgentKbBindings(agentId: number): Promise<AgentKbBinding[]> {
  return get(`/rag/agent-kb/agent/${agentId}`)
}

export function bindAgentKb(data: AgentKbBindRequest): Promise<void> {
  return post('/rag/agent-kb/bind', data)
}

export function updateAgentKbBinding(
  agentId: number,
  kbId: number,
  topK: number,
  similarityThreshold: number
): Promise<void> {
  return put('/rag/agent-kb/update', null, {
    params: { agentId, kbId, topK, similarityThreshold },
  })
}

export function unbindAgentKb(agentId: number, kbId: number): Promise<void> {
  return del('/rag/agent-kb/unbind', {
    params: { agentId, kbId },
  })
}