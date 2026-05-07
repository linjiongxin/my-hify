package com.hify.workflow.api.dto;

import lombok.Data;

/**
 * 工作流连线 DTO
 */
@Data
public class WorkflowEdgeDTO {

    private Long id;

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 源节点 ID
     */
    private String sourceNode;

    /**
     * 目标节点 ID
     */
    private String targetNode;

    /**
     * 条件表达式
     */
    private String condition;

    /**
     * 连线顺序
     */
    private Integer edgeIndex;
}