package com.hify.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import com.hify.common.web.handler.JsonbStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对话消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 会话内严格递增序号
     */
    private Integer seq;

    /**
     * 角色: system / user / assistant
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 状态: streaming / completed / error / interrupted
     */
    private String status;

    /**
     * 结束原因: stop / length / content_filter / error
     */
    private String finishReason;

    /**
     * 从发请求到流结束的耗时(ms)
     */
    private Integer durationMs;

    /**
     * 输入 token 数
     */
    private Integer inputTokens;

    /**
     * 输出 token 数
     */
    private Integer outputTokens;

    /**
     * 实际响应的模型版本
     */
    private String model;

    /**
     * 扩展元数据(JSONB)
     */
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String metadata;

    /**
     * 链路追踪 ID
     */
    private String traceId;
}
