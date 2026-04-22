package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 聊天响应（非流式）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 生成的内容
     */
    private String content;

    /**
     * 推理/思考过程内容
     */
    private String reasoningContent;

    /**
     * 工具调用请求列表
     */
    private List<LlmToolCall> toolCalls;

    /**
     * 结束原因: stop / length / content_filter / tool_calls
     */
    private String finishReason;

    /**
     * Token 使用量
     */
    private LlmUsage usage;
}
