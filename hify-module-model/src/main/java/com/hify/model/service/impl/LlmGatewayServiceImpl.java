package com.hify.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.LlmGatewayApi;
import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.constant.ModelConstants;
import com.hify.model.entity.ModelConfig;
import com.hify.model.entity.ModelProvider;
import com.hify.model.provider.LlmProvider;
import com.hify.model.provider.LlmProviderFactory;
import com.hify.model.provider.LlmProviderSemaphoreManager;
import com.hify.model.service.ModelConfigService;
import com.hify.model.service.ModelProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 网关 Service 实现
 *
 * <p>负责模型路由、参数校验、并发限流，并将请求透传给具体 Provider</p>
 *
 * @author hify
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LlmGatewayServiceImpl implements LlmGatewayApi {

    private final ModelConfigService modelService;
    private final ModelProviderService modelProviderService;
    private final LlmProviderFactory providerFactory;
    private final LlmProviderSemaphoreManager semaphoreManager;

    @Override
    @Transactional(readOnly = true)
    public LlmChatResponse chat(String modelId, LlmChatRequest request) {
        ModelConfig model = getModel(modelId);
        ModelProvider provider = getProvider(model);
        LlmProvider llmProvider = providerFactory.getProvider(resolveProtocolType(request, provider));
        String providerCode = provider.getCode();

        // 根据模型能力预处理请求参数
        LlmChatRequest processedRequest = preprocessRequest(request, model);

        long start = System.currentTimeMillis();
        try {
            semaphoreManager.acquire(providerCode);
            LlmChatResponse response = llmProvider.chat(modelId, provider, processedRequest);
            log.debug("LLM 调用完成, modelId={}, provider={}, duration={}ms",
                    modelId, providerCode, System.currentTimeMillis() - start);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LLM_API_ERROR, "获取 LLM 调用许可被中断", e);
        } finally {
            semaphoreManager.release(providerCode);
        }
    }

    @Override
    public void streamChat(String modelId, LlmChatRequest request, Consumer<LlmStreamChunk> callback) {
        ModelConfig model = getModel(modelId);
        ModelProvider provider = getProvider(model);
        LlmProvider llmProvider = providerFactory.getProvider(resolveProtocolType(request, provider));
        String providerCode = provider.getCode();

        // 根据模型能力预处理请求参数
        LlmChatRequest processedRequest = preprocessRequest(request, model);

        long start = System.currentTimeMillis();
        java.util.concurrent.atomic.AtomicBoolean released = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable doRelease = () -> {
            if (released.compareAndSet(false, true)) {
                semaphoreManager.release(providerCode);
            }
        };

        try {
            semaphoreManager.acquire(providerCode);
            llmProvider.chatStream(modelId, provider, processedRequest, chunk -> {
                callback.accept(chunk);
                if (Boolean.TRUE.equals(chunk.getFinish())) {
                    doRelease.run();
                    log.debug("LLM 流式调用完成, modelId={}, provider={}, duration={}ms",
                            modelId, providerCode, System.currentTimeMillis() - start);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LLM_API_ERROR, "获取 LLM 调用许可被中断", e);
        } finally {
            doRelease.run();
        }
    }

    // ==================== 私有方法 ====================

    private ModelConfig getModel(String modelId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getModelId, modelId);
        ModelConfig model = modelService.getOne(wrapper);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模型不存在或已禁用: " + modelId);
        }
        return model;
    }

    private ModelProvider getProvider(ModelConfig model) {
        ModelProvider provider = modelProviderService.getById(model.getProviderId());
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            throw new BizException(ResultCode.LLM_API_ERROR, "模型提供商不可用: " + model.getProviderId());
        }
        return provider;
    }

    private String resolveProtocolType(LlmChatRequest request, ModelProvider provider) {
        if (request != null && request.getExtra() != null && request.getExtra().get("protocolType") != null) {
            return request.getExtra().get("protocolType");
        }
        return provider.getProtocolType() != null
                ? provider.getProtocolType()
                : ModelConstants.ProtocolType.OPENAI_COMPATIBLE;
    }

    /**
     * 根据模型 capabilities 预处理请求参数
     *
     * <p>清理模型不支持的参数，补充默认值，避免上游传入的参数导致 API 报错</p>
     */
    private LlmChatRequest preprocessRequest(LlmChatRequest request, ModelConfig model) {
        if (request == null) {
            return null;
        }

        Map<String, Object> caps = model.getCapabilities();
        boolean supportsToolCalling = getCapability(caps, "toolCalling", false);
        boolean supportsReasoning = getCapability(caps, "reasoning", false);
        boolean supportsStreaming = getCapability(caps, "streaming", true);
        boolean supportsVision = getCapability(caps, "vision", false);

        // 创建新的 request（浅拷贝，避免修改原始对象）
        LlmChatRequest processed = LlmChatRequest.builder()
                .messages(request.getMessages())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : model.getMaxTokens())
                .stream(request.getStream())
                .topP(request.getTopP())
                .tools(request.getTools())
                .toolChoice(request.getToolChoice())
                .reasoningEffort(request.getReasoningEffort())
                .extra(request.getExtra())
                .build();

        // 1. 模型不支持 toolCalling，移除 tools
        if (!supportsToolCalling && processed.getTools() != null && !processed.getTools().isEmpty()) {
            log.warn("模型 {} 不支持 toolCalling，已移除 tools 参数", model.getModelId());
            processed.setTools(null);
            processed.setToolChoice(null);
        }

        // 2. 模型不支持 reasoning，移除 reasoningEffort
        if (!supportsReasoning && processed.getReasoningEffort() != null) {
            log.warn("模型 {} 不支持 reasoning，已移除 reasoningEffort 参数", model.getModelId());
            processed.setReasoningEffort(null);
        }

        // 3. 模型不支持 streaming，强制改为非流式
        if (!supportsStreaming && Boolean.TRUE.equals(processed.getStream())) {
            log.warn("模型 {} 不支持 streaming，已强制改为非流式", model.getModelId());
            processed.setStream(false);
        }

        // 4. 模型不支持 vision，检查 messages 中是否包含图片
        if (!supportsVision && processed.getMessages() != null) {
            boolean hasVision = processed.getMessages().stream()
                    .anyMatch(m -> m.getContentParts() != null && m.getContentParts().stream()
                            .anyMatch(p -> "image_url".equals(p.getType())));
            if (hasVision) {
                log.warn("模型 {} 不支持 vision，但消息中包含图片，可能报错", model.getModelId());
            }
        }

        return processed;
    }

    private boolean getCapability(Map<String, Object> capabilities, String key, boolean defaultValue) {
        if (capabilities == null) {
            return defaultValue;
        }
        Object value = capabilities.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }
}
