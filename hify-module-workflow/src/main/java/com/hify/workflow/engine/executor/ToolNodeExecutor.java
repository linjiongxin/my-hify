package com.hify.workflow.engine.executor;

import com.hify.mcp.api.McpApi;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.config.ToolNodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TOOL 节点执行器
 * <p>调用 MCP 外部工具</p>
 */
@Component
@RequiredArgsConstructor
public class ToolNodeExecutor implements NodeExecutor {

    private final McpApi mcpApi;

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

            if (toolConfig.mcpServerUrl() == null || toolConfig.mcpServerUrl().isBlank()) {
                return NodeResult.failure("Tool node config missing mcpServerUrl");
            }

            // 替换参数中的占位符
            Map<String, Object> resolvedParams = resolveParams(toolConfig.params(), context);

            String result = mcpApi.callTool(toolConfig.mcpServerUrl(), toolConfig.toolName(), resolvedParams);

            if (toolConfig.outputVar() != null && !toolConfig.outputVar().isEmpty()) {
                context.set(node.getNodeId(), toolConfig.outputVar(), result);
                context.put(toolConfig.outputVar(), result);
            }

            return NodeResult.end();

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
}
