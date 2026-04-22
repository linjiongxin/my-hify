package com.hify.model.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.api.dto.LlmToolCall;
import com.hify.model.api.dto.LlmUsage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议 SSE 事件监听器
 *
 * <p>支持增量文本、reasoning_content、tool_calls 增量解析</p>
 *
 * @author hify
 */
@Slf4j
public class OpenAiSseEventListener extends EventSourceListener {

    private final Consumer<LlmStreamChunk> callback;
    private final ObjectMapper objectMapper;

    public OpenAiSseEventListener(Consumer<LlmStreamChunk> callback, ObjectMapper objectMapper) {
        this.callback = callback;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        if ("[DONE]".equals(data)) {
            callback.accept(LlmStreamChunk.builder().finish(true).build());
            return;
        }
        try {
            LlmStreamChunk chunk = parseStreamChunk(data);
            callback.accept(chunk);
            if (Boolean.TRUE.equals(chunk.getFinish())) {
                eventSource.cancel();
            }
        } catch (Exception e) {
            log.warn("解析 SSE 流数据失败: {}", data, e);
        }
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        log.error("SSE 流连接异常", t);
        callback.accept(LlmStreamChunk.builder().finish(true).build());
    }

    private LlmStreamChunk parseStreamChunk(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            return LlmStreamChunk.builder().content("").build();
        }

        JsonNode firstChoice = choices.get(0);
        JsonNode delta = firstChoice.path("delta");
        String content = delta.path("content").asText("");
        String reasoningContent = delta.path("reasoning_content").asText(null);
        String finishReason = firstChoice.path("finish_reason").asText(null);

        // tool_calls 增量解析
        List<LlmToolCall> toolCalls = OpenAiCompatibleProvider.parseToolCalls(delta.path("tool_calls"));

        // usage 可能在最后一条返回
        LlmUsage usage = OpenAiCompatibleProvider.parseUsage(root.path("usage"));

        return LlmStreamChunk.builder()
                .content(content)
                .reasoningContent(reasoningContent)
                .toolCalls(toolCalls)
                .usage(usage)
                .finish(finishReason != null)
                .finishReason(finishReason)
                .build();
    }
}
