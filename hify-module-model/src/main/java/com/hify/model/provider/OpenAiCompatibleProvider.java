package com.hify.model.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.api.dto.LlmUsage;
import com.hify.model.constant.ModelConstants;
import com.hify.model.entity.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容协议 Provider
 * <p>覆盖 OpenAI、DeepSeek、通义千问等</p>
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
                .connectionPool(new okhttp3.ConnectionPool(200, 5, TimeUnit.MINUTES))
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
        // 作为默认兼容协议，支持所有未被特定 Provider 处理的模型
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
            String bodyString = null;
            if (response.body() != null) {
                bodyString = response.body().string();
            }
            if (!response.isSuccessful()) {
                throw new BizException(ResultCode.LLM_API_ERROR,
                        "LLM API 调用失败: " + response.code() + ", body=" + (bodyString != null ? bodyString : ""));
            }
            return parseChatResponse(bodyString != null ? bodyString : "{}");
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
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")));

        applyAuthHeaders(requestBuilder, provider, request);

        EventSource.Factory factory = EventSources.createFactory(getClient(true));
        factory.newEventSource(requestBuilder.build(), new OpenAiSseEventListener(callback, objectMapper));
    }

    private String resolveApiUrl(ModelProvider provider, LlmChatRequest request) {
        // 优先从请求扩展参数中读取运行时覆盖
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
                String headerName = config.get(ModelConstants.AuthConfigKey.HEADER_NAME) != null
                        ? config.get(ModelConstants.AuthConfigKey.HEADER_NAME).toString()
                        : ModelConstants.HeaderName.API_KEY;
                String prefix = config.get(ModelConstants.AuthConfigKey.PREFIX) != null
                        ? config.get(ModelConstants.AuthConfigKey.PREFIX).toString()
                        : "";
                requestBuilder.header(headerName, prefix + apiKey);
            }
            case ModelConstants.AuthType.NONE -> {
                // 不添加认证头
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
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BizException(ResultCode.LLM_API_ERROR, "构造 LLM 请求体失败", e);
        }
    }

    private List<Map<String, String>> toOpenAiMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(m -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    return map;
                })
                .toList();
    }

    private LlmChatResponse parseChatResponse(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            return LlmChatResponse.builder().content("").build();
        }
        String content = choices.get(0).path("message").path("content").asText("");
        String finishReason = choices.get(0).path("finish_reason").asText();

        LlmUsage usage = null;
        JsonNode usageNode = root.path("usage");
        if (!usageNode.isMissingNode()) {
            usage = new LlmUsage(
                    usageNode.path("prompt_tokens").asLong(),
                    usageNode.path("completion_tokens").asLong(),
                    usageNode.path("total_tokens").asLong()
            );
        }

        return LlmChatResponse.builder()
                .content(content)
                .finishReason(finishReason)
                .usage(usage)
                .build();
    }

}
