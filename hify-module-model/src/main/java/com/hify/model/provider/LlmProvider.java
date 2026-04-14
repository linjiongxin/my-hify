package com.hify.model.provider;

import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmStreamChunk;

import java.util.function.Consumer;

/**
 * LLM Provider 接口
 *
 * @author hify
 */
public interface LlmProvider {

    /**
     * Provider 代码标识
     */
    String getCode();

    /**
     * 是否支持该模型
     */
    boolean supports(String modelId);

    /**
     * 非流式对话
     */
    LlmChatResponse chat(String modelId, LlmChatRequest request);

    /**
     * 流式对话
     */
    void chatStream(String modelId, LlmChatRequest request, Consumer<LlmStreamChunk> callback);
}
