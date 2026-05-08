package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * END 节点执行器（v2）
 */
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "END";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        return NodeResult.end();
    }
}
