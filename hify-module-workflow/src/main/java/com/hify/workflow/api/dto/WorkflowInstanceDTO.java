package com.hify.workflow.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例 DTO
 */
@Data
public class WorkflowInstanceDTO {

    private Long id;

    /**
     * 工作流 ID
     */
    private Long workflowId;

    /**
     * 状态：running, completed, failed, cancelled
     */
    private String status;

    /**
     * 当前节点 ID
     */
    private String currentNodeId;

    /**
     * 执行上下文（JSON）
     */
    private String context;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime finishedAt;
}