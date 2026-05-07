package com.hify.workflow.engine;

import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;

/**
 * 节点执行器接口
 */
public interface NodeExecutor {

    /**
     * 执行节点
     *
     * @param node     节点定义
     * @param context  执行上下文
     * @return 节点执行结果
     */
    NodeResult execute(WorkflowNode node, ExecutionContext context);
}
