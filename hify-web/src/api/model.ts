import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageParams as HifyPageParams } from '@/components/HifyTable.vue'

export interface ModelConfig {
  id: number
  providerId: number
  name: string
  modelId: string
  maxTokens?: number
  contextWindow?: number
  capabilities?: Record<string, unknown>
  inputPricePer1m?: number
  outputPricePer1m?: number
  defaultModel: boolean
  enabled: boolean
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface ModelConfigCreateRequest {
  providerId: number
  name: string
  modelId: string
  maxTokens?: number
  contextWindow?: number
  capabilities?: Record<string, unknown>
  inputPricePer1m?: number
  outputPricePer1m?: number
  defaultModel?: boolean
  enabled?: boolean
  sortOrder?: number
}

export interface ModelConfigUpdateRequest {
  providerId: number
  name: string
  modelId: string
  maxTokens?: number
  contextWindow?: number
  capabilities?: Record<string, unknown>
  inputPricePer1m?: number
  outputPricePer1m?: number
  defaultModel?: boolean
  enabled?: boolean
  sortOrder?: number
}

export function getModelPage(params: HifyPageParams): Promise<PageResult<ModelConfig>> {
  return get('/model/page', {
    params: {
      pageNum: params.current,
      pageSize: params.size,
    },
  })
}

export function getModelDetail(id: number): Promise<ModelConfig> {
  return get(`/model/${id}`)
}

export function createModel(data: ModelConfigCreateRequest): Promise<number> {
  return post('/model', data)
}

export function updateModel(id: number, data: ModelConfigUpdateRequest): Promise<void> {
  return put(`/model/${id}`, data)
}

export function deleteModel(id: number): Promise<void> {
  return del(`/model/${id}`)
}

export function getAllEnabledModels(): Promise<ModelConfig[]> {
  return get('/model/all')
}
