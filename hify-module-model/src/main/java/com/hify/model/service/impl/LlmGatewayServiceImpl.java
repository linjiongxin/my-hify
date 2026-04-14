package com.hify.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.entity.Model;
import com.hify.model.entity.ModelProvider;
import com.hify.model.provider.LlmProvider;
import com.hify.model.provider.LlmProviderFactory;
import com.hify.model.provider.LlmProviderSemaphoreManager;
import com.hify.model.service.LlmGatewayService;
import com.hify.model.service.ModelProviderService;
import com.hify.model.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * LLM 网关 Service 实现
 *
 * @author hify
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmGatewayServiceImpl implements LlmGatewayService {

    private final ModelService modelService;
    private final ModelProviderService modelProviderService;
    private final LlmProviderFactory providerFactory;
    private final LlmProviderSemaphoreManager semaphoreManager;

    @Override
    public LlmChatResponse chat(String modelId, LlmChatRequest request) {
        Model model = getModel(modelId);
        ModelProvider provider = modelProviderService.getById(model.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new IllegalStateException("模型提供商不可用: " + model.getProviderId());
        }

        String providerCode = resolveProviderCode(request, provider);
        LlmProvider llmProvider = providerFactory.getProvider(providerCode);

        long start = System.currentTimeMillis();
        try {
            semaphoreManager.acquire(providerCode);
            LlmChatResponse response = llmProvider.chat(modelId, request);
            log.debug("LLM 调用完成, modelId={}, provider={}, duration={}ms",
                    modelId, providerCode, System.currentTimeMillis() - start);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取 LLM 调用许可被中断", e);
        } finally {
            semaphoreManager.release(providerCode);
        }
    }

    @Override
    public void streamChat(String modelId, LlmChatRequest request, Consumer<LlmStreamChunk> callback) {
        Model model = getModel(modelId);
        ModelProvider provider = modelProviderService.getById(model.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new IllegalStateException("模型提供商不可用: " + model.getProviderId());
        }

        String providerCode = resolveProviderCode(request, provider);
        LlmProvider llmProvider = providerFactory.getProvider(providerCode);

        long start = System.currentTimeMillis();
        try {
            semaphoreManager.acquire(providerCode);
            llmProvider.chatStream(modelId, request, chunk -> {
                callback.accept(chunk);
                if (Boolean.TRUE.equals(chunk.getFinish())) {
                    semaphoreManager.release(providerCode);
                    log.debug("LLM 流式调用完成, modelId={}, provider={}, duration={}ms",
                            modelId, providerCode, System.currentTimeMillis() - start);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            semaphoreManager.release(providerCode);
            throw new RuntimeException("获取 LLM 调用许可被中断", e);
        }
    }

    private Model getModel(String modelId) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getModelId, modelId);
        Model model = modelService.getOne(wrapper);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw new IllegalArgumentException("模型不存在或已禁用: " + modelId);
        }
        return model;
    }

    private String resolveProviderCode(LlmChatRequest request, ModelProvider provider) {
        if (request != null && request.getExtra() != null && request.getExtra().get("providerCode") != null) {
            return request.getExtra().get("providerCode");
        }
        return provider.getCode();
    }
}
