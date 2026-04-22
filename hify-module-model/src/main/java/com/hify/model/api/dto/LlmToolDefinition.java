package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * LLM 工具定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmToolDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型: function
     */
    private String type;

    /**
     * 工具函数定义
     */
    private Function function;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Function implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}
