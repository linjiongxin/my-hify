package com.hify.workflow.engine.executor;

import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.config.ToolNodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TOOL 节点执行器（v2）
 * <p>TODO: MCP 模块实现后替换为真实调用</p>
 */
@Component
public class ToolNodeExecutor implements NodeExecutor {

    @Override
    public String nodeType() {
        return "TOOL";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        try {
            ToolNodeConfig toolConfig = (ToolNodeConfig) config;

            if (toolConfig.toolName() == null || toolConfig.toolName().isEmpty()) {
                return NodeResult.failure("Tool node config missing toolName");
            }

            // 替换参数中的占位符
            Map<String, Object> resolvedParams = resolveParams(toolConfig.params(), context);

            // TODO: 等待 MCP 模块实现后替换为真实调用
            Object result = executeTool(toolConfig.toolName(), resolvedParams);

            if (toolConfig.outputVar() != null && !toolConfig.outputVar().isEmpty()) {
                context.set(node.getNodeId(), toolConfig.outputVar(), result);
                context.put(toolConfig.outputVar(), result);
            }

            return NodeResult.success(null);

        } catch (Exception e) {
            return NodeResult.failure("Tool execution failed: " + e.getMessage());
        }
    }

    private Map<String, Object> resolveParams(Map<String, Object> params, ExecutionContext context) {
        if (params == null) {
            return null;
        }
        Map<String, Object> resolved = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof String s) {
                resolved.put(entry.getKey(), context.resolve(s));
            } else {
                resolved.put(entry.getKey(), entry.getValue());
            }
        }
        return resolved;
    }

    private Object executeTool(String toolName, Map<String, Object> params) {
        throw new UnsupportedOperationException("MCP Tool Executor not yet implemented");
    }
}
