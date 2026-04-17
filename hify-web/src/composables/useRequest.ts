import { ref } from 'vue'

export interface UseRequestOptions<T, P extends unknown[]> {
  onSuccess?: (data: T) => void
  onError?: (err: unknown) => void
  initialData?: T
}

export function useRequest<T, P extends unknown[] = unknown[]>(
  api: (...args: P) => Promise<T>,
  options?: UseRequestOptions<T, P>
) {
  const data = ref<T | undefined>(options?.initialData)
  const loading = ref(false)
  const error = ref<unknown>(null)

  async function execute(...args: P): Promise<T | undefined> {
    loading.value = true
    error.value = null
    try {
      const res = await api(...args)
      data.value = res
      options?.onSuccess?.(res)
      return res
    } catch (err) {
      error.value = err
      options?.onError?.(err)
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    data,
    loading,
    error,
    execute,
  }
}
