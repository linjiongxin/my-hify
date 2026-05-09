package com.hify.chat.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息响应 VO
 */
@Data
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Integer seq;
    private String role;
    private String content;
    private String status;
    private String finishReason;
    private Integer durationMs;
    private Integer inputTokens;
    private Integer outputTokens;
    private String model;
    private String traceId;
    private LocalDateTime createdAt;
}
