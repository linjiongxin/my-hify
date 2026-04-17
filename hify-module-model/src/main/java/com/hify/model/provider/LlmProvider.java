package com.hify.model.provider;

import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmStreamChunk;
import com.hify.model.entity.ModelProvider;

import java.util.function.Consumer;

/**
 * LLM Provider 接口
 *
 * @author hify
 */
public interface LlmProvider {

    /**
     * Provider 协议类型标识
     */
    String getCode();

    /**
     * 是否支持该模型
     */
    boolean supports(String modelId);

    /**
     * 非流式对话
     *
     * @param modelId  模型标识
     * @param provider 提供商配置（含鉴权、BaseUrl 等）
     */
    LlmChatResponse chat(String modelId, ModelProvider provider, LlmChatRequest request);

    /**
     * 流式对话
     *
     * @param modelId  模型标识
     * @param provider 提供商配置（含鉴权、BaseUrl 等）
     */
    void chatStream(String modelId, ModelProvider provider, LlmChatRequest request, Consumer<LlmStreamChunk> callback);
}
