# 对话模块 API 文档

## 概述

对话模块提供会话管理、历史消息查询和**流式对话（SSE）**能力。

流式对话支持两种执行模式，由绑定的 Agent 的 `executionMode` 字段决定：

| 模式 | SSE 事件序列 |
|------|-------------|
| ReAct (`react`) | `message` → ... → `message` → `done` |
| Workflow (`workflow`) | `workflow_started` → `message` → `done` |

---

## API 列表

### 一、会话管理

#### 1. 创建会话

```http
POST /api/chat/session
Content-Type: application/json
Authorization: Bearer {token}
```

**请求体**：

```json
{
  "agentId": 100,
  "firstMessage": "可选的首条消息"
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | long | 是 | 关联的 Agent ID |
| firstMessage | string | 否 | 创建时直接发送的首条消息（当前版本未使用） |

**响应**：`Result<ChatSessionVO>`

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 200,
    "userId": 1,
    "agentId": 100,
    "title": "新对话",
    "modelId": "gpt-4o",
    "status": "active",
    "messageCount": 0,
    "lastMessageAt": null,
    "createdAt": "2026-05-08T10:00:00"
  },
  "success": true
}
```

---

#### 2. 获取当前用户会话列表

```http
GET /api/chat/sessions
Authorization: Bearer {token}
```

**响应**：`Result<List<ChatSessionVO>>`

---

#### 3. 获取会话历史消息

```http
GET /api/chat/session/{sessionId}/messages
Authorization: Bearer {token}
```

**响应**：`Result<List<ChatMessageVO>>`

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1001,
      "sessionId": 200,
      "seq": 1,
      "role": "user",
      "content": "我要退款",
      "status": "completed",
      "finishReason": null,
      "durationMs": null,
      "inputTokens": null,
      "outputTokens": null,
      "model": null,
      "createdAt": "2026-05-08T10:00:00"
    },
    {
      "id": 1002,
      "sessionId": 200,
      "seq": 2,
      "role": "assistant",
      "content": "好的，我来帮您处理退款申请...",
      "status": "completed",
      "finishReason": "stop",
      "durationMs": 1500,
      "inputTokens": 120,
      "outputTokens": 80,
      "model": "workflow:2052561816407248897",
      "createdAt": "2026-05-08T10:00:01"
    }
  ],
  "success": true
}
```

> **注意**：Workflow 模式下，`model` 字段格式为 `workflow:{instanceId}`，用于追溯工作流实例。

---

#### 4. 归档会话

```http
POST /api/chat/session/{sessionId}/archive
Authorization: Bearer {token}
```

---

#### 5. 删除会话

```http
DELETE /api/chat/session/{sessionId}
Authorization: Bearer {token}
```

---

### 二、流式对话（SSE）

```http
GET /api/chat/stream/{sessionId}?message={message}&token={token}
Accept: text/event-stream
```

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | long | 是 | 会话 ID（路径参数） |
| message | string | 是 | 用户消息内容（需 URL 编码） |
| token | string | 条件 | JWT Token。SSE 无法设置自定义 Header，故通过 query 参数传递 |

**SSE 事件格式**：

#### ReAct 模式事件流

```
event:message
id:uuid-1
data:{"content":"你好","timestamp":1704067200000}

event:message
id:uuid-2
data:{"content":"！","timestamp":1704067200100}

event:done
data:{"status":"completed"}
```

#### Workflow 模式事件流

```
event:workflow_started
data:{"status":"RUNNING","instanceId":"2052561816407248897"}

event:message
data:{"content":"好的，我来帮您处理退款申请...","timestamp":1704067200000}

event:done
data:{"status":"completed"}
```

#### 错误事件

```
event:error
data:{"code":"WORKFLOW_FAILED","message":"LLM execution failed: ..."}
```

#### 超时事件（工作流长时间未结束，如卡在审批节点）

```
event:workflow_pending
data:{"instanceId":"2052561816407248897","message":"工作流执行中，请稍后刷新查看结果"}
```

**事件说明**：

| 事件名 | 触发条件 | 前端处理 |
|--------|---------|---------|
| `workflow_started` | Workflow 模式，工作流实例创建成功 | 显示"工作流执行中"状态 |
| `message` | 收到内容片段（ReAct 逐字 / Workflow 完整结果） | 追加到对话气泡 |
| `done` | 流式结束 | 关闭 EventSource，解除输入框禁用 |
| `error` | 执行异常（LLM 失败、工作流失败等） | 显示错误提示，关闭连接 |
| `workflow_pending` | 轮询 30 秒未结束 | 提示用户稍后刷新，前端可独立轮询实例状态 |

---

## 前端接入示例

### JavaScript (EventSource)

```javascript
const sessionId = 200;
const message = '我要退款';
const token = 'eyJhbGciOiJIUzUxMiJ9...';

const eventSource = new EventSource(
  `/api/chat/stream/${sessionId}?message=${encodeURIComponent(message)}&token=${token}`
);

eventSource.addEventListener('workflow_started', (e) => {
  const data = JSON.parse(e.data);
  console.log('工作流已启动，实例ID:', data.instanceId);
  showLoading('工作流执行中...');
});

eventSource.addEventListener('message', (e) => {
  const data = JSON.parse(e.data);
  appendMessage(data.content);
});

eventSource.addEventListener('done', (e) => {
  hideLoading();
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  const data = JSON.parse(e.data);
  showError(data.message);
  eventSource.close();
});

eventSource.onerror = (e) => {
  if (eventSource.readyState === EventSource.CLOSED) {
    return;
  }
  console.log('SSE 连接中断，浏览器自动重连中...');
};
```

---

## 错误码

| HTTP 状态 | 错误码 | 说明 |
|-----------|--------|------|
| 400 | PARAM_ERROR | 参数错误（如 message 为空） |
| 401 | UNAUTHORIZED | 未登录或 Token 无效 |
| 404 | DATA_NOT_FOUND | 会话不存在、Agent 不存在或已禁用 |
| 500 | SYSTEM_ERROR | LLM 网关异常、工作流引擎异常 |
