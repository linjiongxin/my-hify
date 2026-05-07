## 1. 模块初始化

- [ ] 1.1 创建 `hify-module-workflow` 模块目录结构（api/service/engine/entity/mapper/controller/config）
- [ ] 1.2 创建 `pom.xml`，依赖 hify-common-core、hify-common-web、mybatis-plus、postgresql
- [ ] 1.3 在 `hify-server/pom.xml` 中添加模块依赖

## 2. 数据库实体

- [ ] 2.1 创建 `Workflow` 实体类（对应 workflow 表）
- [ ] 2.2 创建 `WorkflowNode` 实体类（对应 workflow_node 表）
- [ ] 2.3 创建 `WorkflowEdge` 实体类（对应 workflow_edge 表）
- [ ] 2.4 创建 `WorkflowInstance` 实体类（对应 workflow_instance 表）
- [ ] 2.5 创建 `WorkflowApproval` 实体类（对应 workflow_approval 表）

## 3. Mapper 层

- [ ] 3.1 创建 `WorkflowMapper`（CRUD + 分页查询已发布工作流）
- [ ] 3.2 创建 `WorkflowNodeMapper`（按 workflowId 查询节点列表）
- [ ] 3.3 创建 `WorkflowEdgeMapper`（按 workflowId 查询连线列表）
- [ ] 3.4 创建 `WorkflowInstanceMapper`（创建实例、更新状态、查询进行中的实例）
- [ ] 3.5 创建 `WorkflowApprovalMapper`（创建审批记录、查询待审批）

## 4. API 层

- [ ] 4.1 定义 `WorkflowApi` 接口（CRUD + 触发执行）
- [ ] 4.2 创建 `WorkflowDTO`、`WorkflowNodeDTO`、`WorkflowEdgeDTO`
- [ ] 4.3 创建 `WorkflowCreateRequest`、`WorkflowUpdateRequest`、`WorkflowStartRequest` DTO

## 5. 执行引擎核心

- [ ] 5.1 创建 `ExecutionContext`（执行上下文，存储变量）
- [ ] 5.2 创建 `NodeExecutor` 接口（执行器抽象）
- [ ] 5.3 创建 `NodeExecutorFactory`（根据 type 获取执行器）
- [ ] 5.4 实现 `StartNodeExecutor`
- [ ] 5.5 实现 `EndNodeExecutor`
- [ ] 5.6 实现 `LLMNodeExecutor`（调用模型网关）
- [ ] 5.7 实现 `ToolNodeExecutor`（调用 MCP 工具）
- [ ] 5.8 实现 `ConditionNodeExecutor`（条件分支）
- [ ] 5.9 实现 `ApprovalNodeExecutor`（创建审批记录）

## 6. 执行引擎编排

- [ ] 6.1 创建 `WorkflowEngine`（启动实例、节点调度、失败处理）
- [ ] 6.2 实现失败策略：errorBranch 跳转
- [ ] 6.3 实现重试策略：节点级 > 工作流级 > 全局默认
- [ ] 6.4 实现断点恢复：扫描 PENDING/RUNNING 实例并恢复
- [ ] 6.5 实现 `WorkflowExecutionService`（编排 CRUD + 执行）

## 7. Controller 层

- [ ] 7.1 创建 `WorkflowController`（定义 CRUD HTTP 接口）
- [ ] 7.2 创建 `WorkflowInstanceController`（触发执行、查询状态）
- [ ] 7.3 创建 `WorkflowApprovalController`（审批接口）

## 8. 与 Agent 模块联动

- [ ] 8.1 在 `hify-module-agent` 添加 `WorkflowTool`（实现 Tool 接口）
- [ ] 8.2 Agent 调用工作流时传入参数并获取结果

## 9. 人工审批前端支持

- [ ] 9.1 创建 `/workflow/instance/{id}/pending-approvals` 接口
- [ ] 9.2 创建 `/workflow/approval/{id}` 接口（通过/拒绝）
