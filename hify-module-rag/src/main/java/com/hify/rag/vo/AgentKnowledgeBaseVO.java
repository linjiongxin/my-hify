package com.hify.rag.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Agent 知识库绑定 VO
 */
@Data
public class AgentKnowledgeBaseVO {

    private Long id;

    private Long agentId;

    private Long kbId;

    private String kbName;

    private Integer topK;

    private BigDecimal similarityThreshold;

    private Boolean enabled;
}