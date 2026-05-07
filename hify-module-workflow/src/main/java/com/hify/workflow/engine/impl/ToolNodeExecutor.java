package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.NodeConfigParser;
import com.hify.workflow.engine.config.ToolNodeConfig;
import com.hify.workflow.engine.util.PlaceholderUtils;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具节点执行器
 * <p>调用 MCP 工具执行具体操作</p>
 */
@Component
public class ToolNodeExecutor implements NodeExecutor {

    private final NodeConfigParser nodeConfigParser;

    public ToolNodeExecutor(NodeConfigParser nodeConfigParser) {
        this.nodeConfigParser = nodeConfigParser;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            ToolNodeConfig config = (ToolNodeConfig) nodeConfigParser.parse(node);

            if (config.toolName() == null || config.toolName().isEmpty()) {
                return NodeResult.failure("Tool node config missing toolName");
            }

            // 替换参数中的占位符
            Map<String, Object> resolvedParams = resolveParams(config.params(), context);

            // 调用 MCP 工具
            Object result = executeTool(config.toolName(), resolvedParams);

            // 存入上下文
            if (config.outputVar() != null && !config.outputVar().isEmpty()) {
                context.put(config.outputVar(), result);
            }

            return NodeResult.success(null);

        } catch (Exception e) {
            return NodeResult.failure("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * 替换参数中的占位符
     */
    private Map<String, Object> resolveParams(Map<String, Object> params, ExecutionContext context) {
        if (params == null) {
            return null;
        }
        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof String s) {
                resolved.put(entry.getKey(), PlaceholderUtils.replace(s, context));
            } else {
                resolved.put(entry.getKey(), entry.getValue());
            }
        }
        return resolved;
    }

    /**
     * 执行工具
     * <p>TODO: MCP 模块实现后替换为真实调用</p>
     */
    private Object executeTool(String toolName, Map<String, Object> params) {
        // TODO: 等待 MCP 模块实现后替换为真实调用
        throw new UnsupportedOperationException("MCP Tool Executor not yet implemented");
    }
}
