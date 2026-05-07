package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

/**
 * START 节点执行器
 * <p>与其他节点统一：返回 success(null)，由引擎的 findNextNode 负责边解析</p>
 */
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        return NodeResult.success(null);
    }
}
