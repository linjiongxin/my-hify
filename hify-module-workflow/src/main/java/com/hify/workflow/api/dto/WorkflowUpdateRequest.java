package com.hify.workflow.api.dto;

import lombok.Data;

/**
 * 更新工作流请求
 */
@Data
public class WorkflowUpdateRequest {

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态：draft, published, disabled
     */
    private String status;

    /**
     * 重试配置（JSON）
     */
    private String retryConfig;

    /**
     * 工作流配置（JSON）
     */
    private String config;
}