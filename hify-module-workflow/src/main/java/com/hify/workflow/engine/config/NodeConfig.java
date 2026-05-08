package com.hify.workflow.engine.config;

/**
 * 节点配置密封接口
 * <p>每种节点类型对应一个 record，编译器保证穷举检查</p>
 */
public sealed interface NodeConfig permits
        StartNodeConfig,
        EndNodeConfig,
        LlmNodeConfig,
        ToolNodeConfig,
        ConditionNodeConfig,
        ApprovalNodeConfig,
        ApiCallNodeConfig,
        KnowledgeNodeConfig {

    /**
     * 错误分支跳转目标（可选，所有节点通用）
     */
    String errorBranch();
}
