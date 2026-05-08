import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageParams as HifyPageParams } from '@/components/HifyTable.vue'

export type WorkflowStatus = 'draft' | 'published' | 'disabled'

export interface Workflow {
  id?: number
  name: string
  description?: string
  status?: WorkflowStatus
  version?: number
  retryConfig?: string
  config?: string
  createdAt?: string
  updatedAt?: string
}

export interface WorkflowCreateRequest {
  name: string
  description?: string
  retryConfig?: string
  config?: string
}

export interface WorkflowUpdateRequest {
  name?: string
  description?: string
  status?: WorkflowStatus
  retryConfig?: string
  config?: string
}

export type NodeType = 'START' | 'END' | 'LLM' | 'TOOL' | 'CONDITION' | 'APPROVAL' | 'API_CALL' | 'KNOWLEDGE'

export interface WorkflowNodeDTO {
  id?: number
  workflowId?: number | string
  nodeId: string
  type: NodeType
  name: string
  config: string
  positionX?: number
  positionY?: number
  retryConfig?: string
}

export interface WorkflowEdgeDTO {
  id?: number
  workflowId?: number | string
  sourceNode: string
  targetNode: string
  condition?: string
  edgeIndex?: number
}

export type InstanceStatus = 'running' | 'completed' | 'failed' | 'cancelled'

export interface WorkflowInstanceDTO {
  id: number
  workflowId: number
  status: InstanceStatus
  currentNodeId?: string
  context?: string
  errorMsg?: string
  startedAt?: string
  finishedAt?: string
}

export interface WorkflowStartRequest {
  workflowId: number
  inputs?: Record<string, unknown>
}

export type ApprovalStatus = 'pending' | 'approved' | 'rejected'

export interface WorkflowApprovalDTO {
  id: number
  instanceId: number
  nodeId: string
  status: ApprovalStatus
  remark?: string
  createdAt?: string
  processedAt?: string
}

export function getWorkflowPage(params: HifyPageParams & { name?: string; status?: WorkflowStatus }): Promise<PageResult<Workflow>> {
  return get('/workflow', {
    params: {
      page: params.current,
      pageSize: params.size,
      name: params.name,
      status: params.status,
    },
  })
}

export function getWorkflowDetail(id: number | string): Promise<Workflow> {
  return get(`/workflow/${id}`)
}

export function createWorkflow(data: WorkflowCreateRequest): Promise<number> {
  return post('/workflow', data)
}

export function updateWorkflow(id: number | string, data: WorkflowUpdateRequest): Promise<void> {
  return put(`/workflow/${id}`, data)
}

export function deleteWorkflow(id: number | string): Promise<void> {
  return del(`/workflow/${id}`)
}

export function getWorkflowNodes(id: number | string): Promise<WorkflowNodeDTO[]> {
  return get(`/workflow/${id}/nodes`)
}

export function getWorkflowEdges(id: number | string): Promise<WorkflowEdgeDTO[]> {
  return get(`/workflow/${id}/edges`)
}

export function saveWorkflowNodes(id: number | string, nodes: WorkflowNodeDTO[]): Promise<WorkflowNodeDTO[]> {
  return put(`/workflow/${id}/nodes`, nodes)
}

export function saveWorkflowEdges(id: number | string, edges: WorkflowEdgeDTO[]): Promise<WorkflowEdgeDTO[]> {
  return put(`/workflow/${id}/edges`, edges)
}

export function startWorkflowInstance(data: WorkflowStartRequest): Promise<string> {
  return post('/workflow/instance', data)
}

export function getWorkflowInstance(instanceId: string): Promise<WorkflowInstanceDTO> {
  return get(`/workflow/instance/${instanceId}`)
}

export function getPendingApprovals(instanceId: string): Promise<WorkflowApprovalDTO[]> {
  return get(`/workflow/instance/${instanceId}/pending-approvals`)
}

export function approveApproval(id: number, remark?: string): Promise<void> {
  return post(`/workflow/approval/${id}/approve`, null, { params: { remark } })
}

export function rejectApproval(id: number, remark?: string): Promise<void> {
  return post(`/workflow/approval/${id}/reject`, null, { params: { remark } })
}
