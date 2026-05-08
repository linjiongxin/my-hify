package com.hify.workflow.engine.executor;

import com.hify.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeExecutorRegistryTest {

    @Test
    void shouldReturnExecutor_whenTypeRegistered() {
        NodeExecutor mockLlm = new NodeExecutor() {
            @Override public String nodeType() { return "LLM"; }
            @Override public com.hify.workflow.engine.NodeResult execute(
                    com.hify.workflow.entity.WorkflowNode node,
                    com.hify.workflow.engine.config.NodeConfig config,
                    com.hify.workflow.engine.context.ExecutionContext context) {
                return null;
            }
        };

        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(mockLlm));

        assertThat(registry.get("LLM")).isSameAs(mockLlm);
    }

    @Test
    void shouldThrowBizException_whenTypeUnknown() {
        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of());

        assertThatThrownBy(() -> registry.get("UNKNOWN"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Unknown node type");
    }

    @Test
    void shouldOverride_whenDuplicateTypeRegistered() {
        NodeExecutor first = new NodeExecutor() {
            @Override public String nodeType() { return "LLM"; }
            @Override public com.hify.workflow.engine.NodeResult execute(
                    com.hify.workflow.entity.WorkflowNode node,
                    com.hify.workflow.engine.config.NodeConfig config,
                    com.hify.workflow.engine.context.ExecutionContext context) {
                return null;
            }
        };
        NodeExecutor second = new NodeExecutor() {
            @Override public String nodeType() { return "LLM"; }
            @Override public com.hify.workflow.engine.NodeResult execute(
                    com.hify.workflow.entity.WorkflowNode node,
                    com.hify.workflow.engine.config.NodeConfig config,
                    com.hify.workflow.engine.context.ExecutionContext context) {
                return null;
            }
        };

        NodeExecutorRegistry registry = new NodeExecutorRegistry(List.of(first, second));

        // 后注册者覆盖前者（Spring 默认行为，用最后加载的 bean）
        assertThat(registry.get("LLM")).isSameAs(second);
    }
}
