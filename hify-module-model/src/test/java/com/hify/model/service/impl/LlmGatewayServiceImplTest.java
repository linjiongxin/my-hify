package com.hify.model.service.impl;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmMessage;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.entity.ModelConfig;
import com.hify.model.entity.ModelProvider;
import com.hify.model.provider.LlmProvider;
import com.hify.model.provider.LlmProviderFactory;
import com.hify.model.provider.LlmProviderSemaphoreManager;
import com.hify.model.service.ModelConfigService;
import com.hify.model.service.ModelProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmGatewayServiceImplTest {

    private LlmGatewayServiceImpl llmGatewayService;

    @Mock
    private ModelConfigService modelConfigService;

    @Mock
    private ModelProviderService modelProviderService;

    @Mock
    private LlmProviderFactory providerFactory;

    @Mock
    private LlmProviderSemaphoreManager semaphoreManager;

    @Mock
    private LlmProvider llmProvider;

    @BeforeEach
    void setUp() {
        llmGatewayService = new LlmGatewayServiceImpl(
                modelConfigService, modelProviderService, providerFactory, semaphoreManager
        );
    }

    // ========== chat() 测试 ==========

    @Test
    void shouldReturnResponse_whenChat_givenValidModelAndProvider() throws InterruptedException {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(true);
        provider.setProtocolType("openai_compatible");

        LlmChatRequest request = LlmChatRequest.builder()
                .messages(List.of(LlmMessage.user("hello")))
                .build();

        LlmChatResponse expectedResponse = LlmChatResponse.builder().content("hi").build();

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(llmProvider).when(providerFactory).getProvider("openai_compatible");
        doNothing().when(semaphoreManager).acquire("openai");
        doNothing().when(semaphoreManager).release("openai");
        doReturn(expectedResponse).when(llmProvider).chat("gpt-4o", provider, request);

        LlmChatResponse response = llmGatewayService.chat("gpt-4o", request);

        assertThat(response).isEqualTo(expectedResponse);
        verify(semaphoreManager).acquire("openai");
        verify(semaphoreManager).release("openai");
    }

    @Test
    void shouldThrowBizException_whenChat_givenModelNotFound() {
        doReturn(null).when(modelConfigService).getOne(any());

        LlmChatRequest request = LlmChatRequest.builder().build();

        assertThatThrownBy(() -> llmGatewayService.chat("non-existent", request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                    assertThat(biz.getMessage()).contains("模型不存在或已禁用");
                });
    }

    @Test
    void shouldThrowBizException_whenChat_givenModelDisabled() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setEnabled(false);

        doReturn(model).when(modelConfigService).getOne(any());

        LlmChatRequest request = LlmChatRequest.builder().build();

        assertThatThrownBy(() -> llmGatewayService.chat("gpt-4o", request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldThrowBizException_whenChat_givenProviderNotFound() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(null).when(modelProviderService).getById(1L);

        LlmChatRequest request = LlmChatRequest.builder().build();

        assertThatThrownBy(() -> llmGatewayService.chat("gpt-4o", request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.LLM_API_ERROR.getCode());
                    assertThat(biz.getMessage()).contains("模型提供商不可用");
                });
    }

    @Test
    void shouldThrowBizException_whenChat_givenProviderDisabled() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(false);

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);

        LlmChatRequest request = LlmChatRequest.builder().build();

        assertThatThrownBy(() -> llmGatewayService.chat("gpt-4o", request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.LLM_API_ERROR.getCode());
                });
    }

    @Test
    void shouldReleaseSemaphore_whenChatThrowsException() throws InterruptedException {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(true);
        provider.setProtocolType("openai_compatible");

        LlmChatRequest request = LlmChatRequest.builder().build();

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(llmProvider).when(providerFactory).getProvider("openai_compatible");
        doNothing().when(semaphoreManager).acquire("openai");
        doNothing().when(semaphoreManager).release("openai");
        doThrow(new RuntimeException("LLM error")).when(llmProvider).chat(any(), any(), any());

        assertThatThrownBy(() -> llmGatewayService.chat("gpt-4o", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("LLM error");

        verify(semaphoreManager).release("openai");
    }

    @Test
    void shouldUseRequestProtocolType_whenChat_givenExtraProtocolType() throws InterruptedException {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(true);
        provider.setProtocolType("openai_compatible");

        LlmChatRequest request = LlmChatRequest.builder()
                .extra(Map.of("protocolType", "custom_protocol"))
                .build();

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(llmProvider).when(providerFactory).getProvider("custom_protocol");
        doNothing().when(semaphoreManager).acquire("openai");
        doNothing().when(semaphoreManager).release("openai");
        doReturn(LlmChatResponse.builder().build()).when(llmProvider).chat(any(), any(), any());

        llmGatewayService.chat("gpt-4o", request);

        verify(providerFactory).getProvider("custom_protocol");
    }

    @Test
    void shouldUseProviderDefaultProtocol_whenChat_givenNoExtraAndProviderHasNoProtocol() throws InterruptedException {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(true);
        provider.setProtocolType(null);

        LlmChatRequest request = LlmChatRequest.builder().build();

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(llmProvider).when(providerFactory).getProvider("openai_compatible");
        doNothing().when(semaphoreManager).acquire("openai");
        doNothing().when(semaphoreManager).release("openai");
        doReturn(LlmChatResponse.builder().build()).when(llmProvider).chat(any(), any(), any());

        llmGatewayService.chat("gpt-4o", request);

        verify(providerFactory).getProvider("openai_compatible");
    }

    // ========== streamChat() 测试 ==========

    @Test
    void shouldStreamChat_andReleaseOnFinish_whenGivenValidRequest() throws InterruptedException {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(true);
        provider.setProtocolType("openai_compatible");

        LlmChatRequest request = LlmChatRequest.builder().build();

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(llmProvider).when(providerFactory).getProvider("openai_compatible");
        doNothing().when(semaphoreManager).acquire("openai");
        doNothing().when(semaphoreManager).release("openai");

        // 模拟流式：发送两个 chunk，第二个标记 finish
        doAnswer(inv -> {
            Consumer<LlmStreamChunk> callback = inv.getArgument(3);
            callback.accept(LlmStreamChunk.builder().content("Hello").build());
            callback.accept(LlmStreamChunk.builder().content(" world").finish(true).build());
            return null;
        }).when(llmProvider).chatStream(any(), any(), any(), any(Consumer.class));

        List<LlmStreamChunk> chunks = new java.util.ArrayList<>();
        llmGatewayService.streamChat("gpt-4o", request, chunks::add);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("Hello");
        assertThat(chunks.get(1).getContent()).isEqualTo(" world");
        assertThat(chunks.get(1).getFinish()).isTrue();
        verify(semaphoreManager, times(1)).release("openai");
    }

    @Test
    void shouldReleaseOnlyOnce_whenStreamChat_andFinishChunkNotReceived() throws InterruptedException {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(true);
        provider.setProtocolType("openai_compatible");

        LlmChatRequest request = LlmChatRequest.builder().build();

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(llmProvider).when(providerFactory).getProvider("openai_compatible");
        doNothing().when(semaphoreManager).acquire("openai");
        doNothing().when(semaphoreManager).release("openai");

        // 模拟流式：只发送一个非 finish chunk
        doAnswer(inv -> {
            Consumer<LlmStreamChunk> callback = inv.getArgument(3);
            callback.accept(LlmStreamChunk.builder().content("Hello").build());
            return null;
        }).when(llmProvider).chatStream(any(), any(), any(), any(Consumer.class));

        List<LlmStreamChunk> chunks = new java.util.ArrayList<>();
        llmGatewayService.streamChat("gpt-4o", request, chunks::add);

        // finally 块会释放一次
        verify(semaphoreManager, times(1)).release("openai");
    }

    @Test
    void shouldThrowBizException_whenStreamChat_givenModelNotFound() {
        doReturn(null).when(modelConfigService).getOne(any());

        LlmChatRequest request = LlmChatRequest.builder().build();

        assertThatThrownBy(() -> llmGatewayService.streamChat("non-existent", request, chunk -> {}))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldThrowBizException_whenStreamChat_givenProviderDisabled() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        model.setEnabled(true);

        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        provider.setCode("openai");
        provider.setEnabled(false);

        doReturn(model).when(modelConfigService).getOne(any());
        doReturn(provider).when(modelProviderService).getById(1L);

        LlmChatRequest request = LlmChatRequest.builder().build();

        assertThatThrownBy(() -> llmGatewayService.streamChat("gpt-4o", request, chunk -> {}))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.LLM_API_ERROR.getCode());
                });
    }
}
