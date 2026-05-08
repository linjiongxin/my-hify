package com.hify.workflow.engine.context;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionContextTest {

    @Test
    void shouldPreWriteInputsWithStartNamespace_whenConstructedWithInputs() {
        Map<String, Object> inputs = Map.of("userMessage", "hello", "sessionId", 42L);
        ExecutionContext ctx = new ExecutionContext(100L, inputs);

        assertThat(ctx.getWorkflowInstanceId()).isEqualTo(100L);
        assertThat(ctx.get("start", "userMessage")).isEqualTo("hello");
        assertThat(ctx.get("start", "sessionId")).isEqualTo(42L);
    }

    @Test
    void shouldPreWriteUserMessage_whenConstructedWithUserMessage() {
        ExecutionContext ctx = new ExecutionContext(100L, Map.of("userMessage", "我要退款"));

        assertThat(ctx.get("start", "userMessage")).isEqualTo("我要退款");
    }

    @Test
    void shouldSupportSetAndGet_withNodeKeyAndVarName() {
        ExecutionContext ctx = new ExecutionContext(100L, Collections.emptyMap());

        ctx.set("node_llm", "llmResponse", "退款处理中");

        assertThat(ctx.get("node_llm", "llmResponse")).isEqualTo("退款处理中");
        assertThat(ctx.get("node_llm.llmResponse")).isEqualTo("退款处理中");
    }

    @Test
    void shouldResolveTemplate_withDoubleBraceSyntax() {
        ExecutionContext ctx = new ExecutionContext(100L, Map.of("userMessage", "hello"));
        ctx.set("node_llm", "llmResponse", "你好");

        String result = ctx.resolve("{{start.userMessage}} -> {{node_llm.llmResponse}}");

        assertThat(result).isEqualTo("hello -> 你好");
    }

    @Test
    void shouldKeepPlaceholder_whenVariableNotFound() {
        ExecutionContext ctx = new ExecutionContext(100L, Collections.emptyMap());

        String result = ctx.resolve("{{start.userMessage}}");

        assertThat(result).isEqualTo("{{start.userMessage}}");
    }

    @Test
    void shouldReturnReadOnlySnapshot() {
        ExecutionContext ctx = new ExecutionContext(100L, Map.of("userMessage", "hi"));
        ctx.set("node_a", "score", 90);

        Map<String, Object> snapshot = ctx.snapshot();

        assertThat(snapshot).containsEntry("start.userMessage", "hi")
                .containsEntry("node_a.score", 90);
        assertThatThrownBy(() -> snapshot.put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldMaintainInsertionOrder() {
        ExecutionContext ctx = new ExecutionContext(100L, Collections.emptyMap());
        ctx.set("node_a", "x", 1);
        ctx.set("node_b", "y", 2);
        ctx.set("node_c", "z", 3);

        var keys = ctx.snapshot().keySet().iterator();
        assertThat(keys.next()).isEqualTo("node_a.x");
        assertThat(keys.next()).isEqualTo("node_b.y");
        assertThat(keys.next()).isEqualTo("node_c.z");
    }

    @Test
    void shouldKeepBackwardCompatibility_forPutAndGet() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put("_visitedNodes", java.util.List.of("node_a"));

        assertThat(ctx.get("_visitedNodes")).isEqualTo(java.util.List.of("node_a"));
    }

    @Test
    void shouldKeepTypeSafeGetters() {
        ExecutionContext ctx = new ExecutionContext(100L, Collections.emptyMap());
        ctx.set("node_a", "count", 10);
        ctx.set("node_b", "flag", true);
        ctx.set("node_c", "price", 19.99);

        assertThat(ctx.getInt("node_a.count")).isEqualTo(10);
        assertThat(ctx.getBoolean("node_b.flag")).isTrue();
        assertThat(ctx.getDouble("node_c.price")).isEqualTo(19.99);
    }
}
