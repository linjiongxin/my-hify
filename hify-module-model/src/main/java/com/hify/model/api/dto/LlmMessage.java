package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 消息
 *
 * <p>支持纯文本和多模态（text + image_url）两种模式：</p>
 * <ul>
 *     <li>纯文本：使用 content 字段</li>
 *     <li>多模态：使用 contentParts 字段</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色: system / user / assistant / tool
     */
    private String role;

    /**
     * 纯文本内容（非多模态时使用）
     */
    private String content;

    /**
     * 多模态内容片段（多模态时使用，优先级高于 content）
     */
    private List<LlmContentPart> contentParts;

    /**
     * assistant 的工具调用请求列表
     */
    private List<LlmToolCall> toolCalls;

    /**
     * tool 角色的 tool_call_id（回传结果时必须带上）
     */
    private String toolCallId;

    /**
     * tool 角色的工具名称
     */
    private String name;

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null, null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null, null, null);
    }

    public static LlmMessage tool(String toolCallId, String name, String content) {
        return new LlmMessage("tool", content, null, null, toolCallId, name);
    }

    public static LlmMessage assistantWithToolCalls(List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", null, null, toolCalls, null, null);
    }
}
