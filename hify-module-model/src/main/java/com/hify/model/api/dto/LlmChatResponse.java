package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 聊天响应
 *
 * @author hify
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
     * 结束原因
     */
    private String finishReason;

    /**
     * Token 使用量
     */
    private LlmUsage usage;
}
