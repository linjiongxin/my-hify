# Agent 前端任务清单

> 变更：agent-frontend
> 创建时间：2026-04-21

---

## 任务列表

### Task 1: 创建 API 调用模块

- [ ] 创建 `hify-web/src/api/agent.ts`
- [ ] 定义 TypeScript 类型（Agent, AgentCreateRequest, AgentUpdateRequest, AgentToolBatchRequest）
- [ ] 实现 API 方法（getAgentPage, getAgentDetail, createAgent, updateAgent, deleteAgent, getAgentTools, bindAgentTools, replaceAgentTools, unbindAgentTool）
- [ ] 提交

### Task 2: 创建 Agent 列表页

- [ ] 创建 `hify-web/src/views/agent/AgentList.vue`
- [ ] 实现表格列配置（名称、模型、描述、状态、创建时间、操作）
- [ ] 实现新增/编辑表单
- [ ] 实现工具绑定管理（列表、添加、删除）
- [ ] 实现删除确认
- [ ] 提交

### Task 3: 修改路由配置

- [ ] 修改 `hify-web/src/router/index.ts`
- [ ] Agent 路由指向 `AgentList.vue`
- [ ] 提交

---

## 任务顺序

```
Task 1 → Task 2 → Task 3
```

Task 1 和 Task 3 可以并行，但 Task 2 依赖 Task 1。
