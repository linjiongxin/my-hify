package com.hify.workflow.engine.executor;

import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.LlmNodeConfig;
import com.hify.workflow.engine.config.NodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 节点执行器（v2）
 */
@Component
public class LlmNodeExecutor implements NodeExecutor {

    private final LlmGatewayApi llmGatewayApi;

    public LlmNodeExecutor(LlmGatewayApi llmGatewayApi) {
        this.llmGatewayApi = llmGatewayApi;
    }

    @Override
    public String nodeType() {
        return "LLM";
    }

    @Override
    public NodeResult execute(WorkflowNode node, NodeConfig config, ExecutionContext context) {
        try {
            LlmNodeConfig llmConfig = (LlmNodeConfig) config;

            if (llmConfig.model() == null || llmConfig.prompt() == null) {
                return NodeResult.failure("LLM node config missing model or prompt");
            }

            String resolvedPrompt = context.resolve(llmConfig.prompt());
            if (resolvedPrompt == null) {
                resolvedPrompt = "";
            }

            LlmMessage userMessage = LlmMessage.user(resolvedPrompt);
            LlmChatRequest request = LlmChatRequest.builder()
                    .messages(List.of(userMessage))
                    .build();

            LlmChatResponse response = llmGatewayApi.chat(llmConfig.model(), request);
            String content = response.getContent();
            if (content == null) {
                content = "";
            }

            if (llmConfig.outputVar() != null && !llmConfig.outputVar().isEmpty()) {
                context.set(node.getNodeId(), llmConfig.outputVar(), content);
                context.put(llmConfig.outputVar(), content);
            }

            return NodeResult.success(null);

        } catch (Exception e) {
            return NodeResult.failure("LLM execution failed: " + e.getMessage());
        }
    }
}
