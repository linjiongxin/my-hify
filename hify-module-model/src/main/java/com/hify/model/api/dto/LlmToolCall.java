package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 工具调用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmToolCall implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工具调用 ID（由 LLM 生成，回传结果时需带上）
     */
    private String id;

    /**
     * 类型: function
     */
    private String type;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 参数 JSON 字符串
     */
    private String arguments;
}
