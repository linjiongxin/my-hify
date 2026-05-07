package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * END 节点执行器
 * <p>返回 NodeResult with nextNodeId = null（结束）</p>
 */
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        // END 节点不执行任何操作，直接结束流程
        return NodeResult.end();
    }
}
