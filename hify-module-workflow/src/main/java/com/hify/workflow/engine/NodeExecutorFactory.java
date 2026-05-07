package com.hify.workflow.engine;

import com.hify.workflow.engine.impl.*;
import org.springframework.stereotype.Component;

/**
 * 节点执行器工厂
 * <p>根据节点类型返回对应的执行器</p>
 */
@Component
public class NodeExecutorFactory {

    /**
     * 节点类型常量
     */
    public static final String TYPE_START = "START";
    public static final String TYPE_END = "END";
    public static final String TYPE_LLM = "LLM";
    public static final String TYPE_TOOL = "TOOL";
    public static final String TYPE_CONDITION = "CONDITION";
    public static final String TYPE_APPROVAL = "APPROVAL";

    private final StartNodeExecutor startNodeExecutor;
    private final EndNodeExecutor endNodeExecutor;
    private final LLMNodeExecutor llmNodeExecutor;
    private final ToolNodeExecutor toolNodeExecutor;
    private final ConditionNodeExecutor conditionNodeExecutor;
    private final ApprovalNodeExecutor approvalNodeExecutor;

    public NodeExecutorFactory(
            StartNodeExecutor startNodeExecutor,
            EndNodeExecutor endNodeExecutor,
            LLMNodeExecutor llmNodeExecutor,
            ToolNodeExecutor toolNodeExecutor,
            ConditionNodeExecutor conditionNodeExecutor,
            ApprovalNodeExecutor approvalNodeExecutor) {
        this.startNodeExecutor = startNodeExecutor;
        this.endNodeExecutor = endNodeExecutor;
        this.llmNodeExecutor = llmNodeExecutor;
        this.toolNodeExecutor = toolNodeExecutor;
        this.conditionNodeExecutor = conditionNodeExecutor;
        this.approvalNodeExecutor = approvalNodeExecutor;
    }

    /**
     * 根据节点类型获取执行器
     *
     * @param nodeType 节点类型
     * @return 对应的执行器
     */
    public NodeExecutor getExecutor(String nodeType) {
        return switch (nodeType) {
            case TYPE_START -> startNodeExecutor;
            case TYPE_END -> endNodeExecutor;
            case TYPE_LLM -> llmNodeExecutor;
            case TYPE_TOOL -> toolNodeExecutor;
            case TYPE_CONDITION -> conditionNodeExecutor;
            case TYPE_APPROVAL -> approvalNodeExecutor;
            default -> throw new IllegalArgumentException("Unknown node type: " + nodeType);
        };
    }
}
