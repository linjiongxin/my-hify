package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;

/**
 * 节点执行器接口（v2）
 * <p>支持 Spring 自动注册到 {@link NodeExecutorRegistry}</p>
 */
public interface NodeExecutor {

    /**
     * 节点类型标识，用于 Registry 自动分发
     */
    String nodeType();

    /**
     * 执行节点
     *
     * @param node    节点定义
     * @param config  预解析的节点配置
     * @param context 执行上下文
     * @return 执行结果
     */
    NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context);
}
