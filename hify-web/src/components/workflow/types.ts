import type { Node, Edge } from '@vue-flow/core'
import type { WorkflowNodeDTO, WorkflowEdgeDTO, NodeType } from '@/api/workflow'

export interface FlowNodeData {
  name: string
  type: NodeType
  config: Record<string, unknown>
}

export type FlowNode = Node<FlowNodeData>
export type FlowEdge = Edge

export function toFlowNode(dto: WorkflowNodeDTO): FlowNode {
  return {
    id: dto.nodeId,
    type: 'default',
    position: { x: dto.positionX ?? 0, y: dto.positionY ?? 0 },
    data: {
      name: dto.name,
      type: dto.type,
      config: dto.config ? JSON.parse(dto.config) : {},
    },
    label: dto.name,
  }
}

export function toFlowEdge(dto: WorkflowEdgeDTO): FlowEdge {
  return {
    id: `e-${dto.sourceNode}-${dto.targetNode}`,
    source: dto.sourceNode,
    target: dto.targetNode,
    label: dto.condition || undefined,
    data: {
      condition: dto.condition,
      edgeIndex: dto.edgeIndex,
    },
  }
}

export function toNodeDTO(node: any, workflowId: number | string): WorkflowNodeDTO {
  return {
    workflowId,
    nodeId: node.id,
    type: node.data?.type ?? 'LLM',
    name: node.data?.name ?? node.id,
    config: JSON.stringify(node.data?.config ?? {}),
    positionX: Math.round(node.position?.x ?? 0),
    positionY: Math.round(node.position?.y ?? 0),
  }
}

export function toEdgeDTO(edge: any, workflowId: number | string): WorkflowEdgeDTO {
  return {
    workflowId,
    sourceNode: edge.source,
    targetNode: edge.target,
    condition: edge.data?.condition || undefined,
    edgeIndex: edge.data?.edgeIndex || undefined,
  }
}

export const NODE_TYPE_LABELS: Record<NodeType, string> = {
  START: '开始',
  END: '结束',
  LLM: 'LLM',
  TOOL: '工具',
  CONDITION: '条件分支',
  APPROVAL: '审批',
  API_CALL: 'API 调用',
  KNOWLEDGE: '知识库',
}

export const NODE_TYPE_COLORS: Record<NodeType, string> = {
  START: '#67C23A',
  END: '#F56C6C',
  LLM: '#8B5CF6',
  TOOL: '#E6A23C',
  CONDITION: '#F59E0B',
  APPROVAL: '#9254DE',
  API_CALL: '#13C2C2',
  KNOWLEDGE: '#52C41A',
}
