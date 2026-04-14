package com.hify.model.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LLM Provider 工厂
 *
 * @author hify
 */
@Component
@RequiredArgsConstructor
public class LlmProviderFactory {

    private final List<LlmProvider> providers;
    private volatile Map<String, LlmProvider> providerMap;

    public LlmProvider getProvider(String code) {
        if (providerMap == null) {
            synchronized (this) {
                if (providerMap == null) {
                    providerMap = providers.stream()
                            .collect(Collectors.toMap(LlmProvider::getCode, Function.identity(), (a, b) -> a));
                }
            }
        }
        LlmProvider provider = providerMap.get(code);
        if (provider == null) {
            // 默认使用 OpenAI 兼容协议
            provider = providerMap.get("openai_compatible");
        }
        if (provider == null) {
            throw new IllegalArgumentException("不支持的 LLM Provider: " + code);
        }
        return provider;
    }

    public LlmProvider getProviderByModelId(String modelId) {
        for (LlmProvider provider : providers) {
            if (provider.supports(modelId)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("不支持的模型: " + modelId);
    }
}
