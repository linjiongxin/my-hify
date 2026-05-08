package com.hify.workflow.engine.executor;

import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.workflow.engine.NodeResult;
import com.hify.workflow.engine.config.LlmNodeConfig;
import com.hify.workflow.engine.context.ExecutionContext;
import com.hify.workflow.entity.WorkflowNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmNodeExecutorTest {

    @Mock
    private LlmGatewayApi llmGatewayApi;

    private LlmNodeExecutor executor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        executor = new LlmNodeExecutor(llmGatewayApi);
    }

    @Test
    void shouldResolvePromptAndCallLlm_whenExecute() {
        WorkflowNode node = new WorkflowNode();
        node.setNodeId("node_llm");

        ExecutionContext ctx = new ExecutionContext(1L, java.util.Map.of("userMessage", "hello"));
        LlmNodeConfig config = new LlmNodeConfig("gpt-4o", "用户说: {{start.userMessage}}", "llmResponse", null);

        LlmChatResponse response = new LlmChatResponse();
        response.setContent("你好");
        when(llmGatewayApi.chat(eq("gpt-4o"), any())).thenReturn(response);

        NodeResult result = executor.execute(node, config, ctx);

        assertThat(result.isSuccess()).isTrue();
        assertThat(ctx.get("node_llm", "llmResponse")).isEqualTo("你好");
        assertThat(ctx.get("llmResponse")).isEqualTo("你好"); // 扁平兼容
        verify(llmGatewayApi).chat(eq("gpt-4o"), any());
    }

    @Test
    void shouldReturnFailure_whenModelConfigMissing() {
        WorkflowNode node = new WorkflowNode();
        node.setNodeId("node_llm");

        ExecutionContext ctx = new ExecutionContext();
        LlmNodeConfig config = new LlmNodeConfig(null, "prompt", "out", null);

        NodeResult result = executor.execute(node, config, ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("missing model");
    }

    @Test
    void shouldReturnFailure_whenLlmThrows() {
        WorkflowNode node = new WorkflowNode();
        node.setNodeId("node_llm");

        ExecutionContext ctx = new ExecutionContext();
        LlmNodeConfig config = new LlmNodeConfig("gpt-4o", "hi", "out", null);

        when(llmGatewayApi.chat(any(), any())).thenThrow(new RuntimeException("timeout"));

        NodeResult result = executor.execute(node, config, ctx);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("timeout");
    }
}
