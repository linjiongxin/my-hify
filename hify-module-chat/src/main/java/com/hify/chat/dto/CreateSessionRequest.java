package com.hify.chat.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建会话请求
 */
@Data
public class CreateSessionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 首条用户消息（可选，创建时直接发消息）
     */
    private String firstMessage;
}
