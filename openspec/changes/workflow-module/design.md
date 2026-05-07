## Context

Hify 是一个面向团队内部的 AI Agent 开发平台。工作流模块让用户通过 JSON 配置定义有固定步骤的业务流程（客服对话、订单处理等），与纯 Agent 自由发挥互补。

**当前状态**：RAG 知识库模块正在开发，Agent 模块有基本配置能力，但缺少业务编排能力。

**约束**：
- PostgreSQL 15+（已用 pgvector）
- Spring Boot 3.x + MyBatis-Plus
- Redis 7（会话、SSE 流式）
- 多模块单体架构，禁止循环依赖

---

## Goals / Non-Goals

**Goals：**
- 工作流定义 CRUD（发布、版本管理）
- 异步执行引擎，支持条件分支、循环、并行
- 节点级人工审批（轮询查询 pending 状态）
- 与 Agent 模块联动：Agent 可调用工作流作为工具
- 失败策略：分支处理 > 重试 > 失败

**Non-Goals：**
- 不做可视化画布（JSON 配置即可）
- 不做复杂 DAG（只做线性执行 + 条件分支）
- 不做工作流版本 diff/回滚
- 不做子工作流嵌套

---

## Decisions

### 1. 模块结构

```
hify-module-workflow/
├── api/                    # 对外暴露的唯一入口
│   ├── WorkflowApi.java    # 工作流定义 CRUD
│   └── dto/                # DTO
├── service/                # 业务逻辑
│   ├── WorkflowDefinitionService.java
│   ├── WorkflowExecutionService.java   # 执行引擎
│   └── NodeExecutorFactory.java        # 节点执行器工厂
├── engine/                 # 执行引擎核心
│   ├── WorkflowEngine.java           # 引擎入口
│   ├── NodeExecutor.java             # 节点执行器接口
│   ├── impl/
│   │   ├── LLMNodeExecutor.java
│   │   ├── ToolNodeExecutor.java
│   │   ├── ConditionNodeExecutor.java
│   │   ├── ApprovalNodeExecutor.java
│   │   ├── StartNodeExecutor.java
│   │   └── EndNodeExecutor.java
│   └── context/
│       └── ExecutionContext.java      # 执行上下文
├── entity/                 # 数据库映射
│   ├── Workflow.java
│   ├── WorkflowNode.java
│   ├── WorkflowEdge.java
│   └── WorkflowInstance.java
├── mapper/
├── controller/
└── config/
```

**为什么**：遵循 CLAUDE.md 的模块内分层规范，`engine/` 是执行引擎专属包，不属于 service。

---

### 2. 数据库表结构

```sql
-- 工作流定义
CREATE TABLE workflow (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(500),
    status      VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',  -- DRAFT, PUBLISHED, ARCHIVED
    version     INT           NOT NULL DEFAULT 1,
    retry_config JSONB,  -- 全局重试配置 {"maxRetries": 3, "retryInterval": 3}
    config      JSONB,    -- 整个工作流的节点+连线定义
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

-- 节点定义
CREATE TABLE workflow_node (
    id             BIGINT PRIMARY KEY,
    workflow_id    BIGINT REFERENCES workflow(id),
    node_id        VARCHAR(50) NOT NULL,  -- 唯一标识，如 "node_001"
    type           VARCHAR(30) NOT NULL,  -- START, END, LLM, TOOL, CONDITION, APPROVAL
    name           VARCHAR(100),
    config         JSONB,   -- 节点配置（type 不同，config 结构不同）
    position_x     INT,
    position_y     INT,
    retry_config   JSONB,   -- 节点级重试覆盖 {"maxRetries": 1, "retryInterval": 5}
    UNIQUE(workflow_id, node_id)
);

-- 连线定义
CREATE TABLE workflow_edge (
    id             BIGINT PRIMARY KEY,
    workflow_id    BIGINT REFERENCES workflow(id),
    source_node    VARCHAR(50) NOT NULL,
    target_node    VARCHAR(50) NOT NULL,
    condition      VARCHAR(200),  -- 条件表达式，可空
    edge_index     INT DEFAULT 0  -- 同源节点多条出线时的顺序
);

-- 执行实例
CREATE TABLE workflow_instance (
    id              BIGINT PRIMARY KEY,
    workflow_id     BIGINT REFERENCES workflow(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, RUNNING, WAITING_APPROVAL, COMPLETED, FAILED
    current_node_id VARCHAR(50),
    context         JSONB,       -- 执行过程中的变量
    error_msg       TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP
);

-- 审批记录
CREATE TABLE workflow_approval (
    id              BIGINT PRIMARY KEY,
    instance_id     BIGINT REFERENCES workflow_instance(id),
    node_id         VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    remark          TEXT,
    created_at      TIMESTAMP,
    processed_at    TIMESTAMP
);
```

**为什么 JSONB 存储 config**：
- 不同节点类型字段差异大，硬拆列会导致大量 nullable 字段
- 新增节点类型不改表结构
- PostgreSQL JSONB 支持索引，方便按 type 查询

---

### 3. 节点配置 JSON 结构

```json
// START 节点（固定，无需配置）
{ "type": "START" }

// END 节点
{ "type": "END" }

// LLM 节点
{
  "type": "LLM",
  "model": "gpt-4",
  "prompt": "你是客服，回复用户：${userMessage}",
  "inputVars": ["userMessage"],
  "outputVar": "llmResponse"
}

// TOOL 节点
{
  "type": "TOOL",
  "toolName": "query_order",
  "params": {"orderId": "${orderId}"},
  "outputVar": "orderInfo"
}

// CONDITION 节点
{
  "type": "CONDITION",
  "expression": "${orderInfo.status} == '已签收'",
  "trueBranch": "node_refund",
  "falseBranch": "node_close"
}

// APPROVAL 节点
{
  "type": "APPROVAL",
  "prompt": "退款金额 ${refundAmount} 元，请审批",
  "variables": ["refundAmount"]
}
```

---

### 4. 执行引擎核心逻辑

```java
public class WorkflowEngine {

    public String start(WorkflowStartRequest request) {
        // 1. 加载工作流定义
        Workflow workflow = workflowMapper.selectById(request.getWorkflowId());

        // 2. 创建执行实例
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowId(workflow.getId());
        instance.setStatus("RUNNING");
        instance.setContext(new JSONObject());  // 初始化上下文
        instanceMapper.insert(instance);

        // 3. 找到 START 节点，异步执行
        String startNodeId = findStartNode(workflow);
        executeAsync(instance.getId(), startNodeId);

        return instance.getId().toString();
    }

    private void executeAsync(Long instanceId, String nodeId) {
        CompletableFuture.runAsync(() -> {
            ExecutionContext ctx = buildContext(instanceId);

            while (nodeId != null && !"END".equals(nodeId)) {
                // 更新当前节点
                instanceMapper.updateCurrentNode(instanceId, nodeId);

                // 获取节点执行器
                NodeExecutor executor = executorFactory.get(node.getType());

                // 执行节点
                NodeResult result = executor.execute(node, ctx);

                // 处理结果
                if (result.isRequiresApproval()) {
                    instanceMapper.updateStatus(instanceId, "WAITING_APPROVAL");
                    return;  // 暂停，等待审批
                }

                if (!result.isSuccess()) {
                    // 失败策略：分支 > 重试 > 失败
                    String nextNode = handleFailure(node, ctx, instanceId);
                    if (nextNode == null) return;
                    nodeId = nextNode;
                    continue;
                }

                // 正常流转到下一节点
                nodeId = findNextNode(node, ctx, result);
            }

            // 完成
            instanceMapper.updateStatus(instanceId, "COMPLETED");
        });
    }
}
```

---

### 5. 失败处理策略

```
节点执行失败时：
1. 检查节点/工作流是否配置了 errorBranch
   → 有：跳转到错误分支节点
2. 检查重试配置（节点级 > 工作流级 > 全局默认）
   → 可重试：按间隔重试，最多重试 maxRetries 次
   → 仍失败：标记实例 FAILED
3. 无分支、无重试配置
   → 直接标记实例 FAILED
```

**重试配置优先级**：节点级 > 工作流级 > 全局默认值（maxRetries=3, interval=3s）

---

### 6. 与 Agent 模块联动

**工作流作为 Agent 的工具**：

```java
// Agent 调用工作流时，触发以下流程：
// 1. Agent 决定调用工作流工具
// 2. 传入参数（从对话上下文中提取）
// 3. WorkflowExecutionService.start() 创建实例
// 4. 返回 instanceId 给 Agent
// 5. Agent 通过轮询或回调获取结果

public class WorkflowApi {
    // Agent 调用入口
    public WorkflowInvokeResult invokeAsTool(Long workflowId,
                                              Map<String, Object> inputs,
                                              String callbackUrl) {
        String instanceId = workflowEngine.start(workflowId, inputs);
        return new WorkflowInvokeResult(instanceId, "RUNNING");
    }
}
```

**为什么这样做**：符合 CLAUDE.md 规范，模块间通过 api 层调用，不直接依赖 Service/Mapper。

---

### 7. 人工审批流程

```
1. 执行到 APPROVAL 节点 → 状态改为 WAITING_APPROVAL
2. 前端每 N 秒轮询 /workflow/instance/{id}/pending-approvals
3. 用户审批（通过/拒绝）→ 调用 /workflow/approval/{id}
4. 审批通过 → 继续执行；拒绝 → 实例标记 FAILED
```

**为什么轮询而非 SSE 推送**：实现简单，满足 20-50 人团队使用，复杂度低。

---

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| 执行中途服务重启，实例丢失 | 实例状态存 PostgreSQL，重启后扫描 PENDING/RUNNING 状态实例恢复执行 |
| JSONB 配置无校验，运行时才发现错误 | 提供工作流预校验接口，发布前检查节点配置合法性 |
| 循环依赖导致无限循环 | 执行引擎记录已访问节点集合，超出阈值强制终止 |

---

## Open Questions

| Question | Decision |
|----------|----------|
| 并行分支是否需要支持 | 第一期不做，只支持线性执行 + 条件分支 |
| 工作流版本如何管理 | 第一期不做版本，只做状态（DRAFT/PUBLISHED/ARCHIVED） |
| 执行上下文大小限制 | 暂不限制，PostgreSQL JSONB 存储上限足够 |
