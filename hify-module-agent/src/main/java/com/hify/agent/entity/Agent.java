package com.hify.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Agent 实体类
 *
 * @author hify
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent")
public class Agent extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * Agent 名称
     */
    private String name;

    /**
     * Agent 描述
     */
    private String description;

    /**
     * 模型 ID
     */
    private String modelId;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * 最大 tokens
     */
    private Integer maxTokens;

    /**
     * Top P 参数
     */
    private BigDecimal topP;

    /**
     * 欢迎消息
     */
    private String welcomeMessage;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 绑定主工作流 ID
     */
    private Long workflowId;

    /**
     * 执行模式: react / workflow
     */
    private String executionMode;
}
