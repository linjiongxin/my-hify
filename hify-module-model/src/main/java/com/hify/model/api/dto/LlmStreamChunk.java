package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 流式响应块
 *
 * <p>各 Provider 将自身协议解析后，统一组装为此格式回调给上层</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmStreamChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 增量文本内容
     */
    private String content;

    /**
     * 增量推理/思考过程内容（DeepSeek R1 reasoning_content, Claude thinking 等）
     */
    private String reasoningContent;

    /**
     * 增量工具调用片段（流式返回工具调用时）
     */
    private List<LlmToolCall> toolCalls;

    /**
     * Token 使用量（部分 Provider 在最后 chunk 返回）
     */
    private LlmUsage usage;

    /**
     * 是否结束
     */
    private Boolean finish;

    /**
     * 结束原因: stop / length / content_filter / tool_calls / error
     */
    private String finishReason;
}
