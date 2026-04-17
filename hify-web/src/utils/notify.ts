import { ElMessage, type MessageOptions } from 'element-plus'

type MessageType = 'success' | 'error' | 'warning'

const baseConfig: Partial<MessageOptions> = {
  duration: 3000,
  showClose: true,
  grouping: true,
}

function notify(type: MessageType, message: string, options?: Partial<MessageOptions>) {
  ElMessage({
    ...baseConfig,
    ...options,
    type,
    message,
  })
}

export function notifySuccess(message: string, options?: Partial<MessageOptions>) {
  notify('success', message, options)
}

export function notifyError(message: string, options?: Partial<MessageOptions>) {
  notify('error', message, options)
}

export function notifyWarning(message: string, options?: Partial<MessageOptions>) {
  notify('warning', message, options)
}
