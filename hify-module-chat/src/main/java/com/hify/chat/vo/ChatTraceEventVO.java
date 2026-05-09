package com.hify.chat.vo;

import lombok.Data;

import java.util.Map;

/**
 * 链路追踪事件 VO
 */
@Data
public class ChatTraceEventVO {

    /**
     * 事件类型：user_message / rag / workflow / mcp / llm_reply
     */
    private String type;

    /**
     * 事件标题
     */
    private String title;

    /**
     * 发生时间（ISO 格式）
     */
    private String time;

    /**
     * 结束时间（ISO 格式，可选）
     */
    private String endTime;

    /**
     * 耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 状态：success / failed / running / completed
     */
    private String status;

    /**
     * 扩展详情
     */
    private Map<String, Object> details;
}
