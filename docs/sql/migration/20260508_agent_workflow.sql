-- Agent 表增加工作流关联字段
ALTER TABLE agent
    ADD COLUMN IF NOT EXISTS workflow_id BIGINT,
    ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(20) DEFAULT 'react';

COMMENT ON COLUMN agent.workflow_id IS '绑定主工作流 ID';
COMMENT ON COLUMN agent.execution_mode IS '执行模式: react / workflow';
