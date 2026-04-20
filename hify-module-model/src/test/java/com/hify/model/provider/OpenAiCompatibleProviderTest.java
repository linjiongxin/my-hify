package com.hify.model.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.constant.ModelConstants;
import com.hify.model.entity.ModelProvider;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAiCompatibleProviderTest {

    private OpenAiCompatibleProvider provider;

    @Mock
    private OkHttpClient mockClient;

    @Mock
    private Call mockCall;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        provider = new OpenAiCompatibleProvider(objectMapper);
        // 注入 mock client（覆盖构造器中创建的 client）
        ReflectionTestUtils.setField(provider, "baseClient", mockClient);
    }

    // ========== getCode / supports ==========

    @Test
    void shouldReturnOpenAiCompatibleCode_whenGetCode() {
        assertThat(provider.getCode()).isEqualTo(ModelConstants.ProtocolType.OPENAI_COMPATIBLE);
    }

    @Test
    void shouldSupportAnyModel_whenSupports() {
        assertThat(provider.supports("gpt-4o")).isTrue();
        assertThat(provider.supports("deepseek-chat")).isTrue();
        assertThat(provider.supports("anything")).isTrue();
    }

    // ========== chat() 成功路径 ==========

    @Test
    void shouldReturnResponse_whenChat_givenSuccessfulCall() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200, "{\"choices\":[{\"message\":{\"content\":\"hi\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}");

        LlmChatResponse response = provider.chat("gpt-4o", modelProvider, request);

        assertThat(response.getContent()).isEqualTo("hi");
        assertThat(response.getFinishReason()).isEqualTo("stop");
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(1);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(2);
    }

    @Test
    void shouldUseProviderApiUrl_whenChat_givenNoOverride() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://custom.api.com/v1", ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().url().toString()).isEqualTo("https://custom.api.com/v1/chat/completions");
    }

    @Test
    void shouldUseRequestOverrideUrl_whenChat_givenExtraApiBaseUrl() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://provider.api.com/v1", ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .extra(Map.of("apiBaseUrl", "https://override.api.com/v1"))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().url().toString()).isEqualTo("https://override.api.com/v1/chat/completions");
    }

    @Test
    void shouldUseDefaultUrl_whenChat_givenNoProviderUrlAndNoOverride() throws IOException {
        ModelProvider modelProvider = createProvider("openai", null, ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().url().toString()).isEqualTo("https://api.openai.com/v1/chat/completions");
    }

    // ========== chat() 鉴权测试 ==========

    @Test
    void shouldSetBearerHeader_whenChat_givenBearerAuthType() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, "sk-secret");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("Authorization")).isEqualTo("Bearer sk-secret");
    }

    @Test
    void shouldSetApiKeyHeader_whenChat_givenApiKeyAuthType() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.API_KEY, "my-api-key");
        modelProvider.setAuthConfig(Map.of(
                ModelConstants.AuthConfigKey.HEADER_NAME, "X-API-Key",
                ModelConstants.AuthConfigKey.PREFIX, "ApiKey "
        ));
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("X-API-Key")).isEqualTo("ApiKey my-api-key");
    }

    @Test
    void shouldUseDefaultApiKeyHeader_whenChat_givenApiKeyAuthTypeWithoutConfig() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.API_KEY, "my-key");
        modelProvider.setAuthConfig(null);
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("api-key")).isEqualTo("my-key");
    }

    @Test
    void shouldNotSetAuthHeader_whenChat_givenNoneAuthType() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.NONE, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("Authorization")).isNull();
        assertThat(requestCaptor.getValue().header("api-key")).isNull();
    }

    @Test
    void shouldSetCustomHeaders_whenChat_givenCustomAuthType() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.CUSTOM, null);
        modelProvider.setAuthConfig(Map.of(
                ModelConstants.AuthConfigKey.HEADERS, Map.of("X-Custom-Auth", "token123", "X-Request-ID", "uuid-456")
        ));
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("X-Custom-Auth")).isEqualTo("token123");
        assertThat(requestCaptor.getValue().header("X-Request-ID")).isEqualTo("uuid-456");
    }

    @Test
    void shouldUseRequestApiKey_whenChat_givenExtraApiKey() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, "sk-provider");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .extra(Map.of("apiKey", "sk-request"))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("Authorization")).isEqualTo("Bearer sk-request");
    }

    @Test
    void shouldUseEmptyApiKey_whenChat_givenNoApiKey() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, null);
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("Authorization")).startsWith("Bearer");
    }

    @Test
    void shouldDefaultToBearer_whenChat_givenUnknownAuthType() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", "UNKNOWN_TYPE", "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        setupMockResponse(200);

        provider.chat("gpt-4o", modelProvider, request);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient).newCall(requestCaptor.capture());
        assertThat(requestCaptor.getValue().header("Authorization")).isEqualTo("Bearer sk-test");
    }

    // ========== chat() 错误路径 ==========

    @Test
    void shouldThrowBizException_whenChat_givenHttpError() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        ResponseBody mockBody = mock(ResponseBody.class);
        when(mockBody.string()).thenReturn("{\"error\":\"invalid key\"}");
        Response mockResponse = mock(Response.class);
        when(mockResponse.code()).thenReturn(401);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.body()).thenReturn(mockBody);
        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);

        assertThatThrownBy(() -> provider.chat("gpt-4o", modelProvider, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.LLM_API_ERROR.getCode());
                    assertThat(biz.getMessage()).contains("401");
                });
    }

    @Test
    void shouldThrowBizException_whenChat_givenIOException() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new IOException("Connection timeout"));

        assertThatThrownBy(() -> provider.chat("gpt-4o", modelProvider, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.LLM_API_ERROR.getCode());
                    assertThat(biz.getMessage()).contains("请求异常");
                });
    }

    @Test
    void shouldHandleEmptyBody_whenChat_givenNullResponseBody() throws IOException {
        ModelProvider modelProvider = createProvider("openai", "https://api.openai.com/v1", ModelConstants.AuthType.BEARER, "sk-test");
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        Response mockResponse = mock(Response.class);
        when(mockResponse.code()).thenReturn(500);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.body()).thenReturn(null);
        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);

        assertThatThrownBy(() -> provider.chat("gpt-4o", modelProvider, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.LLM_API_ERROR.getCode());
                });
    }

    // ========== buildRequestBody 测试（通过反射调用 private 方法） ==========

    @Test
    void shouldBuildCorrectRequestBody_whenBuildRequestBody_givenAllFields() throws Exception {
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.system("sys"), LlmMessage.user("hello")))
                .temperature(0.7)
                .maxTokens(100)
                .topP(0.9)
                .build();

        String json = (String) ReflectionTestUtils.invokeMethod(provider, "buildRequestBody", "gpt-4o", request, false);
        Map<?, ?> body = objectMapper.readValue(json, Map.class);

        assertThat(body.get("model")).isEqualTo("gpt-4o");
        assertThat(body.get("stream")).isEqualTo(false);
        assertThat(body.get("temperature")).isEqualTo(0.7);
        assertThat(body.get("max_tokens")).isEqualTo(100);
        assertThat(body.get("top_p")).isEqualTo(0.9);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).get("role")).isEqualTo("system");
        assertThat(messages.get(0).get("content")).isEqualTo("sys");
        assertThat(messages.get(1).get("role")).isEqualTo("user");
        assertThat(messages.get(1).get("content")).isEqualTo("hello");
    }

    @Test
    void shouldOmitNullFields_whenBuildRequestBody_givenPartialFields() throws Exception {
        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        String json = (String) ReflectionTestUtils.invokeMethod(provider, "buildRequestBody", "gpt-4o", request, true);
        Map<?, ?> body = objectMapper.readValue(json, Map.class);

        assertThat(body.get("model")).isEqualTo("gpt-4o");
        assertThat(body.get("stream")).isEqualTo(true);
        assertThat(body.containsKey("temperature")).isFalse();
        assertThat(body.containsKey("max_tokens")).isFalse();
        assertThat(body.containsKey("top_p")).isFalse();
    }

    // ========== parseChatResponse 测试 ==========

    @Test
    void shouldParseResponse_whenParseChatResponse_givenValidJson() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"Hello\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":5,\"completion_tokens\":3,\"total_tokens\":8}}";

        LlmChatResponse response = (LlmChatResponse) ReflectionTestUtils.invokeMethod(provider, "parseChatResponse", json);

        assertThat(response.getContent()).isEqualTo("Hello");
        assertThat(response.getFinishReason()).isEqualTo("stop");
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(5);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(3);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(8);
    }

    @Test
    void shouldReturnEmptyContent_whenParseChatResponse_givenEmptyChoices() throws Exception {
        String json = "{\"choices\":[],\"usage\":null}";

        LlmChatResponse response = (LlmChatResponse) ReflectionTestUtils.invokeMethod(provider, "parseChatResponse", json);

        assertThat(response.getContent()).isEqualTo("");
        assertThat(response.getUsage()).isNull();
    }

    @Test
    void shouldReturnEmptyContent_whenParseChatResponse_givenNoUsage() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"test\"}}]}";

        LlmChatResponse response = (LlmChatResponse) ReflectionTestUtils.invokeMethod(provider, "parseChatResponse", json);

        assertThat(response.getContent()).isEqualTo("test");
        assertThat(response.getUsage()).isNull();
    }

    // ========== 辅助方法 ==========

    private ModelProvider createProvider(String code, String apiBaseUrl, String authType, String apiKey) {
        ModelProvider provider = new ModelProvider();
        provider.setCode(code);
        provider.setApiBaseUrl(apiBaseUrl);
        provider.setAuthType(authType);
        provider.setApiKey(apiKey);
        return provider;
    }

    private void setupMockResponse(int statusCode, String body) throws IOException {
        ResponseBody mockBody = mock(ResponseBody.class);
        when(mockBody.string()).thenReturn(body);
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccessful()).thenReturn(statusCode >= 200 && statusCode < 300);
        when(mockResponse.body()).thenReturn(mockBody);
        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
    }

    private void setupMockResponse(int statusCode) throws IOException {
        Response mockResponse = mock(Response.class);
        when(mockResponse.isSuccessful()).thenReturn(statusCode >= 200 && statusCode < 300);
        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
    }
}
