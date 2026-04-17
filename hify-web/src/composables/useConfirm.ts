import { ElMessageBox } from 'element-plus'
import { notifySuccess, notifyError } from '@/utils/notify'

export interface UseConfirmOptions {
  title?: string
  message?: string
  confirmButtonText?: string
  cancelButtonText?: string
  successMessage?: string
}

export function useConfirm() {
  async function confirmDelete<T>(
    api: () => Promise<T>,
    options?: UseConfirmOptions
  ): Promise<T | undefined> {
    try {
      await ElMessageBox.confirm(
        options?.message ?? '删除后不可恢复，是否继续？',
        options?.title ?? '确认删除',
        {
          confirmButtonText: options?.confirmButtonText ?? '删除',
          cancelButtonText: options?.cancelButtonText ?? '取消',
          type: 'warning',
          beforeClose: undefined,
        }
      )
      const res = await api()
      notifySuccess(options?.successMessage ?? '删除成功')
      return res
    } catch (err: unknown) {
      if (err === 'cancel' || (err as Error)?.message === 'cancel') {
        return undefined
      }
      notifyError('删除失败，请稍后重试')
      throw err
    }
  }

  return {
    confirmDelete,
  }
}
