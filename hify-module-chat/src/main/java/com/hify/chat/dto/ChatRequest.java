package com.hify.chat.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 发送消息请求
 */
@Data
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 消息内容
     */
    private String message;
}
