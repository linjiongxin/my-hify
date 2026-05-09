package com.hify.model.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
 * OpenAI 兼容协议 Provider
 * <p>覆盖 OpenAI、DeepSeek、通义千问、Kimi、GLM、Gemini、Ollama 等</p>
 *
 * @author hify
 */
@Slf4j
@Component
public class OpenAiCompatibleProvider implements LlmProvider {

    private final ObjectMapper objectMapper;
    private final OkHttpClient baseClient;

    public OpenAiCompatibleProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(200, 5, TimeUnit.MINUTES))
                .build();
    }

    private OkHttpClient getClient(boolean isStream) {
        if (isStream) {
            return baseClient.newBuilder()
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build();
        }
        return baseClient;
    }

    @Override
    public String getCode() {
        return ModelConstants.ProtocolType.OPENAI_COMPATIBLE;
    }

    @Override
    public boolean supports(String modelId) {
        return true;
    }

    @Override
    public LlmChatResponse chat(String modelId, ModelProvider provider, LlmChatRequest request) {
        String apiUrl = resolveApiUrl(provider, request) + "/chat/completions";
        String jsonBody = buildRequestBody(modelId, request, false);

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        applyAuthHeaders(requestBuilder, provider, request);

        try (Response response = getClient(false).newCall(requestBuilder.build()).execute()) {
            String bodyString = response.body() != null ? response.body().string() : "{}";
            if (!response.isSuccessful()) {
                throw new BizException(ResultCode.LLM_API_ERROR,
                        "LLM API 调用失败: " + response.code() + ", body=" + bodyString);
            }
            return parseChatResponse(bodyString);
        } catch (IOException e) {
            throw new BizException(ResultCode.LLM_API_ERROR, "LLM API 请求异常", e);
        }
    }

    @Override
    public void chatStream(String modelId, ModelProvider provider, LlmChatRequest request, Consumer<LlmStreamChunk> callback) {
        String apiUrl = resolveApiUrl(provider, request) + "/chat/completions";
        String jsonBody = buildRequestBody(modelId, request, true);

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Connection", "close")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        applyAuthHeaders(requestBuilder, provider, request);

        EventSource.Factory factory = EventSources.createFactory(getClient(true));
        factory.newEventSource(requestBuilder.build(), new OpenAiSseEventListener(callback, objectMapper));
    }

    // ==================== 私有方法 ====================

    private String resolveApiUrl(ModelProvider provider, LlmChatRequest request) {
        if (request != null && request.getExtra() != null && request.getExtra().get("apiBaseUrl") != null) {
            return request.getExtra().get("apiBaseUrl");
        }
        if (provider != null && provider.getApiBaseUrl() != null) {
            return provider.getApiBaseUrl();
        }
        return "https://api.openai.com/v1";
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

    private void applyAuthHeaders(Request.Builder requestBuilder, ModelProvider provider, LlmChatRequest request) {
        String authType = provider != null && provider.getAuthType() != null ? provider.getAuthType() : ModelConstants.AuthType.BEARER;
        String apiKey = resolveApiKey(provider, request);

        switch (authType.toUpperCase()) {
            case ModelConstants.AuthType.BEARER -> requestBuilder.header(ModelConstants.HeaderName.AUTHORIZATION, "Bearer " + apiKey);
            case ModelConstants.AuthType.API_KEY -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = provider.getAuthConfig() != null ? provider.getAuthConfig() : Map.of();
                String headerName = config.getOrDefault(ModelConstants.AuthConfigKey.HEADER_NAME, ModelConstants.HeaderName.API_KEY).toString();
                String prefix = config.getOrDefault(ModelConstants.AuthConfigKey.PREFIX, "").toString();
                requestBuilder.header(headerName, prefix + apiKey);
            }
            case ModelConstants.AuthType.NONE -> {
            }
            case ModelConstants.AuthType.CUSTOM -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = provider.getAuthConfig() != null ? provider.getAuthConfig() : Map.of();
                Object headersObj = config.get(ModelConstants.AuthConfigKey.HEADERS);
                if (headersObj instanceof Map<?, ?> headers) {
                    headers.forEach((k, v) -> {
                        if (k != null && v != null) {
                            requestBuilder.header(k.toString(), v.toString());
                        }
                    });
                }
            }
            default -> requestBuilder.header(ModelConstants.HeaderName.AUTHORIZATION, "Bearer " + apiKey);
        }
    }

    private String buildRequestBody(String modelId, LlmChatRequest request, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelId);
        body.put("messages", toOpenAiMessages(request.getMessages()));
        body.put("stream", stream);

        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            body.put("max_tokens", request.getMaxTokens());
        }
        if (request.getTopP() != null) {
            body.put("top_p", request.getTopP());
        }
        if (request.getReasoningEffort() != null) {
            body.put("reasoning_effort", request.getReasoningEffort());
        }
        if (request.getTools() != null && !request.getTools().isEmpty()) {
            body.put("tools", request.getTools());
        }
        if (request.getToolChoice() != null) {
            body.put("tool_choice", request.getToolChoice());
        }

        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BizException(ResultCode.LLM_API_ERROR, "构造 LLM 请求体失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> toOpenAiMessages(List<LlmMessage> messages) {
        List<Object> result = new ArrayList<>();
        for (LlmMessage m : messages) {
            Map<String, Object> map = new HashMap<>();
            map.put("role", m.getRole());

            // 多模态 content parts
            if (m.getContentParts() != null && !m.getContentParts().isEmpty()) {
                List<Map<String, Object>> parts = new ArrayList<>();
                for (LlmContentPart part : m.getContentParts()) {
                    Map<String, Object> partMap = new HashMap<>();
                    partMap.put("type", part.getType());
                    if ("text".equals(part.getType())) {
                        partMap.put("text", part.getText());
                    } else if ("image_url".equals(part.getType()) && part.getImageUrl() != null) {
                        Map<String, Object> imageUrlMap = new HashMap<>();
                        imageUrlMap.put("url", part.getImageUrl().getUrl());
                        if (part.getImageUrl().getDetail() != null) {
                            imageUrlMap.put("detail", part.getImageUrl().getDetail());
                        }
                        partMap.put("image_url", imageUrlMap);
                    }
                    parts.add(partMap);
                }
                map.put("content", parts);
            } else {
                map.put("content", m.getContent() != null ? m.getContent() : "");
            }

            // tool_calls (assistant)
            if (m.getToolCalls() != null && !m.getToolCalls().isEmpty()) {
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                for (LlmToolCall tc : m.getToolCalls()) {
                    Map<String, Object> tcMap = new HashMap<>();
                    tcMap.put("id", tc.getId());
                    tcMap.put("type", tc.getType());
                    Map<String, Object> function = new HashMap<>();
                    function.put("name", tc.getName());
                    function.put("arguments", tc.getArguments());
                    tcMap.put("function", function);
                    toolCalls.add(tcMap);
                }
                map.put("tool_calls", toolCalls);
            }

            // tool_call_id (tool)
            if (m.getToolCallId() != null) {
                map.put("tool_call_id", m.getToolCallId());
            }
            if (m.getName() != null) {
                map.put("name", m.getName());
            }

            result.add(map);
        }
        return result;
    }

    private LlmChatResponse parseChatResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            return LlmChatResponse.builder().content("").build();
        }

        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.path("message");
        String content = message.path("content").asText("");
        String finishReason = firstChoice.path("finish_reason").asText();

        // reasoning_content (DeepSeek R1)
        String reasoningContent = message.path("reasoning_content").asText(null);
        if (reasoningContent == null) {
            reasoningContent = root.path("reasoning_content").asText(null);
        }

        // tool_calls
        List<LlmToolCall> toolCalls = parseToolCalls(message.path("tool_calls"));

        LlmUsage usage = parseUsage(root.path("usage"));

        return LlmChatResponse.builder()
                .content(content)
                .reasoningContent(reasoningContent)
                .toolCalls(toolCalls)
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }

    static List<LlmToolCall> parseToolCalls(JsonNode toolCallsNode) {
        if (toolCallsNode == null || toolCallsNode.isMissingNode() || !toolCallsNode.isArray()) {
            return null;
        }
        List<LlmToolCall> result = new ArrayList<>();
        for (JsonNode tc : toolCallsNode) {
            LlmToolCall call = LlmToolCall.builder()
                    .id(tc.path("id").asText(null))
                    .type(tc.path("type").asText("function"))
                    .name(tc.path("function").path("name").asText(null))
                    .arguments(tc.path("function").path("arguments").asText(null))
                    .build();
            result.add(call);
        }
        return result.isEmpty() ? null : result;
    }

    static LlmUsage parseUsage(JsonNode usageNode) {
        if (usageNode == null || usageNode.isMissingNode()) {
            return null;
        }
        return new LlmUsage(
                usageNode.path("prompt_tokens").asLong(),
                usageNode.path("completion_tokens").asLong(),
                usageNode.path("total_tokens").asLong()
        );
    }
}
