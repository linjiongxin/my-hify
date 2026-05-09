package com.hify.chat.vo;

import lombok.Data;

import java.util.List;

/**
 * 对话链路追踪 VO
 */
@Data
public class ChatTraceVO {

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 用户消息摘要
     */
    private String userMessage;

    /**
     * Agent 名称
     */
    private String agentName;

    /**
     * 总耗时（毫秒）
     */
    private Integer totalDurationMs;

    /**
     * 链路事件列表（按时间排序）
     */
    private List<ChatTraceEventVO> events;
}
