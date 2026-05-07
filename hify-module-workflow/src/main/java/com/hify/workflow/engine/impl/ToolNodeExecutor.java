package com.hify.workflow.engine.impl;

import com.hify.workflow.engine.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowNode;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具节点执行器
 * <p>调用 MCP 工具执行具体操作</p>
 */
@Component
public class ToolNodeExecutor implements NodeExecutor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    // TODO: 等待 MCP 模块实现后注入 McpToolExecutor
    // private final McpToolExecutor mcpToolExecutor;

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            // 解析配置
            JSONObject config = JSONObject.parseObject(node.getConfig());
            String toolName = config.getString("toolName");
            JSONObject params = config.getJSONObject("params");
            String outputVar = config.getString("outputVar");

            if (toolName == null || toolName.isEmpty()) {
                return NodeResult.failure("Tool node config missing toolName");
            }

            // 替换参数中的占位符
            JSONObject resolvedParams = replaceParamsPlaceholders(params, context);

            // 调用 MCP 工具
            Object result = executeTool(toolName, resolvedParams);

            // 存入上下文
            if (outputVar != null && !outputVar.isEmpty()) {
                context.put(outputVar, result);
            }

            return NodeResult.success(null);

        } catch (Exception e) {
            return NodeResult.failure("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * 替换参数中的占位符
     */
    private JSONObject replaceParamsPlaceholders(JSONObject params, ExecutionContext context) {
        if (params == null) {
            return new JSONObject();
        }

        JSONObject resolved = new JSONObject();
        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value instanceof String) {
                resolved.put(key, replacePlaceholders((String) value, context));
            } else {
                resolved.put(key, value);
            }
        }
        return resolved;
    }

    /**
     * 替换字符串中的 ${variable} 占位符
     */
    private String replacePlaceholders(String template, ExecutionContext context) {
        if (template == null) {
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = context.get(varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value.toString() : ""));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 执行工具
     * <p>TODO: MCP 模块实现后替换为真实调用</p>
     */
    private Object executeTool(String toolName, JSONObject params) {
        // TODO: 等待 MCP 模块实现后替换为真实调用
        // return mcpToolExecutor.execute(toolName, params);
        throw new UnsupportedOperationException("MCP Tool Executor not yet implemented");
    }
}
