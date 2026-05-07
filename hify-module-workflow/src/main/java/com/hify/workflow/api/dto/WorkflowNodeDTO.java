package com.hify.workflow.api.dto;

import lombok.Data;

/**
 * 工作流节点 DTO
 */
@Data
public class WorkflowNodeDTO {

    private Long id;

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 节点唯一标识
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点配置（JSON）
     */
    private String config;

    /**
     * 节点 X 坐标
     */
    private Integer positionX;

    /**
     * 节点 Y 坐标
     */
    private Integer positionY;

    /**
     * 重试配置（JSON）
     */
    private String retryConfig;
}