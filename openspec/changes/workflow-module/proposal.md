## Why

Hify 需要一个工作流引擎，让用户通过 JSON 配置定义有固定步骤的业务流程（如客服对话、订单处理），而不是依赖 Agent 自由发挥。RAG 知识库模块已在推进，工作流是 Agent 执行确定性业务操作的基础设施。

## What Changes

- 新增 `hify-module-workflow` 模块，包含工作流定义、执行引擎
- 工作流由节点（LLM 节点、工具节点、条件节点）和连线构成
- 执行引擎按拓扑顺序遍历节点，条件节点根据表达式选择分支
- 支持断点恢复（执行到一半可以暂停继续）
- 与 Agent 模块联动：Agent 可以调用工作流，工作流可以包含 LLM 调用节点

## Capabilities

### New Capabilities
- `workflow-definition`: 工作流定义管理（CRUD、版本）
- `workflow-execution`: 工作流执行引擎（节点调度、条件分支、断点恢复）
- `workflow-node-llm`: LLM 调用节点类型
- `workflow-node-tool`: 工具调用节点类型
- `workflow-node-condition`: 条件分支节点类型

### Modified Capabilities
<!-- 工作流模块是新模块，无现有规格修改 -->

## Impact

- 新增模块：`hify-module-workflow`
- 数据库：新增 `workflow`, `workflow_node`, `workflow_edge`, `workflow_instance` 四张表
- API：工作流定义 CRUD、触发执行、查询执行状态
- 依赖：Agent 模块（工作流被 Agent 调用）、工具模块（工作流节点调用工具）