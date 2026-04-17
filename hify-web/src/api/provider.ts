import { get, post, put, del } from '@/utils/request'
import type { PageResult, PageParams as HifyPageParams } from '@/components/HifyTable.vue'

export interface Provider {
  id: number
  name: string
  code: string
  protocolType: string
  apiBaseUrl: string
  authType: string
  apiKey: string
  authConfig?: Record<string, unknown>
  enabled: boolean
  sortOrder: number
  healthStatus?: string
  createdAt: string
  updatedAt: string
}

export interface ProviderCreateRequest {
  name: string
  code: string
  protocolType: string
  apiBaseUrl: string
  authType: string
  apiKey?: string
  authConfig?: Record<string, unknown>
  enabled?: boolean
  sortOrder?: number
}

export interface ProviderUpdateRequest {
  name: string
  protocolType: string
  apiBaseUrl: string
  authType: string
  apiKey?: string
  authConfig?: Record<string, unknown>
  enabled?: boolean
  sortOrder?: number
}

export function getProviderPage(params: HifyPageParams): Promise<PageResult<Provider>> {
  return get('/model-provider/page', {
    params: {
      pageNum: params.current,
      pageSize: params.size,
    },
  })
}

export function getProviderDetail(id: number): Promise<Provider> {
  return get(`/model-provider/${id}`)
}

export function createProvider(data: ProviderCreateRequest): Promise<number> {
  return post('/model-provider', data)
}

export function updateProvider(id: number, data: ProviderUpdateRequest): Promise<void> {
  return put(`/model-provider/${id}`, data)
}

export function deleteProvider(id: number): Promise<void> {
  return del(`/model-provider/${id}`)
}
