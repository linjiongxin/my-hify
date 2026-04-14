package com.hify.model.api;

import com.hify.model.api.dto.LlmChatRequest;
import com.hify.model.api.dto.LlmChatResponse;
import com.hify.model.api.dto.LlmStreamChunk;

import java.util.function.Consumer;

/**
 * LLM 网关 API
 *
 * @author hify
 */
public interface LlmGatewayApi {

    /**
     * 非流式对话
     *
     * @param modelId 模型标识（对应 model 表的 model_id 字段）
     * @param request 聊天请求
     * @return 聊天响应
     */
    LlmChatResponse chat(String modelId, LlmChatRequest request);

    /**
     * 流式对话
     *
     * @param modelId  模型标识
     * @param request  聊天请求
     * @param callback 流式回调
     */
    void streamChat(String modelId, LlmChatRequest request, Consumer<LlmStreamChunk> callback);
}
