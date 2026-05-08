# Agent 模块 API 文档

## 概述

Agent 是 Hify 的核心对话实体，封装了模型选择、系统提示词、工具绑定、知识库绑定和**工作流执行模式**等配置。

执行模式 `executionMode` 支持两种：

| 模式 | 值 | 说明 |
|------|-----|------|
| ReAct | `react`（默认） | 传统 ReAct 模式，直接调用 LLM 进行流式对话 |
| Workflow | `workflow` | 绑定工作流，用户消息触发工作流执行，按节点编排返回结果 |

---

## API 列表

### 一、Agent 管理

#### 1. 创建 Agent

```http
POST /api/agent
Content-Type: application/json
```

**请求体**：

```json
{
  "name": "退款客服",
  "description": "处理用户退款申请",
  "modelId": "gpt-4o",
  "systemPrompt": "你是退款客服助手，请友好地帮助用户处理退款问题。",
  "temperature": 0.7,
  "maxTokens": 2048,
  "topP": 1.0,
  "welcomeMessage": "您好，请问有什么可以帮您？",
  "enabled": true,
  "workflowId": 10,
  "executionMode": "workflow"
}
```

**字段说明**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | Agent 名称 |
| description | string | 否 | 描述 |
| modelId | string | 是 | 模型 ID（如 `gpt-4o`） |
| systemPrompt | string | 否 | 系统提示词 |
| temperature | decimal | 否 | 采样温度，默认 `0.7` |
| maxTokens | int | 否 | 最大 token 数，默认 `2048` |
| topP | decimal | 否 | 核采样，默认 `1.0` |
| welcomeMessage | string | 否 | 欢迎语 |
| enabled | boolean | 否 | 是否启用，默认 `true` |
| workflowId | long | 否 | 绑定工作流 ID（`executionMode=workflow` 时必填） |
| executionMode | string | 否 | 执行模式：`react` / `workflow`，默认 `react` |

**响应**：`200 OK`，返回 `Result<Long>`（Agent ID）

```json
{
  "code": 200,
  "message": "成功",
  "data": 100,
  "success": true
}
```

> **注意**：当 `executionMode` 未传时，默认值为 `react`。若设为 `workflow`，需同时传入有效的 `workflowId`。

---

#### 2. 获取 Agent 详情

```http
GET /api/agent/{id}
```

**响应示例**：

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 100,
    "name": "退款客服",
    "description": "处理用户退款申请",
    "modelId": "gpt-4o",
    "systemPrompt": "你是退款客服助手...",
    "temperature": 0.7,
    "maxTokens": 2048,
    "topP": 1.0,
    "welcomeMessage": "您好，请问有什么可以帮您？",
    "enabled": true,
    "workflowId": 10,
    "executionMode": "workflow",
    "createdAt": "2026-05-08T10:00:00",
    "updatedAt": "2026-05-08T10:00:00",
    "tools": [
      {
        "id": 1,
        "toolName": "query_order",
        "toolType": "builtin",
        "toolImpl": "com.hify.tool.QueryOrderTool",
        "enabled": true,
        "sortOrder": 1
      }
    ]
  },
  "success": true
}
```

---

#### 3. 更新 Agent

```http
PUT /api/agent/{id}
Content-Type: application/json
```

**请求体**（字段均为可选）：

```json
{
  "name": "退款客服 V2",
  "modelId": "claude-sonnet-4-6",
  "systemPrompt": "更新后的系统提示词",
  "workflowId": 20,
  "executionMode": "workflow",
  "enabled": true
}
```

> 可将 `workflowId` 和 `executionMode` 同时更新，实现 Agent 在不同模式间切换。

---

#### 4. 删除 Agent

```http
DELETE /api/agent/{id}
```

级联删除 Agent 绑定的工具、MCP 服务器、知识库关联。

---

#### 5. 分页查询 Agent

```http
GET /api/agent?page=1&pageSize=20
```

**响应**：`Result<PageResult<AgentVO>>`

---

### 二、工具绑定

#### 1. 绑定工具

```http
POST /api/agent/{id}/tools
Content-Type: application/json
```

**请求体**：

```json
{
  "tools": [
    {
      "toolName": "query_order",
      "toolType": "builtin",
      "toolImpl": "com.hify.tool.QueryOrderTool",
      "enabled": true,
      "sortOrder": 1
    }
  ]
}
```

---

#### 2. 替换工具（全量）

```http
PUT /api/agent/{id}/tools
Content-Type: application/json
```

先删除该 Agent 所有现有工具，再写入新列表。

---

#### 3. 解绑工具

```http
DELETE /api/agent/{id}/tools/{toolId}
```

---

### 三、MCP 服务器绑定

#### 1. 获取 MCP 服务器列表

```http
GET /api/agent/{id}/mcp-servers
```

> 当前版本预留接口，后续实现。

---

## 执行模式详解

### ReAct 模式（默认）

- 用户发送消息 → Chat 模块加载历史 → 直接调用 `LlmGatewayApi.streamChat()`
- LLM 流式返回内容通过 SSE 推送到前端
- 支持工具调用（ReAct 循环）

### Workflow 模式

- 用户发送消息 → Chat 模块启动绑定的工作流（`workflowApi.start()`）
- 推送 `event: workflow_started` SSE 事件
- 后台轮询工作流实例状态（500ms 间隔，最多 30 秒）
- 工作流完成后，从上下文提取 `reply` 或 `llmResponse` 作为回复内容
- 推送 `event: message` + `event: done`

**适用场景**：

| 场景 | 推荐模式 | 理由 |
|------|---------|------|
| 开放式问答、创意生成 | `react` | 灵活、直接调用 LLM |
| 固定业务流程（退款、审批） | `workflow` | 节点编排、条件分支、审批可控 |

---

## 错误码

| HTTP 状态 | 错误码 | 说明 |
|-----------|--------|------|
| 400 | PARAM_ERROR | 参数校验失败（如 `name` 为空） |
| 404 | DATA_NOT_FOUND | Agent / 模型 / 工作流不存在 |
| 500 | SYSTEM_ERROR | 系统内部错误 |
