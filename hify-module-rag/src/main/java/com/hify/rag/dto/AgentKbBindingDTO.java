package com.hify.rag.dto;

import lombok.Data;

/**
 * 创建知识库绑定 DTO
 */
@Data
public class AgentKbBindingDTO {

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 检索 Top-K
     */
    private Integer topK;

    /**
     * 相似度阈值
     */
    private java.math.BigDecimal similarityThreshold;
}