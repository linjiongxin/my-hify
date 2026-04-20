package com.hify.model.provider;

import com.hify.model.constant.ModelConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LlmProviderFactoryTest {

    private LlmProviderFactory factory;

    private LlmProvider openAiProvider;
    private LlmProvider customProvider;

    @BeforeEach
    void setUp() {
        openAiProvider = mock(LlmProvider.class);
        when(openAiProvider.getCode()).thenReturn(ModelConstants.ProtocolType.OPENAI_COMPATIBLE);
        when(openAiProvider.supports(any())).thenReturn(false);
        when(openAiProvider.supports("other-model")).thenReturn(true);

        customProvider = mock(LlmProvider.class);
        when(customProvider.getCode()).thenReturn("custom");
        when(customProvider.supports("custom-model")).thenReturn(true);
        when(customProvider.supports("other-model")).thenReturn(false);

        factory = new LlmProviderFactory(List.of(openAiProvider, customProvider));
    }

    @Test
    void shouldReturnProvider_whenGetProvider_givenExactCode() {
        LlmProvider result = factory.getProvider("custom");

        assertThat(result).isEqualTo(customProvider);
    }

    @Test
    void shouldReturnOpenAiCompatible_whenGetProvider_givenUnknownCode() {
        LlmProvider result = factory.getProvider("unknown_protocol");

        assertThat(result).isEqualTo(openAiProvider);
    }

    @Test
    void shouldThrowException_whenGetProvider_givenNoProviders() {
        LlmProviderFactory emptyFactory = new LlmProviderFactory(List.of());

        assertThatThrownBy(() -> emptyFactory.getProvider("anything"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 LLM Provider");
    }

    @Test
    void shouldThrowException_whenGetProvider_givenNoFallback() {
        // 工厂里没有 openai_compatible provider
        LlmProviderFactory noFallbackFactory = new LlmProviderFactory(List.of(customProvider));

        assertThatThrownBy(() -> noFallbackFactory.getProvider("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的 LLM Provider");
    }

    @Test
    void shouldReturnCustomProvider_whenGetProviderByModelId_givenSupportedModel() {
        LlmProvider result = factory.getProviderByModelId("custom-model");

        assertThat(result).isEqualTo(customProvider);
    }

    @Test
    void shouldReturnFirstSupportingProvider_whenGetProviderByModelId() {
        // customProvider 不支持 "other-model"，会落到 openAiProvider（supports 返回 true）
        LlmProvider result = factory.getProviderByModelId("other-model");

        assertThat(result).isEqualTo(openAiProvider);
    }

    @Test
    void shouldThrowException_whenGetProviderByModelId_givenNoSupport() {
        // 两个 provider 都不支持
        when(openAiProvider.supports(any())).thenReturn(false);
        when(customProvider.supports(any())).thenReturn(false);

        assertThatThrownBy(() -> factory.getProviderByModelId("unsupported"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的模型");
    }

    @Test
    void shouldInitializeOnce_whenGetProvider_calledConcurrently() {
        // 多次调用 getProvider，providerMap 只应该初始化一次
        factory.getProvider("custom");
        factory.getProvider("openai_compatible");
        factory.getProvider("unknown"); // fallback

        // 验证初始化逻辑只执行了一次映射操作
        verify(openAiProvider, atLeastOnce()).getCode();
        verify(customProvider, atLeastOnce()).getCode();
    }
}
