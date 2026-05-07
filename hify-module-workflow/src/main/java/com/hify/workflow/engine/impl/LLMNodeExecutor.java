package com.hify.workflow.engine.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 节点执行器
 * <p>调用模型网关执行 LLM 对话</p>
 */
@Component
public class LLMNodeExecutor implements NodeExecutor {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final LlmGatewayApi llmGatewayApi;

    public LLMNodeExecutor(LlmGatewayApi llmGatewayApi) {
        this.llmGatewayApi = llmGatewayApi;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            // 解析配置
            JsonNode config = objectMapper.readTree(node.getConfig());
            String model = config.get("model").asText();
            String prompt = config.get("prompt").asText();
            String outputVar = config.has("outputVar") ? config.get("outputVar").asText() : null;

            if (model == null || prompt == null) {
                return NodeResult.failure("LLM node config missing model or prompt");
            }

            // 替换 prompt 中的占位符
            String resolvedPrompt = replacePlaceholders(prompt, context);

            // 构建请求
            LlmMessage userMessage = LlmMessage.user(resolvedPrompt);
            LlmChatRequest request = LlmChatRequest.builder()
                    .messages(List.of(userMessage))
                    .build();

            // 调用模型网关
            LlmChatResponse response = llmGatewayApi.chat(model, request);

            // 获取结果内容
            String content = response.getContent();
            if (content == null) {
                content = "";
            }

            // 存入上下文
            if (outputVar != null && !outputVar.isEmpty()) {
                context.put(outputVar, content);
            }

            // 找下一节点
            String nextNodeId = findNextNodeId(node, context);
            return NodeResult.success(nextNodeId);

        } catch (Exception e) {
            return NodeResult.failure("LLM execution failed: " + e.getMessage());
        }
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
     * 查找下一节点 ID
     * <p>默认找第一条出线，如果没有连线则结束</p>
     */
    private String findNextNodeId(WorkflowNode node, ExecutionContext context) {
        // 这里暂时返回 null，让编排器处理连线查找
        // 后续编排器会根据节点 ID 查找对应的连线
        return null;
    }
}
