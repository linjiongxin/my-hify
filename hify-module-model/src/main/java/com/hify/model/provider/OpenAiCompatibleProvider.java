package com.hify.model.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.api.dto.LlmUsage;
import com.hify.model.config.LlmGatewayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
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
@RequiredArgsConstructor
public class OpenAiCompatibleProvider implements LlmProvider {

    private final ObjectMapper objectMapper;
    private final LlmGatewayProperties properties;

    private OkHttpClient getClient(boolean isStream) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(isStream ? 120 : 60, TimeUnit.SECONDS);
        return builder.build();
    }

    @Override
    public String getCode() {
        return "openai_compatible";
    }

    @Override
    public boolean supports(String modelId) {
        // 作为默认兼容协议，支持所有未被特定 Provider 处理的模型
        return true;
    }

    @Override
    public LlmChatResponse chat(String modelId, LlmChatRequest request) {
        String apiUrl = resolveApiUrl(request) + "/chat/completions";
        String apiKey = resolveApiKey(request);
        String jsonBody = buildRequestBody(modelId, request, false);

        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = getClient(false).newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("LLM API 调用失败: " + response.code() + ", body=" + (response.body() != null ? response.body().string() : ""));
            }
            return parseChatResponse(response.body() != null ? response.body().string() : "{}");
        } catch (IOException e) {
            throw new RuntimeException("LLM API 请求异常", e);
        }
    }

    @Override
    public void chatStream(String modelId, LlmChatRequest request, Consumer<LlmStreamChunk> callback) {
        String apiUrl = resolveApiUrl(request) + "/chat/completions";
        String apiKey = resolveApiKey(request);
        String jsonBody = buildRequestBody(modelId, request, true);

        Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        EventSource.Factory factory = EventSources.createFactory(getClient(true));
        factory.newEventSource(httpRequest, new EventSourceListener() {
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
        });
    }

    private String resolveApiUrl(LlmChatRequest request) {
        // 优先从请求扩展参数中读取，否则使用默认配置
        String providerCode = getProviderCode(request);
        LlmGatewayProperties.ProviderConfig config = properties.getProviders().get(providerCode);
        if (config != null && config.getApiBaseUrl() != null) {
            return config.getApiBaseUrl();
        }
        return "https://api.openai.com/v1";
    }

    private String resolveApiKey(LlmChatRequest request) {
        String providerCode = getProviderCode(request);
        LlmGatewayProperties.ProviderConfig config = properties.getProviders().get(providerCode);
        if (config != null && config.getApiKey() != null) {
            return config.getApiKey();
        }
        return "";
    }

    private String getProviderCode(LlmChatRequest request) {
        if (request != null && request.getExtra() != null) {
            String code = request.getExtra().get("providerCode");
            if (code != null) {
                return code;
            }
        }
        return "openai";
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
            throw new RuntimeException("构造请求体失败", e);
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

    private LlmStreamChunk parseStreamChunk(String json) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (choices.isEmpty()) {
            return LlmStreamChunk.builder().content("").build();
        }
        JsonNode delta = choices.get(0).path("delta");
        String content = delta.path("content").asText("");
        String finishReason = choices.get(0).path("finish_reason").asText(null);

        return LlmStreamChunk.builder()
                .content(content)
                .finishReason(finishReason)
                .finish(finishReason != null)
                .build();
    }
}
