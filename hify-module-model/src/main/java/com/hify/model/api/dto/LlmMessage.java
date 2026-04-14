package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 消息
 *
 * @author hify
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
     * 内容
     */
    private String content;

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content);
    }
}
