import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageParams as HifyPageParams } from '@/components/HifyTable.vue'

export type McpTransportType = 'sse' | 'stdio'
export type McpServerStatus = 'active' | 'error' | 'offline'

export interface McpServer {
  id?: number
  name: string
  code: string
  transportType: McpTransportType
  baseUrl?: string
  command?: string
  argsJson?: Record<string, unknown>
  envJson?: Record<string, unknown>
  enabled?: boolean
  status?: McpServerStatus
  lastHeartbeatAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface McpServerCreateRequest {
  name: string
  code: string
  transportType: McpTransportType
  baseUrl?: string
  command?: string
  argsJson?: Record<string, unknown>
  envJson?: Record<string, unknown>
  enabled?: boolean
}

export interface McpServerUpdateRequest {
  name?: string
  code?: string
  transportType?: McpTransportType
  baseUrl?: string
  command?: string
  argsJson?: Record<string, unknown>
  envJson?: Record<string, unknown>
  enabled?: boolean
}

export interface McpTool {
  id?: number
  serverId: number
  name: string
  description?: string
  schemaJson?: Record<string, unknown>
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
}

export function getMcpServerPage(params: HifyPageParams & { name?: string; code?: string }): Promise<PageResult<McpServer>> {
  return get('/mcp-server', {
    params: {
      pageNum: params.current,
      pageSize: params.size,
      name: params.name,
      code: params.code,
    },
  })
}

export function getMcpServerDetail(id: number): Promise<McpServer> {
  return get(`/mcp-server/${id}`)
}

export function createMcpServer(data: McpServerCreateRequest): Promise<number> {
  return post('/mcp-server', data)
}

export function updateMcpServer(id: number, data: McpServerUpdateRequest): Promise<void> {
  return put(`/mcp-server/${id}`, data)
}

export function deleteMcpServer(id: number): Promise<void> {
  return del(`/mcp-server/${id}`)
}

export function syncMcpServerTools(id: number): Promise<void> {
  return post(`/mcp-server/${id}/sync-tools`, null)
}

export function getMcpServerList(enabled?: boolean): Promise<McpServer[]> {
  return get('/mcp-server', {
    params: {
      pageNum: 1,
      pageSize: 1000,
      enabled: enabled !== undefined ? enabled : undefined,
    },
  }).then((res) => (res as PageResult<McpServer>).records || [])
}

// ==================== Agent MCP 绑定 ====================

export function getAgentMcpServers(agentId: number): Promise<number[]> {
  return get(`/agent/${agentId}/mcp-servers`)
}

export function bindAgentMcpServers(agentId: number, serverIds: number[]): Promise<void> {
  return post(`/agent/${agentId}/mcp-servers`, serverIds)
}

export function unbindAgentMcpServer(agentId: number, serverId: number): Promise<void> {
  return del(`/agent/${agentId}/mcp-servers/${serverId}`)
}
