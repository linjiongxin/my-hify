import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    if (res.code === 200) {
      return res.data as unknown as AxiosResponse
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(res)
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, config) as Promise<T>
}

export function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.post(url, data, config) as Promise<T>
}

export function put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request.put(url, data, config) as Promise<T>
}

export function del<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request.delete(url, config) as Promise<T>
}

export default request
