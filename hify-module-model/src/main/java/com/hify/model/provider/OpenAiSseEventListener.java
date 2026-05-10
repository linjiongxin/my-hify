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

    // 处理模型在 content 中输出 <think> 标签的情况
    private boolean inThinkTag = false;

    // 累积 tool_calls（OpenAI 增量 SSE 格式会分多个 chunk 下发）
    private final java.util.List<LlmToolCall> accumulatedToolCalls = new java.util.ArrayList<>();

    // 防止 onEvent 和 onFailure 同时触发 callback 导致重复处理
    private final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);

    public OpenAiSseEventListener(Consumer<LlmStreamChunk> callback, ObjectMapper objectMapper) {
        this.callback = callback;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        if (finished.get()) {
            return;
        }
        if ("[DONE]".equals(data)) {
            if (finished.compareAndSet(false, true)) {
                callback.accept(LlmStreamChunk.builder().finish(true).build());
            }
            return;
        }
        try {
            LlmStreamChunk chunk = parseStreamChunk(data);
            // 过滤 <think> 标签内容，避免思考过程暴露给用户
            String filtered = filterThinkContent(chunk.getContent());
            chunk.setContent(filtered);
            // 如果累积了 tool_calls，附加到 finish chunk 中
            if (Boolean.TRUE.equals(chunk.getFinish()) && !accumulatedToolCalls.isEmpty()) {
                chunk.setToolCalls(new java.util.ArrayList<>(accumulatedToolCalls));
            }
            if (Boolean.TRUE.equals(chunk.getFinish()) && finished.compareAndSet(false, true)) {
                callback.accept(chunk);
                eventSource.cancel();
            } else if (!Boolean.TRUE.equals(chunk.getFinish())) {
                callback.accept(chunk);
            }
        } catch (Exception e) {
            log.warn("解析 SSE 流数据失败: {}", data, e);
        }
    }

    /**
     * 过滤 <think>...</think> 包裹的思考过程内容
     * 支持跨 chunk 的 <think> 标签
     */
    private String filterThinkContent(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (!inThinkTag) {
                int thinkStart = text.indexOf("<think>", i);
                if (thinkStart == -1) {
                    result.append(text.substring(i));
                    break;
                }
                result.append(text.substring(i, thinkStart));
                i = thinkStart + 7; // len("<think>") = 7
                inThinkTag = true;
            } else {
                int thinkEnd = text.indexOf("</think>", i);
                if (thinkEnd == -1) {
                    // <think> 未闭合，忽略剩余内容直到下一条
                    break;
                }
                i = thinkEnd + 8; // len("</think>") = 8
                inThinkTag = false;
            }
        }
        return result.toString();
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        String errorMsg = "SSE 流连接异常";
        if (response != null) {
            errorMsg += " (HTTP " + response.code() + ")";
            try {
                if (response.body() != null) {
                    String body = response.body().string();
                    errorMsg += ": " + body;
                }
            } catch (IOException e) {
                // ignore
            }
        }
        if (t != null) {
            errorMsg += " | " + t.getMessage();
            log.error("SSE 流连接异常", t);
        } else {
            log.error("SSE 流连接异常: {}", errorMsg);
        }
        callback.accept(LlmStreamChunk.builder()
                .error(errorMsg)
                .finish(true)
                .finishReason("error")
                .build());
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

        // tool_calls 增量累积解析
        JsonNode deltaToolCalls = delta.path("tool_calls");
        if (deltaToolCalls.isArray()) {
            for (JsonNode tcDelta : deltaToolCalls) {
                int index = tcDelta.path("index").asInt(0);
                // 确保列表足够长
                while (accumulatedToolCalls.size() <= index) {
                    accumulatedToolCalls.add(LlmToolCall.builder().type("function").build());
                }
                LlmToolCall call = accumulatedToolCalls.get(index);
                if (tcDelta.has("id")) {
                    call.setId(tcDelta.path("id").asText(null));
                }
                if (tcDelta.has("type")) {
                    call.setType(tcDelta.path("type").asText("function"));
                }
                JsonNode func = tcDelta.path("function");
                if (func.has("name")) {
                    call.setName(func.path("name").asText(null));
                }
                if (func.has("arguments")) {
                    String argsDelta = func.path("arguments").asText("");
                    call.setArguments((call.getArguments() != null ? call.getArguments() : "") + argsDelta);
                }
            }
        }

        // usage 可能在最后一条返回
        LlmUsage usage = OpenAiCompatibleProvider.parseUsage(root.path("usage"));

        return LlmStreamChunk.builder()
                .content(content)
                .reasoningContent(reasoningContent)
                .usage(usage)
                .finish(finishReason != null)
                .finishReason(finishReason)
                .build();
    }
}
