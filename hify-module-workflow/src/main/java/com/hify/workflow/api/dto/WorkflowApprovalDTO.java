package com.hify.workflow.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流审批 DTO
 */
@Data
public class WorkflowApprovalDTO {

    private Long id;

    /**
     * 实例 ID
     */
    private Long instanceId;

    /**
     * 节点 ID
     */
    private String nodeId;

    /**
     * 状态：pending, approved, rejected
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;
}