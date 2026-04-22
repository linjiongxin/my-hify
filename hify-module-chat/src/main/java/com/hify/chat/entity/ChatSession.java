package com.hify.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 对话会话实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 实际使用的模型标识
     */
    private String modelId;

    /**
     * 状态: active / archived
     */
    private String status;

    /**
     * 消息数量缓存
     */
    private Integer messageCount;

    /**
     * 最后消息时间
     */
    private LocalDateTime lastMessageAt;
}
