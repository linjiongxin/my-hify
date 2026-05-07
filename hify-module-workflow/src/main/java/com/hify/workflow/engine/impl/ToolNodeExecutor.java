package com.hify.workflow.engine.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具节点执行器
 * <p>调用 MCP 工具执行具体操作</p>
 */
@Component
public class ToolNodeExecutor implements NodeExecutor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            // 解析配置
            JsonNode config = objectMapper.readTree(node.getConfig());
            String toolName = config.has("toolName") ? config.get("toolName").asText() : null;
            JsonNode paramsNode = config.has("params") ? config.get("params") : null;
            String outputVar = config.has("outputVar") ? config.get("outputVar").asText() : null;

            if (toolName == null || toolName.isEmpty()) {
                return NodeResult.failure("Tool node config missing toolName");
            }

            // 替换参数中的占位符
            ObjectNode resolvedParams = replaceParamsPlaceholders(paramsNode, context);

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
    private ObjectNode replaceParamsPlaceholders(JsonNode params, ExecutionContext context) {
        ObjectNode resolved = objectMapper.createObjectNode();
        if (params == null || !params.isObject()) {
            return resolved;
        }

        Iterator<String> fieldNames = params.fieldNames();
        while (fieldNames.hasNext()) {
            String key = fieldNames.next();
            JsonNode value = params.get(key);
            if (value.isTextual()) {
                resolved.put(key, replacePlaceholders(value.asText(), context));
            } else {
                resolved.set(key, value);
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
    private Object executeTool(String toolName, ObjectNode params) {
        // TODO: 等待 MCP 模块实现后替换为真实调用
        throw new UnsupportedOperationException("MCP Tool Executor not yet implemented");
    }
}
