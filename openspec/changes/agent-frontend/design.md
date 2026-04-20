# Agent 前端设计

> 变更：agent-frontend
> 创建时间：2026-04-21
> 状态：designed

---

## 1. 背景与目标

### 1.1 当前状态
- 后端 Agent CRUD API 已完成
- 前端 `/agents` 路由指向 PlaceholderView.vue

### 1.2 目标
实现 Agent 管理的完整前端页面，包括列表页和表单弹窗。

---

## 2. 页面结构

```
/agents → AgentList.vue（替换 PlaceholderView.vue）
```

---

## 3. 文件清单

### 新增
- `hify-web/src/api/agent.ts` — API 调用
- `hify-web/src/views/agent/AgentList.vue` — Agent 列表页

### 修改
- `hify-web/src/router/index.ts` — Agent 路由指向真实组件

---

## 4. API 接口（前端调用后端）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /agent | 分页查询 |
| GET | /agent/{id} | 详情 |
| POST | /agent | 创建 |
| PUT | /agent/{id} | 更新 |
| DELETE | /agent/{id} | 删除 |
| GET | /agent/{id}/tools | 工具列表 |
| POST | /agent/{id}/tools | 批量添加工具 |
| PUT | /agent/{id}/tools | 批量替换工具 |
| DELETE | /agent/{id}/tools/{toolId} | 删除工具 |

---

## 5. Agent 列表页功能

### 5.1 表格列

| 列名 | 字段 | 说明 |
|------|------|------|
| 名称 | name | Agent 名称 |
| 模型 | modelId | 模型标识 |
| 描述 | description | 描述 |
| 状态 | enabled | 启用/禁用 |
| 创建时间 | createdAt | 创建时间 |
| 操作 | - | 编辑/删除 |

### 5.2 表单字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | input | ✅ | Agent 名称 |
| modelId | select | ✅ | 从 model_config 列表选择 |
| description | textarea | - | 描述 |
| systemPrompt | textarea | - | 系统提示词 |
| temperature | number | - | 温度 (0-1)，默认 0.7 |
| maxTokens | number | - | 最大 Token，默认 2048 |
| topP | number | - | TopP (0-1)，默认 1.0 |
| welcomeMessage | textarea | - | 欢迎语 |
| enabled | switch | - | 是否启用，默认开启 |

### 5.3 工具绑定（嵌套在表单内）

| 功能 | 说明 |
|------|------|
| 工具列表 | 显示已绑定工具（名称、类型、启用状态） |
| 添加工具 | 选择类型（builtin/mcp）、填写工具名、实现标识 |
| 批量替换 | 全量替换工具列表 |
| 删除工具 | 删除单个工具 |

---

## 6. 组件复用

- `HifyTable` — 列表展示
- `HifyFormDialog` — 表单弹窗
- `useConfirm` — 删除确认
- `notifySuccess` — 操作成功提示

---

## 7. 技术栈

- Vue 3 + TypeScript
- Element Plus
- Pinia（无复杂状态，暂不需要）
- API 调用复用现有 `request.ts` 封装

---

## 8. 依赖关系

```
后端 API（已完成）
├── GET  /agent              → 分页
├── POST /agent              → 创建
├── GET  /agent/{id}         → 详情
├── PUT  /agent/{id}          → 更新
├── DELETE /agent/{id}        → 删除
└── 工具绑定相关接口

前端
├── api/agent.ts            → 调用后端 API
├── views/agent/AgentList.vue → 页面组件
└── router/index.ts         → 路由配置
```
