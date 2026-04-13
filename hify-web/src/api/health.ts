import { get } from '@/utils/request'

export interface HealthResult {
  status: string
}

export function getHealth(): Promise<HealthResult> {
  return get<HealthResult>('/v1/health')
}
