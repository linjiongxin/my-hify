package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 流式响应块
 *
 * @author hify
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmStreamChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 增量内容
     */
    private String content;

    /**
     * 是否结束
     */
    private Boolean finish;

    /**
     * 结束原因
     */
    private String finishReason;
}
