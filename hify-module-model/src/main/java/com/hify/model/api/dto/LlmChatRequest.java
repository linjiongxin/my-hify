package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 聊天请求
 *
 * <p>统一格式，各 Provider 实现负责转换为自身协议</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息列表
     */
    private List<LlmMessage> messages;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大生成 Token 数
     */
    private Integer maxTokens;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * Top P
     */
    private Double topP;

    /**
     * 工具定义列表
     */
    private List<LlmToolDefinition> tools;

    /**
     * 工具选择模式: auto / none / required / {"type": "function", "function": {"name": "xxx"}}
     */
    private Object toolChoice;

    /**
     * 推理/思考深度控制（o-series: low/medium/high，DeepSeek: true/false）
     */
    private String reasoningEffort;

    /**
     * 扩展参数（如 providerCode、apiKey 覆盖等）
     */
    private java.util.Map<String, String> extra;
}
