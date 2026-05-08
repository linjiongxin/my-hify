-- 将 executionMode=workflow 的 Agent 迁移为工具绑定
-- 所有 Agent 统一走 ReAct 模式，工作流作为工具绑定

INSERT INTO agent_tool (id, agent_id, tool_name, tool_type, tool_impl, enabled, sort_order, created_at, updated_at)
SELECT
    a.id * 1000000,
    a.id,
    'workflow',
    'workflow',
    a.workflow_id::varchar,
    true,
    0,
    now(),
    now()
FROM agent a
WHERE a.execution_mode = 'workflow' AND a.workflow_id IS NOT NULL;

-- 更新所有 workflow 模式的 Agent 为 react 模式
UPDATE agent SET execution_mode = 'react' WHERE execution_mode = 'workflow';
