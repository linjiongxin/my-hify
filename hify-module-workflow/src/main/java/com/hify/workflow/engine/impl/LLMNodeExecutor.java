package com.hify.workflow.engine.impl;

import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.engine.NodeExecutor;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.LlmNodeConfig;
import com.hify.workflow.engine.config.NodeConfigParser;
import com.hify.workflow.engine.util.PlaceholderUtils;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 节点执行器
 * <p>调用模型网关执行 LLM 对话</p>
 */
@Component
public class LLMNodeExecutor implements NodeExecutor {

    private final NodeConfigParser nodeConfigParser;
    private final LlmGatewayApi llmGatewayApi;

    public LLMNodeExecutor(NodeConfigParser nodeConfigParser, LlmGatewayApi llmGatewayApi) {
        this.nodeConfigParser = nodeConfigParser;
        this.llmGatewayApi = llmGatewayApi;
    }

    @Override
    public NodeResult execute(WorkflowNode node, ExecutionContext context) {
        try {
            LlmNodeConfig config = (LlmNodeConfig) nodeConfigParser.parse(node);

            if (config.model() == null || config.prompt() == null) {
                return NodeResult.failure("LLM node config missing model or prompt");
            }

            // 替换 prompt 中的占位符
            String resolvedPrompt = PlaceholderUtils.replace(config.prompt(), context);

            // 构建请求
            LlmMessage userMessage = LlmMessage.user(resolvedPrompt);
            LlmChatRequest request = LlmChatRequest.builder()
                    .messages(List.of(userMessage))
                    .build();

            // 调用模型网关
            LlmChatResponse response = llmGatewayApi.chat(config.model(), request);

            // 获取结果内容
            String content = response.getContent();
            if (content == null) {
                content = "";
            }

            // 存入上下文（按节点命名空间写入，避免多节点覆盖）
            if (config.outputVar() != null && !config.outputVar().isEmpty()) {
                context.set(node.getNodeId(), config.outputVar(), content);
                // 同时保留扁平 key，便于下游兼容读取
                context.put(config.outputVar(), content);
            }

            return NodeResult.success(null);

        } catch (Exception e) {
            return NodeResult.failure("LLM execution failed: " + e.getMessage());
        }
    }
}
