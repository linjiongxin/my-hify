package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * START 节点执行器（v2）
 */
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "START";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        return NodeResult.success(null);
    }
}
