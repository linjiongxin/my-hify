package com.hify.agent.tool;

import com.hify.agent.api.dto.AgentToolDTO;
import com.hify.model.api.dto.LlmToolDefinition;
import com.hify.workflow.api.WorkflowApi;
import com.hify.workflow.api.dto.WorkflowDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 工作流 Tool Definition 构建器
 * <p>将工作流转换为 LLM function calling 的 Tool Definition</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowToolDefinitionBuilder {

    private final WorkflowApi workflowApi;

    /**
     * 根据 Agent 绑定的工具配置生成 Tool Definition
     *
     * @param agentTool Agent 工具绑定配置
     * @return LLM Tool Definition
     */
    public LlmToolDefinition build(AgentToolDTO agentTool) {
        if (agentTool == null || !"workflow".equals(agentTool.getToolType())) {
            return null;
        }

        Long workflowId = parseWorkflowId(agentTool.getToolImpl());
        if (workflowId == null) {
            log.warn("Invalid workflowId in agentTool: toolImpl={}", agentTool.getToolImpl());
            return null;
        }

        WorkflowDTO workflow = workflowApi.getById(workflowId);
        if (workflow == null) {
            log.warn("Workflow not found: workflowId={}", workflowId);
            return null;
        }

        String toolName = agentTool.getToolName();
        if (toolName == null || toolName.isEmpty()) {
            toolName = workflow.getName();
        }
        // 确保名称符合 OpenAI function name 规范：^[a-zA-Z0-9_-]+$ 且不超过 64 字符
        toolName = sanitizeToolName(toolName);

        String description = workflow.getDescription();
        if (description == null || description.isEmpty()) {
            description = "执行 " + workflow.getName() + " 工作流";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = agentTool.getConfigJson() != null
                ? (Map<String, Object>) agentTool.getConfigJson().get("parameters")
                : null;
        if (parameters == null) {
            parameters = defaultParameters();
        }

        return LlmToolDefinition.builder()
                .type("function")
                .function(LlmToolDefinition.Function.builder()
                        .name(toolName)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }

    private Long parseWorkflowId(String toolImpl) {
        if (toolImpl == null || toolImpl.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(toolImpl);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sanitizeToolName(String name) {
        if (name == null) {
            return "workflow_tool";
        }
        // 替换非法字符为下划线
        String sanitized = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        // 不能以数字开头
        if (sanitized.matches("^\\d.*")) {
            sanitized = "wf_" + sanitized;
        }
        // 截断至 64 字符
        if (sanitized.length() > 64) {
            sanitized = sanitized.substring(0, 64);
        }
        // 兜底
        if (sanitized.isEmpty()) {
            sanitized = "workflow_tool";
        }
        return sanitized;
    }

    private Map<String, Object> defaultParameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "input", Map.of(
                                "type", "string",
                                "description", "用户输入内容或需要传递给工作流的参数"
                        )
                )
        );
    }
}
