package com.hify.model.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.dto.*;
import com.hify.model.constant.ModelConstants;
import com.hify.model.entity.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Anthropic Claude 原生协议 Provider
 * <p>支持 Messages API、thinking 模式、tool use</p>
 *
 * @author hify
 */
@Slf4j
@Component
public class AnthropicProvider implements LlmProvider {

    private final ObjectMapper objectMapper;
    private final OkHttpClient baseClient;

    public AnthropicProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(200, 5, TimeUnit.MINUTES))
                .build();
    }

    private OkHttpClient getClient(boolean isStream) {
        if (isStream) {
            return baseClient.newBuilder().readTimeout(120, TimeUnit.SECONDS).build();
        }
        return baseClient;
    }

    @Override
    public String getCode() {
        return ModelConstants.ProtocolType.ANTHROPIC;
    }

    @Override
    public boolean supports(String modelId) {
        return modelId != null && (modelId.startsWith("claude-") || modelId.startsWith("anthropic."));
    }

    @Override
    public LlmChatResponse chat(String modelId, ModelProvider provider, LlmChatRequest request) {
        String apiUrl = resolveApiUrl(provider, request) + "/v1/messages";
        String jsonBody = buildRequestBody(modelId, request, false);

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .header(ModelConstants.HeaderName.X_API_KEY, resolveApiKey(provider, request))
                .header(ModelConstants.HeaderName.ANTHROPIC_VERSION, ModelConstants.AnthropicVersion.V2023_06_01)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        try (Response response = getClient(false).newCall(requestBuilder.build()).execute()) {
            String bodyString = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new BizException(ResultCode.LLM_API_ERROR,
                        "Claude API 调用失败: " + response.code() + ", body=" + bodyString);
            }
            return parseChatResponse(bodyString);
        } catch (IOException e) {
            throw new BizException(ResultCode.LLM_API_ERROR, "Claude API 请求异常", e);
        }
    }

    @Override
    public void chatStream(String modelId, ModelProvider provider, LlmChatRequest request, Consumer<LlmStreamChunk> callback) {
        String apiUrl = resolveApiUrl(provider, request) + "/v1/messages";
        String jsonBody = buildRequestBody(modelId, request, true);

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header(ModelConstants.HeaderName.X_API_KEY, resolveApiKey(provider, request))
                .header(ModelConstants.HeaderName.ANTHROPIC_VERSION, ModelConstants.AnthropicVersion.V2023_06_01)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        EventSource.Factory factory = EventSources.createFactory(getClient(true));
        factory.newEventSource(requestBuilder.build(), new AnthropicSseEventListener(callback, objectMapper));
    }

    // ==================== 私有方法 ====================

    private String resolveApiUrl(ModelProvider provider, LlmChatRequest request) {
        if (request != null && request.getExtra() != null && request.getExtra().get("apiBaseUrl") != null) {
            return request.getExtra().get("apiBaseUrl");
        }
        if (provider != null && provider.getApiBaseUrl() != null) {
            return provider.getApiBaseUrl();
        }
        return "https://api.anthropic.com";
    }

    private String resolveApiKey(ModelProvider provider, LlmChatRequest request) {
        if (request != null && request.getExtra() != null && request.getExtra().get("apiKey") != null) {
            return request.getExtra().get("apiKey");
        }
        if (provider != null && provider.getApiKey() != null) {
            return provider.getApiKey();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String buildRequestBody(String modelId, LlmChatRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelId);
        body.put("stream", stream);

        // max_tokens 对 Claude 是必填项
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4096);

        // system 提示词是顶层字段
        String systemPrompt = extractSystemPrompt(request.getMessages());
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("system", systemPrompt);
        }

        // 过滤掉 system 消息
        List<LlmMessage> nonSystemMessages = request.getMessages().stream()
                .filter(m -> !"system".equals(m.getRole()))
                .toList();
        body.put("messages", toAnthropicMessages(nonSystemMessages));

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }

        // thinking 模式
        if (request.getReasoningEffort() != null) {
            Map<String, Object> thinking = new HashMap<>();
            thinking.put("type", "enabled");
            int budgetTokens = switch (request.getReasoningEffort().toLowerCase()) {
                case "low" -> 4096;
                case "medium" -> 8192;
                case "high" -> 16384;
                default -> 4096;
            };
            thinking.put("budget_tokens", budgetTokens);
            body.put("thinking", thinking);
        }

        // tools
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", toAnthropicTools(request.getTools()));
        }
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }

        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BizException(ResultCode.LLM_API_ERROR, "构造 Claude 请求体失败", e);
        }
    }

    private String extractSystemPrompt(List<LlmMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (LlmMessage m : messages) {
            if ("system".equals(m.getRole()) && m.getContent() != null) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(m.getContent());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toAnthropicMessages(List<LlmMessage> messages) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LlmMessage m : messages) {
            Map<String, Object> map = new HashMap<>();
            String role = mapRole(m.getRole());
            map.put("role", role);

            // content 支持文本和多模态
            if (m.getContentParts() != null && !m.getContentParts().isEmpty()) {
                List<Map<String, Object>> contentBlocks = new ArrayList<>();
                for (LlmContentPart part : m.getContentParts()) {
                    if ("text".equals(part.getType())) {
                        Map<String, Object> block = new HashMap<>();
                        block.put("type", "text");
                        block.put("text", part.getText());
                        contentBlocks.add(block);
                    } else if ("image_url".equals(part.getType()) && part.getImageUrl() != null) {
                        Map<String, Object> block = new HashMap<>();
                        block.put("type", "image");
                        String url = part.getImageUrl().getUrl();
                        if (url.startsWith("data:")) {
                            // base64 格式: data:image/png;base64,xxx
                            String[] parts = url.split("[,;]", 3);
                            String mediaType = parts[0].replace("data:", "");
                            String data = parts.length > 2 ? parts[2] : "";
                            Map<String, Object> source = new HashMap<>();
                            source.put("type", "base64");
                            source.put("media_type", mediaType);
                            source.put("data", data);
                            block.put("source", source);
                        } else {
                            Map<String, Object> source = new HashMap<>();
                            source.put("type", "url");
                            source.put("url", url);
                            block.put("source", source);
                        }
                        contentBlocks.add(block);
                    }
                }
                map.put("content", contentBlocks);
            } else if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                // assistant 的 tool_use 请求
                List<Map<String, Object>> contentBlocks = new ArrayList<>();
                for (LlmToolCall tc : m.getToolCalls()) {
                    Map<String, Object> block = new HashMap<>();
                    block.put("type", "tool_use");
                    block.put("id", tc.getId());
                    block.put("name", tc.getName());
                    try {
                        block.put("input", objectMapper.readValue(tc.getArguments(), Map.class));
                    } catch (Exception e) {
                        block.put("input", Map.of());
                    }
                    contentBlocks.add(block);
                }
                map.put("content", contentBlocks);
            } else if ("tool".equals(m.getRole()) && m.getToolCallId() != null) {
                // tool 回传结果
                List<Map<String, Object>> contentBlocks = new ArrayList<>();
                Map<String, Object> block = new HashMap<>();
                block.put("type", "tool_result");
                block.put("tool_use_id", m.getToolCallId());
                block.put("content", m.getContent());
                contentBlocks.add(block);
                map.put("content", contentBlocks);
            } else {
                map.put("content", m.getContent());
            }

            result.add(map);
        }
        return result;
    }

    private String mapRole(String role) {
        return switch (role) {
            case "user" -> "user";
            case "assistant", "tool" -> "assistant";
            default -> "user";
        };
    }

    private List<Map<String, Object>> toAnthropicTools(List<LlmToolDefinition> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (LlmToolDefinition tool : tools) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", tool.getFunction().getName());
            map.put("description", tool.getFunction().getDescription());
            map.put("input_schema", tool.getFunction().getParameters());
            result.add(map);
        }
        return result;
    }

    private LlmChatResponse parseChatResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);

        String content = "";
        String reasoningContent = null;
        List<LlmToolCall> toolCalls = null;

        JsonNode contentArray = root.path("content");
        if (contentArray.isArray()) {
            for (JsonNode block : contentArray) {
                String type = block.path("type").asText("");
                if ("text".equals(type)) {
                    content += block.path("text").asText("");
                } else if ("thinking".equals(type)) {
                    if (reasoningContent == null) reasoningContent = "";
                    reasoningContent += block.path("thinking").asText("");
                } else if ("tool_use".equals(type)) {
                    if (toolCalls == null) toolCalls = new ArrayList<>();
                    LlmToolCall tc = LlmToolCall.builder()
                            .id(block.path("id").asText(null))
                            .type("function")
                            .name(block.path("name").asText(null))
                            .arguments(block.path("input").toString())
                            .build();
                    toolCalls.add(tc);
                }
            }
        }

        String finishReason = root.path("stop_reason").asText(null);
        if ("end_turn".equals(finishReason)) finishReason = "stop";
        if ("max_tokens".equals(finishReason)) finishReason = "length";

        LlmUsage usage = OpenAiCompatibleProvider.parseUsage(root.path("usage"));

        return LlmChatResponse.builder()
                .content(content)
                .reasoningContent(reasoningContent)
                .toolCalls(toolCalls)
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }

    // ==================== SSE 事件监听器 ====================

    @Slf4j
    static class AnthropicSseEventListener extends okhttp3.sse.EventSourceListener {

        private final Consumer<LlmStreamChunk> callback;
        private final ObjectMapper objectMapper;

        AnthropicSseEventListener(Consumer<LlmStreamChunk> callback, ObjectMapper objectMapper) {
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
                log.warn("解析 Claude SSE 数据失败: {}", data, e);
            }
        }

        @Override
        public void onFailure(EventSource eventSource, Throwable t, Response response) {
            log.error("Claude SSE 连接异常", t);
            callback.accept(LlmStreamChunk.builder().finish(true).build());
        }

        private LlmStreamChunk parseStreamChunk(String json) throws IOException {
            JsonNode root = objectMapper.readTree(json);
            String eventType = root.path("type").asText("");

            String content = "";
            String reasoningContent = null;
            List<LlmToolCall> toolCalls = null;
            String finishReason = null;
            LlmUsage usage = null;

            if ("content_block_delta".equals(eventType)) {
                JsonNode delta = root.path("delta");
                String deltaType = delta.path("type").asText("");
                if ("text_delta".equals(deltaType)) {
                    content = delta.path("text").asText("");
                } else if ("thinking_delta".equals(deltaType)) {
                    reasoningContent = delta.path("thinking").asText("");
                }
            } else if ("content_block_start".equals(eventType)) {
                JsonNode contentBlock = root.path("content_block");
                String blockType = contentBlock.path("type").asText("");
                if ("text".equals(blockType)) {
                    content = contentBlock.path("text").asText("");
                } else if ("tool_use".equals(blockType)) {
                    LlmToolCall tc = LlmToolCall.builder()
                            .id(contentBlock.path("id").asText(null))
                            .type("function")
                            .name(contentBlock.path("name").asText(null))
                            .arguments(contentBlock.path("input").toString())
                            .build();
                    toolCalls = List.of(tc);
                }
            } else if ("message_delta".equals(eventType)) {
                String stopReason = root.path("delta").path("stop_reason").asText(null);
                if (stopReason != null) {
                    finishReason = "end_turn".equals(stopReason) ? "stop" : stopReason;
                }
                usage = OpenAiCompatibleProvider.parseUsage(root.path("usage"));
            } else if ("message_stop".equals(eventType)) {
                finishReason = "stop";
            }

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
}
