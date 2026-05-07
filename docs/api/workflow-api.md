# 工作流模块 API 文档

## 概述

工作流引擎支持 6 种节点类型，节点间通过连线（Edge）构成有向图。连线可配置条件表达式，引擎按条件优先级匹配决定流程走向。

---

## API 列表

### 一、工作流定义管理

#### 1. 创建工作流
```http
POST /api/workflow
Content-Type: application/json
```

**请求体**：
```json
{
  "name": "退款审批流程",
  "description": "用户申请退款后的自动审批流",
  "retryConfig": "{\"maxRetries\":3,\"retryIntervalSeconds\":3}",
  "config": "{}"
}
```

**响应**：`200 OK`，返回 `Long` 工作流 ID

> **注意**：当前 create 接口只保存工作流元数据，节点和连线需要通过单独的保存接口写入（见下方> 缺失接口说明）。

---

#### 2. 更新工作流
```http
PUT /api/workflow/{id}
Content-Type: application/json
```

**请求体**（字段均为可选）：
```json
{
  "name": "新名称",
  "description": "新描述",
  "status": "published",
  "retryConfig": "{\"maxRetries\":2,\"retryIntervalSeconds\":5}",
  "config": "{}"
}
```

---

#### 3. 删除工作流
```http
DELETE /api/workflow/{id}
```

级联删除：节点、连线、实例全部删除。

---

#### 4. 获取工作流详情
```http
GET /api/workflow/{id}
```

---

#### 5. 分页查询工作流
```http
GET /api/workflow?page=1&pageSize=20&name=退款&status=published
```

---

#### 6. 保存节点（全量替换）
```http
PUT /api/workflow/{id}/nodes
Content-Type: application/json
```

**请求体**：节点列表（全量替换旧节点）
```json
[
  {
    "nodeId": "node_start",
    "type": "START",
    "name": "开始",
    "config": "{}",
    "positionX": 100,
    "positionY": 100
  },
  {
    "nodeId": "node_llm",
    "type": "LLM",
    "name": "评分",
    "config": "{\"model\":\"gpt-4\",\"prompt\":\"评分：${content}\",\"outputVar\":\"score\"}",
    "positionX": 300,
    "positionY": 100
  }
]
```

**响应**：保存后的节点列表（含生成的 `id`）

> 注意：调用后旧节点会被全部删除，再写入新节点。

---

#### 7. 获取节点列表
```http
GET /api/workflow/{id}/nodes
```

**响应示例**：
```json
[
  {
    "id": 1,
    "workflowId": 1,
    "nodeId": "node_start",
    "type": "START",
    "name": "开始",
    "config": "{}",
    "positionX": 100,
    "positionY": 100
  },
  {
    "id": 2,
    "workflowId": 1,
    "nodeId": "node_llm",
    "type": "LLM",
    "name": "评分",
    "config": "{\"model\":\"gpt-4\",\"prompt\":\"评分：${content}\",\"outputVar\":\"score\"}",
    "positionX": 300,
    "positionY": 100
  }
]
```

---

#### 8. 保存连线（全量替换）
```http
PUT /api/workflow/{id}/edges
Content-Type: application/json
```

**请求体**：连线列表（全量替换旧连线）
```json
[
  {
    "sourceNode": "node_start",
    "targetNode": "node_llm",
    "condition": null,
    "edgeIndex": 0
  },
  {
    "sourceNode": "node_llm",
    "targetNode": "node_approval",
    "condition": "${score} >= 80",
    "edgeIndex": 0
  }
]
```

**响应**：保存后的连线列表（含生成的 `id`）

> 注意：调用后旧连线会被全部删除，再写入新连线。

---

#### 9. 获取连线列表
```http
GET /api/workflow/{id}/edges
```

**响应示例**：
```json
[
  {
    "id": 1,
    "workflowId": 1,
    "sourceNode": "node_start",
    "targetNode": "node_llm",
    "condition": null,
    "edgeIndex": 0
  },
  {
    "id": 2,
    "workflowId": 1,
    "sourceNode": "node_llm",
    "targetNode": "node_approval",
    "condition": "${score} >= 80",
    "edgeIndex": 0
  },
  {
    "id": 3,
    "workflowId": 1,
    "sourceNode": "node_llm",
    "targetNode": "node_reject",
    "condition": "${score} < 80",
    "edgeIndex": 1
  }
]
```

---

### 二、工作流执行

#### 1. 启动工作流
```http
POST /api/workflow/instance
Content-Type: application/json
```

**请求体**：
```json
{
  "workflowId": 1,
  "inputs": {
    "userId": "123",
    "content": "我要退款"
  }
}
```

**响应**：`200 OK`，返回 `String` 实例 ID

---

#### 2. 查询实例状态
```http
GET /api/workflow/instance/{instanceId}
```

**响应示例**：
```json
{
  "id": 100,
  "workflowId": 1,
  "status": "RUNNING",
  "currentNodeId": "node_approval",
  "context": "{\"userId\":\"123\",\"score\":\"85\"}",
  "startedAt": "2026-05-07T10:00:00",
  "finishedAt": null
}
```

---

### 三、审批

#### 1. 审批通过
```http
POST /api/workflow/approval/{id}/approve?remark=同意退款
```

审批通过后，流程会从当前审批节点继续，走 `approveBranch` 配置的分支（如果配置了）。

---

#### 2. 审批拒绝
```http
POST /api/workflow/approval/{id}/reject?remark=不符合退款条件
```

拒绝后，流程会从当前审批节点继续，走 `rejectBranch` 配置的分支（如果配置了）。

---

#### 3. 查询待审批列表
```http
GET /api/workflow/instance/{instanceId}/pending-approvals
```

---

## 节点配置 JSON 格式

每种节点类型的 `config` 字段为 JSON 字符串，结构如下：

### START / END
```json
{}
```

### LLM
```json
{
  "model": "gpt-4",
  "prompt": "你是客服，回复用户：${userMessage}",
  "outputVar": "llmResponse"
}
```

### TOOL
```json
{
  "toolName": "query_order",
  "params": {
    "orderId": "${orderId}"
  },
  "outputVar": "orderInfo"
}
```

### CONDITION
```json
{
  "expression": "${score} >= 80",
  "trueBranch": "node_approval",
  "falseBranch": "node_reject"
}
```

### APPROVAL
```json
{
  "prompt": "退款金额 ${refundAmount} 元，请审批",
  "approveBranch": "node_refund_success",
  "rejectBranch": "node_refund_fail"
}
```

> `approveBranch` / `rejectBranch` 为可选字段。未配置时，审批完成后走默认连线。

---

## 条件连线

### 前端如何传递

在保存连线时，`condition` 字段传 SpEL 表达式字符串：

```json
{
  "sourceNode": "node_llm",
  "targetNode": "node_approval",
  "condition": "${score} >= 80",
  "edgeIndex": 0
}
```

### 匹配规则

1. 引擎按 `edgeIndex` 升序遍历连线
2. 对每条有 `condition` 的连线：
   - 替换 `${variable}` 为上下文中的实际值
   - 使用 **SpEL** 评估表达式
   - 第一个评估为 `true` 的连线被选中
3. 如果所有条件都不匹配，走 `condition` 为 `null` 或空字符串的**默认连线**
4. 没有默认连线时，流程结束

### 支持的表达式语法

基于 **Spring Expression Language (SpEL)** 子集：

| 示例 | 说明 |
|------|------|
| `${score} > 80` | 数值比较 |
| `${status} == 'approved'` | 字符串比较 |
| `${score} >= 80 && ${score} < 100` | 逻辑与 |
| `${flag} == true` | 布尔值比较 |

> 字符串值会自动加单引号，前端写表达式时直接用 `${var}` 即可。

---

