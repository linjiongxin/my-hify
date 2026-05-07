package com.hify.rag.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hify.common.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Agent × 知识库绑定实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_knowledge_base")
public class AgentKnowledgeBase extends BaseEntity {

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 检索返回的 Top-K 数量
     */
    private Integer topK;

    /**
     * 相似度阈值
     */
    private BigDecimal similarityThreshold;

    /**
     * 是否启用
     */
    private Boolean enabled;
}