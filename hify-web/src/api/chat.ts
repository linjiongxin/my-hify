import { get, post, del } from '@/utils/request'

export interface ChatSession {
  id: number
  userId: number
  agentId: number
  title: string
  modelId?: string
  status: string
  messageCount: number
  lastMessageAt?: string
  createdAt: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  seq: number
  role: string
  content: string
  status: string
  finishReason?: string
  durationMs?: number
  inputTokens?: number
  outputTokens?: number
  model?: string
  createdAt: string
}

export interface CreateSessionRequest {
  agentId: number
  firstMessage?: string
}

export function createSession(data: CreateSessionRequest): Promise<ChatSession> {
  return post('/chat/session', data)
}

export function listSessions(): Promise<ChatSession[]> {
  return get('/chat/sessions')
}

export function listMessages(sessionId: number): Promise<ChatMessage[]> {
  return get(`/chat/session/${sessionId}/messages`)
}

export function archiveSession(sessionId: number): Promise<void> {
  return post(`/chat/session/${sessionId}/archive`)
}

export function deleteSession(sessionId: number): Promise<void> {
  return del(`/chat/session/${sessionId}`)
}

export function streamChat(
  sessionId: number,
  message: string,
  onMessage: (content: string) => void,
  onDone: () => void,
  onError: (error: string) => void
): () => void {
  const url = `/api/chat/stream/${sessionId}?message=${encodeURIComponent(message)}`
  const eventSource = new EventSource(url)

  eventSource.addEventListener('message', (e) => {
    try {
      const data = JSON.parse(e.data)
      if (data.content) {
        onMessage(data.content)
      }
    } catch {
      // ignore parse error
    }
  })

  eventSource.addEventListener('done', () => {
    onDone()
    eventSource.close()
  })

  eventSource.addEventListener('error', (e) => {
    try {
      const data = JSON.parse((e as MessageEvent).data || '{}')
      onError(data.message || '流式响应出错')
    } catch {
      onError('流式响应出错')
    }
    eventSource.close()
  })

  eventSource.onerror = () => {
    onError('SSE 连接中断')
    eventSource.close()
  }

  return () => eventSource.close()
}
